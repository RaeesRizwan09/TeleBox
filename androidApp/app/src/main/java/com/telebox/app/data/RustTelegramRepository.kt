package com.telebox.app.data

import android.content.Context
import uniffi.telegram_drive_engine.*
import com.telebox.app.util.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class RustTelegramRepository(
    private val context: Context,
    private val preferencesStore: PreferencesStore
) : TelegramRepository {

    private val engine: TelegramDriveEngine by lazy {
        TelegramDriveEngine().apply {
            val dataDir = context.filesDir.absolutePath
            val cacheDir = context.cacheDir.absolutePath
            setStoragePaths(dataDir, cacheDir)
            setProgressListener(progressListener)
        }
    }

    private val _uploadProgress = MutableSharedFlow<ProgressPayload>(
        replay = 0,
        extraBufferCapacity = 64
    )
    override val uploadProgress: SharedFlow<ProgressPayload> = _uploadProgress.asSharedFlow()

    private val _downloadProgress = MutableSharedFlow<ProgressPayload>(
        replay = 0,
        extraBufferCapacity = 64
    )
    override val downloadProgress: SharedFlow<ProgressPayload> = _downloadProgress.asSharedFlow()

    private val progressListener = object : TransferProgressListener {
        override fun onUploadProgress(
            id: String,
            percent: Byte,
            transferredBytes: Long,
            totalBytes: Long,
            speedBytesPerSec: Long
        ) {
            _uploadProgress.tryEmit(
                ProgressPayload(
                    id = id,
                    percent = percent.toFloat(),
                    uploadedBytes = transferredBytes,
                    totalBytes = totalBytes,
                    speedBytesPerSec = speedBytesPerSec
                )
            )
        }

        override fun onDownloadProgress(
            id: String,
            percent: Byte,
            transferredBytes: Long,
            totalBytes: Long,
            speedBytesPerSec: Long
        ) {
            _downloadProgress.tryEmit(
                ProgressPayload(
                    id = id,
                    percent = percent.toFloat(),
                    uploadedBytes = transferredBytes,
                    totalBytes = totalBytes,
                    speedBytesPerSec = speedBytesPerSec
                )
            )
        }
    }

    private val cancelledTransfers = ConcurrentHashMap.newKeySet<String>()

    private suspend fun <T> engineCall(block: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: EngineError) {
                throw RuntimeException(e.message ?: "Telegram engine error", e)
            } catch (e: Exception) {
                throw RuntimeException("Engine call failed", e)
            }
        }

    override suspend fun connect(apiId: Int) {
        engineCall { engine.connect(apiId) }
    }

    override suspend fun checkConnection(): Boolean =
        engineCall { engine.checkConnection() }

    override suspend fun isNetworkAvailable(): Boolean =
        engineCall { engine.isNetworkAvailable() }

    override suspend fun requestAuthCode(phone: String, apiId: Int, apiHash: String) {
        engineCall { engine.requestLoginCode(phone, apiId, apiHash) }
    }

    override suspend fun signIn(code: String): com.telebox.app.data.AuthResult =
        engineCall {
            val result = engine.signIn(code)
            com.telebox.app.data.AuthResult(success = result.success, nextStep = result.nextStep)
        }

    override suspend fun checkPassword(password: String): com.telebox.app.data.AuthResult =
        engineCall {
            val result = engine.checkPassword(password)
            com.telebox.app.data.AuthResult(success = result.success, nextStep = result.nextStep)
        }

    override suspend fun qrLogin(apiId: Int, apiHash: String): String =
        engineCall { engine.authQrLogin(apiId, apiHash) }

    override suspend fun qrPoll(): com.telebox.app.data.AuthResult =
        engineCall {
            val result = engine.authQrPoll()
            com.telebox.app.data.AuthResult(success = result.success, nextStep = result.nextStep)
        }

    override suspend fun logout() {
        engineCall { engine.logout() }
    }

    override suspend fun cleanCache() {
        engineCall { engine.cleanCache() }
    }

    override suspend fun getFiles(folderId: Long?): List<TelegramFile> =
        engineCall { engine.getFiles(folderId).map { it.toTelegramFile(folderId) } }

    override suspend fun getBandwidth(): com.telebox.app.data.BandwidthStats =
        engineCall {
            val stats = engine.getBandwidth()
            com.telebox.app.data.BandwidthStats(upBytes = stats.upBytes, downBytes = stats.downBytes)
        }

    override suspend fun scanFolders(): List<TelegramFolder> =
        engineCall {
            engine.scanFolders().map { rust ->
                TelegramFolder(id = rust.id, name = rust.name, parentId = rust.parentId)
            }
        }

    override suspend fun createFolder(name: String): TelegramFolder =
        engineCall {
            val rustFolder = engine.createFolder(name)
            TelegramFolder(id = rustFolder.id, name = rustFolder.name, parentId = rustFolder.parentId)
        }

    override suspend fun deleteFolder(folderId: Long) {
        engineCall { engine.deleteFolder(folderId) }
    }

    override suspend fun deleteFile(messageId: Long, folderId: Long?) {
        engineCall { engine.deleteFile(messageId.toInt(), folderId) }
    }

    override suspend fun moveFiles(messageIds: List<Long>, sourceFolderId: Long?, targetFolderId: Long?) {
        engineCall { engine.moveFiles(messageIds.map { it.toInt() }, sourceFolderId, targetFolderId) }
    }

    override suspend fun searchGlobal(query: String): List<TelegramFile> =
        engineCall { engine.searchGlobal(query).map { it.toTelegramFile(null) } }

    override suspend fun uploadFile(path: String, folderId: Long?, transferId: String) {
        val tid = transferId.ifEmpty { java.util.UUID.randomUUID().toString() }
        engineCall { engine.uploadFile(path, folderId, tid) }
    }

    override suspend fun downloadFile(messageId: Long, savePath: String, folderId: Long?, transferId: String) {
        val tid = transferId.ifEmpty { java.util.UUID.randomUUID().toString() }
        engineCall { engine.downloadFile(messageId.toInt(), folderId, savePath, tid) }
    }

    override suspend fun cancelTransfer(transferId: String) {
        engineCall { engine.cancelTransfer(transferId) }
    }

    override suspend fun getThumbnail(messageId: Long, folderId: Long?): String? =
        engineCall { engine.getThumbnail(messageId.toInt(), folderId).takeIf { it.isNotEmpty() } }

    override suspend fun getPreview(messageId: Long, folderId: Long?): String? =
        engineCall { engine.getPreview(messageId.toInt(), folderId).takeIf { it.isNotEmpty() } }

    override suspend fun getStreamInfo(): com.telebox.app.data.StreamInfo =
        engineCall {
            val rustInfo = engine.getStreamInfo()
            com.telebox.app.data.StreamInfo(token = rustInfo.token, baseUrl = rustInfo.baseUrl)
        }

    override fun streamUrl(folderId: Long?, fileId: Long, info: com.telebox.app.data.StreamInfo): String {
        val folderParam = folderId?.toString() ?: "home"
        return "${info.baseUrl}/stream/$folderParam/$fileId?token=${info.token}"
    }

    private fun FileMetadata.toTelegramFile(folderId: Long?): TelegramFile {
        return TelegramFile(
            id = this.id,
            name = this.name,
            size = this.size,
            sizeStr = formatBytes(this.size),
            createdAt = null,
            type = ItemType.FILE,
            iconType = null
        )
    }
}
