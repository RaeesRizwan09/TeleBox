package com.telebox.app.data

import android.content.Context
import com.telebox.app.engine.*          // generated UniFFI bindings
import com.telebox.app.util.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real implementation of TelegramRepository that delegates to the Rust engine.
 * All engine calls are executed on Dispatchers.IO to avoid blocking the main thread.
 */
class RustTelegramRepository(
    private val context: Context,
    private val preferencesStore: PreferencesStore
) : TelegramRepository {

    // ─── Engine singleton ──────────────────────────────────────────────────────────
    private val engine: TelegramDriveEngine by lazy {
        TelegramDriveEngine().apply {
            // Must call setStoragePaths before any other operation.
            val dataDir = context.filesDir.absolutePath
            val cacheDir = context.cacheDir.absolutePath
            setStoragePaths(dataDir, cacheDir)
            // Register the progress listener (created below)
            setProgressListener(progressListener)
            // Optionally start the streaming server if you enabled the feature.
            // startStreamingServer() // see notes below
        }
    }

    // ─── Progress flows ──────────────────────────────────────────────────────────
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

    // ─── Progress listener implementation ──────────────────────────────────────
    private val progressListener = object : TransferProgressListener {
        override fun onUploadProgress(
            id: String,
            percent: Byte,
            transferredBytes: Long,
            totalBytes: Long,
            speedBytesPerSec: Long
        ) {
            // percent is Byte (0-100), convert to Float
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

    // ─── Cancellation tracking ──────────────────────────────────────────────────
    private val cancelledTransfers = ConcurrentHashMap.newKeySet<String>()

    // ─── Helper: run engine call on IO dispatcher ─────────────────────────────
    private suspend fun <T> engineCall(block: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: EngineError) {
                throw mapEngineError(e)
            } catch (e: Exception) {
                throw RuntimeException("Engine call failed", e)
            }
        }

    private fun mapEngineError(e: EngineError): Exception {
        // Convert flat EngineError into a more specific exception if needed
        // For simplicity, we wrap it in a generic RuntimeException.
        // You can also check e.message for known strings like "FLOOD_WAIT_*".
        return RuntimeException(e.message ?: "Telegram engine error")
    }

    // ─── Implement TelegramRepository methods ──────────────────────────────────

    override suspend fun connect(apiId: Int) {
        engineCall { engine.connect(apiId) }
    }

    override suspend fun checkConnection(): Boolean =
        engineCall { engine.checkConnection() }

    override suspend fun isNetworkAvailable(): Boolean =
        engineCall { engine.isNetworkAvailable() }

    override suspend fun requestAuthCode(phone: String, apiId: Int, apiHash: String) {
        engineCall {
            engine.requestLoginCode(phone, apiId, apiHash)
        }
    }

    override suspend fun signIn(code: String): AuthResult =
        engineCall {
            val result = engine.signIn(code)
            // result is AuthResult from Rust (success, nextStep, error)
            // Convert to app's AuthResult (which has success and nextStep)
            com.telebox.app.data.AuthResult(
                success = result.success,
                nextStep = result.nextStep
            )
        }

    override suspend fun checkPassword(password: String): AuthResult =
        engineCall {
            val result = engine.checkPassword(password)
            com.telebox.app.data.AuthResult(
                success = result.success,
                nextStep = result.nextStep
            )
        }

    override suspend fun qrLogin(apiId: Int, apiHash: String): String =
        engineCall { engine.authQrLogin(apiId, apiHash) }

    override suspend fun qrPoll(): AuthResult =
        engineCall {
            val result = engine.authQrPoll()
            com.telebox.app.data.AuthResult(
                success = result.success,
                nextStep = result.nextStep
            )
        }

    override suspend fun logout() {
        engineCall { engine.logout() }
    }

    override suspend fun cleanCache() {
        engineCall { engine.cleanCache() }
    }

    override suspend fun getFiles(folderId: Long?): List<TelegramFile> =
        engineCall {
            val rustFiles = engine.getFiles(folderId)
            rustFiles.map { it.toTelegramFile(folderId) }
        }

    override suspend fun getBandwidth(): BandwidthStats =
        engineCall {
            val stats = engine.getBandwidth()
            // stats is com.telebox.app.engine.BandwidthStats (date, upBytes, downBytes)
            com.telebox.app.data.BandwidthStats(
                upBytes = stats.upBytes,
                downBytes = stats.downBytes
            )
        }

    override suspend fun scanFolders(): List<TelegramFolder> =
        engineCall {
            val rustFolders = engine.scanFolders()
            rustFolders.map { rust ->
                TelegramFolder(
                    id = rust.id,
                    name = rust.name,
                    parentId = rust.parentId
                )
            }
        }

    override suspend fun createFolder(name: String): TelegramFolder =
        engineCall {
            val rustFolder = engine.createFolder(name)
            TelegramFolder(
                id = rustFolder.id,
                name = rustFolder.name,
                parentId = rustFolder.parentId
            )
        }

    override suspend fun deleteFolder(folderId: Long) {
        engineCall { engine.deleteFolder(folderId) }
    }

    override suspend fun deleteFile(messageId: Long, folderId: Long?) {
        // Rust expects messageId as Int (i32). Ensure it fits.
        engineCall { engine.deleteFile(messageId.toInt(), folderId) }
    }

    override suspend fun moveFiles(
        messageIds: List<Long>,
        sourceFolderId: Long?,
        targetFolderId: Long?
    ) {
        engineCall {
            val ids = messageIds.map { it.toInt() }
            engine.moveFiles(ids, sourceFolderId, targetFolderId)
        }
    }

    override suspend fun searchGlobal(query: String): List<TelegramFile> =
        engineCall {
            val rustFiles = engine.searchGlobal(query)
            rustFiles.map { it.toTelegramFile(null) } // folderId not available from search result
        }

    override suspend fun uploadFile(
        path: String,
        folderId: Long?,
        transferId: String
    ) {
        // If no transferId provided, generate one or pass empty string.
        val tid = transferId.ifEmpty { java.util.UUID.randomUUID().toString() }
        // Add to cancelled set? Not needed; engine handles cancellation via its own set.
        engineCall {
            engine.uploadFile(path, folderId, tid)
        }
    }

    override suspend fun downloadFile(
        messageId: Long,
        savePath: String,
        folderId: Long?,
        transferId: String
    ) {
        val tid = transferId.ifEmpty { java.util.UUID.randomUUID().toString() }
        engineCall {
            engine.downloadFile(messageId.toInt(), folderId, savePath, tid)
        }
    }

    override suspend fun cancelTransfer(transferId: String) {
        engineCall { engine.cancelTransfer(transferId) }
    }

    override suspend fun getThumbnail(messageId: Long, folderId: Long?): String? =
        engineCall {
            engine.getThumbnail(messageId.toInt(), folderId).takeIf { it.isNotEmpty() }
        }

    override suspend fun getPreview(messageId: Long, folderId: Long?): String? =
        engineCall {
            engine.getPreview(messageId.toInt(), folderId).takeIf { it.isNotEmpty() }
        }

    override suspend fun getStreamInfo(): StreamInfo =
        engineCall {
            val rustInfo = engine.getStreamInfo()
            com.telebox.app.data.StreamInfo(
                token = rustInfo.token,
                baseUrl = rustInfo.baseUrl
            )
        }

    override fun streamUrl(folderId: Long?, fileId: Long, info: StreamInfo): String {
        val folderParam = folderId?.toString() ?: "home"
        return "${info.baseUrl}/stream/$folderParam/$fileId?token=${info.token}"
    }

    // ─── Mapping extensions ──────────────────────────────────────────────────────
    private fun FileMetadata.toTelegramFile(folderId: Long?): TelegramFile {
        return TelegramFile(
            id = this.id,
            name = this.name,
            size = this.size,
            sizeStr = formatBytes(this.size),
            createdAt = this.createdAt,
            type = ItemType.FILE,
            iconType = this.iconType
        )
    }
}