package com.telebox.app.data

import com.telebox.app.util.formatBytes
import com.telebox.app.util.randomId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap

interface TelegramRepository {
    val uploadProgress: SharedFlow<ProgressPayload>
    val downloadProgress: SharedFlow<ProgressPayload>

    suspend fun connect(apiId: Int)
    suspend fun checkConnection(): Boolean
    suspend fun isNetworkAvailable(): Boolean
    suspend fun requestAuthCode(phone: String, apiId: Int, apiHash: String)
    suspend fun signIn(code: String): AuthResult
    suspend fun checkPassword(password: String): AuthResult
    suspend fun qrLogin(apiId: Int, apiHash: String): String
    suspend fun qrPoll(): AuthResult
    suspend fun logout()
    suspend fun cleanCache()
    suspend fun getFiles(folderId: Long?): List<TelegramFile>
    suspend fun getBandwidth(): BandwidthStats
    suspend fun scanFolders(): List<TelegramFolder>
    suspend fun createFolder(name: String): TelegramFolder
    suspend fun deleteFolder(folderId: Long)
    suspend fun deleteFile(messageId: Long, folderId: Long?)
    suspend fun moveFiles(messageIds: List<Long>, sourceFolderId: Long?, targetFolderId: Long?)
    suspend fun searchGlobal(query: String): List<TelegramFile>
    suspend fun uploadFile(path: String, folderId: Long?, transferId: String)
    suspend fun downloadFile(messageId: Long, savePath: String, folderId: Long?, transferId: String)
    suspend fun cancelTransfer(transferId: String)
    suspend fun getThumbnail(messageId: Long, folderId: Long?): String?
    suspend fun getPreview(messageId: Long, folderId: Long?): String?
    suspend fun getStreamInfo(): StreamInfo
    fun streamUrl(folderId: Long?, fileId: Long, info: StreamInfo): String
    suspend fun prepareMediaPlayback(messageId: Long, folderId: Long?, fileName: String): String
}

class DemoTelegramRepository : TelegramRepository {

    private val _uploadProgress = MutableSharedFlow<ProgressPayload>(extraBufferCapacity = 64)
    private val _downloadProgress = MutableSharedFlow<ProgressPayload>(extraBufferCapacity = 64)
    override val uploadProgress: SharedFlow<ProgressPayload> = _uploadProgress.asSharedFlow()
    override val downloadProgress: SharedFlow<ProgressPayload> = _downloadProgress.asSharedFlow()

    private val cancelled = ConcurrentHashMap.newKeySet<String>()
    private val filesByFolder = ConcurrentHashMap<Long, MutableList<TelegramFile>>()
    private val rootFiles = mutableListOf<TelegramFile>()
    private val folders = mutableListOf<TelegramFolder>()
    private var connected = false
    private var authorized = false
    private var qrAuthorized = false
    private var passwordRequired = false
    private var bandwidth = BandwidthStats(upBytes = 12L * 1024 * 1024, downBytes = 48L * 1024 * 1024)
    private var nextFileId = 1000L
    private var nextFolderId = 10L

    init {
        seed()
    }

    private fun seed() {
        folders.clear()
        folders += TelegramFolder(1, "Documents")
        folders += TelegramFolder(2, "Photos")
        folders += TelegramFolder(3, "Work")
        rootFiles.clear()
        rootFiles += file(101, "Quarterly Report.pdf", 2_400_000, "2026-08-12")
        rootFiles += file(102, "family-trip.mp4", 85_000_000, "2026-07-01")
        rootFiles += file(103, "notes.md", 12_400, "2026-08-20")
        rootFiles += file(104, "cover.jpg", 1_250_000, "2026-06-11")
        rootFiles += file(105, "soundtrack.mp3", 8_200_000, "2026-05-02")
        rootFiles += file(106, "archive.zip", 44_000_000, "2026-04-18")
        rootFiles += file(107, "budget.xlsx", 320_000, "2026-08-01")
        rootFiles += file(108, "deck.pptx", 5_100_000, "2026-03-22")
        filesByFolder[1] = mutableListOf(
            file(201, "contract.docx", 240_000, "2026-02-14"),
            file(202, "readme.txt", 4_200, "2026-01-09")
        )
        filesByFolder[2] = mutableListOf(
            file(301, "sunset.png", 3_400_000, "2026-07-21"),
            file(302, "portrait.heic", 2_100_000, "2026-07-22")
        )
        filesByFolder[3] = mutableListOf(
            file(401, "api-spec.json", 18_000, "2026-08-28"),
            file(402, "main.kt", 6_800, "2026-08-29")
        )
    }

    private fun file(id: Long, name: String, size: Long, created: String) = TelegramFile(
        id = id,
        name = name,
        size = size,
        sizeStr = formatBytes(size),
        createdAt = created,
        type = ItemType.FILE
    )

    private fun listFor(folderId: Long?): MutableList<TelegramFile> {
        return if (folderId == null) rootFiles else filesByFolder.getOrPut(folderId) { mutableListOf() }
    }

    override suspend fun connect(apiId: Int) {
        delay(250)
        connected = true
    }

    override suspend fun checkConnection(): Boolean {
        delay(200)
        return connected && authorized
    }

    override suspend fun isNetworkAvailable(): Boolean = true

    override suspend fun requestAuthCode(phone: String, apiId: Int, apiHash: String) {
        delay(400)
        if (phone.contains("flood", ignoreCase = true)) {
            error("FLOOD_WAIT_32")
        }
        connected = true
    }

    override suspend fun signIn(code: String): AuthResult {
        delay(350)
        if (code.trim() == "22222") {
            passwordRequired = true
            return AuthResult(success = false, nextStep = "password")
        }
        if (code.isBlank()) return AuthResult(false)
        authorized = true
        return AuthResult(true)
    }

    override suspend fun checkPassword(password: String): AuthResult {
        delay(350)
        if (password.isBlank()) return AuthResult(false)
        authorized = true
        passwordRequired = false
        return AuthResult(true)
    }

    override suspend fun qrLogin(apiId: Int, apiHash: String): String {
        delay(300)
        if (authorized) return "__authorized__"
        qrAuthorized = false
        return "tg://login?token=telebox-demo-qr-${randomId()}"
    }

    override suspend fun qrPoll(): AuthResult {
        delay(200)
        return if (qrAuthorized) {
            authorized = true
            AuthResult(true)
        } else {
            AuthResult(false, nextStep = "waiting")
        }
    }

    override suspend fun logout() {
        delay(150)
        authorized = false
        connected = false
        qrAuthorized = false
    }

    override suspend fun cleanCache() {
        delay(50)
    }

    override suspend fun getFiles(folderId: Long?): List<TelegramFile> {
        delay(180)
        return listFor(folderId).toList()
    }

    override suspend fun getBandwidth(): BandwidthStats = bandwidth

    override suspend fun scanFolders(): List<TelegramFolder> {
        delay(400)
        return folders.toList()
    }

    override suspend fun createFolder(name: String): TelegramFolder {
        delay(250)
        val folder = TelegramFolder(nextFolderId++, name)
        folders += folder
        filesByFolder[folder.id] = mutableListOf()
        return folder
    }

    override suspend fun deleteFolder(folderId: Long) {
        delay(250)
        folders.removeAll { it.id == folderId }
        filesByFolder.remove(folderId)
    }

    override suspend fun deleteFile(messageId: Long, folderId: Long?) {
        delay(180)
        listFor(folderId).removeAll { it.id == messageId }
    }

    override suspend fun moveFiles(messageIds: List<Long>, sourceFolderId: Long?, targetFolderId: Long?) {
        delay(220)
        val source = listFor(sourceFolderId)
        val moving = source.filter { it.id in messageIds }
        source.removeAll { it.id in messageIds }
        listFor(targetFolderId).addAll(moving)
    }

    override suspend fun searchGlobal(query: String): List<TelegramFile> {
        delay(250)
        val q = query.lowercase()
        val all = rootFiles + filesByFolder.values.flatten()
        return all.filter { it.name.lowercase().contains(q) }
    }

    override suspend fun uploadFile(path: String, folderId: Long?, transferId: String) {
        val file = File(path)
        val total = if (file.exists()) file.length().coerceAtLeast(1L) else 4_000_000L
        var uploaded = 0L
        while (uploaded < total) {
            if (cancelled.remove(transferId)) error("Transfer cancelled")
            delay(180)
            uploaded = (uploaded + total / 8).coerceAtMost(total)
            val percent = (uploaded * 100f / total)
            _uploadProgress.emit(
                ProgressPayload(transferId, percent, uploaded, total, 420_000)
            )
        }
        val name = file.name.ifBlank { path.substringAfterLast('/') }
        val newFile = file(nextFileId++, name, total, "2026-09-03")
        listFor(folderId).add(0, newFile)
        bandwidth = bandwidth.copy(upBytes = bandwidth.upBytes + total)
    }

    override suspend fun downloadFile(messageId: Long, savePath: String, folderId: Long?, transferId: String) {
        val item = listFor(folderId).find { it.id == messageId }
        val total = item?.size?.coerceAtLeast(1L) ?: 1_000_000L
        var uploaded = 0L
        while (uploaded < total) {
            if (cancelled.remove(transferId)) error("Transfer cancelled")
            delay(160)
            uploaded = (uploaded + total / 10).coerceAtMost(total)
            val percent = (uploaded * 100f / total)
            _downloadProgress.emit(
                ProgressPayload(transferId, percent, uploaded, total, 380_000)
            )
        }
        bandwidth = bandwidth.copy(downBytes = bandwidth.downBytes + total)
        runCatching {
            File(savePath).parentFile?.mkdirs()
            File(savePath).writeText("TeleBox download placeholder for ${item?.name ?: messageId}")
        }
    }

    override suspend fun cancelTransfer(transferId: String) {
        cancelled.add(transferId)
    }

    override suspend fun getThumbnail(messageId: Long, folderId: Long?): String? {
        delay(120)
        return null
    }

    override suspend fun getPreview(messageId: Long, folderId: Long?): String? {
        delay(250)
        return null
    }

    override suspend fun getStreamInfo(): StreamInfo {
        delay(120)
        return StreamInfo(token = "demo-token", baseUrl = "https://stream.local")
    }

    override fun streamUrl(folderId: Long?, fileId: Long, info: StreamInfo): String {
        val folderParam = folderId?.toString() ?: "home"
        return "${info.baseUrl}/stream/$folderParam/$fileId?token=${info.token}"
    }

    override suspend fun prepareMediaPlayback(messageId: Long, folderId: Long?, fileName: String): String {
        delay(180)
        val item = listFor(folderId).find { it.id == messageId }
        return streamUrl(folderId, messageId, StreamInfo(token = "demo-token", baseUrl = "https://stream.local"))
            .plus(if (item != null) "&name=${item.name}" else "")
    }

    fun authorizeQrForDemo() {
        qrAuthorized = true
    }
}
