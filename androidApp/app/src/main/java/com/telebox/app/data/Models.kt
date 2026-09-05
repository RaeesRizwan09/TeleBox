package com.telebox.app.data

enum class ItemType {
    FOLDER,
    FILE
}

enum class TransferStatus {
    PENDING,
    UPLOADING,
    DOWNLOADING,
    SUCCESS,
    ERROR,
    CANCELLED
}

enum class AuthStatus {
    LOADING,
    AUTHENTICATED,
    UNAUTHENTICATED
}

enum class AuthStep {
    SETUP,
    PHONE,
    CODE,
    PASSWORD
}

enum class LoginMethod {
    PHONE,
    QR
}

enum class ViewMode {
    GRID,
    LIST
}

enum class LibrarySection {
    ALL,
    VIDEOS,
    PICTURES,
    DOCUMENTS,
    OTHERS
}

data class LibraryStats(
    val videosCount: Int = 0,
    val videosBytes: Long = 0L,
    val picturesCount: Int = 0,
    val picturesBytes: Long = 0L,
    val documentsCount: Int = 0,
    val documentsBytes: Long = 0L,
    val othersCount: Int = 0,
    val othersBytes: Long = 0L
) {
    fun countFor(section: LibrarySection): Int = when (section) {
        LibrarySection.ALL -> videosCount + picturesCount + documentsCount + othersCount
        LibrarySection.VIDEOS -> videosCount
        LibrarySection.PICTURES -> picturesCount
        LibrarySection.DOCUMENTS -> documentsCount
        LibrarySection.OTHERS -> othersCount
    }

    fun bytesFor(section: LibrarySection): Long = when (section) {
        LibrarySection.ALL -> videosBytes + picturesBytes + documentsBytes + othersBytes
        LibrarySection.VIDEOS -> videosBytes
        LibrarySection.PICTURES -> picturesBytes
        LibrarySection.DOCUMENTS -> documentsBytes
        LibrarySection.OTHERS -> othersBytes
    }
}

data class LocalDownload(
    val path: String,
    val name: String,
    val size: Long,
    val sizeStr: String,
    val modifiedAt: Long
)

enum class SortField {
    NAME,
    SIZE,
    DATE
}

enum class SortDirection {
    ASC,
    DESC
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ConfirmVariant {
    DANGER,
    INFO
}

data class TelegramFile(
    val id: Long,
    val name: String,
    val size: Long,
    val sizeStr: String,
    val createdAt: String? = null,
    val type: ItemType = ItemType.FILE,
    val iconType: String? = null,
    val folderId: Long? = null
)

data class TelegramFolder(
    val id: Long,
    val name: String,
    val parentId: Long? = null
)

data class QueueItem(
    val id: String,
    val path: String,
    val folderId: Long?,
    val status: TransferStatus = TransferStatus.PENDING,
    val error: String? = null,
    val progress: Float? = null,
    val uploadedBytes: Long? = null,
    val totalBytes: Long? = null,
    val speedBytesPerSec: Long? = null
)

data class DownloadItem(
    val id: String,
    val messageId: Long,
    val filename: String,
    val folderId: Long?,
    val status: TransferStatus = TransferStatus.PENDING,
    val error: String? = null,
    val progress: Float? = null,
    val uploadedBytes: Long? = null,
    val totalBytes: Long? = null,
    val speedBytesPerSec: Long? = null,
    val localPath: String? = null
)

data class BandwidthStats(
    val upBytes: Long = 0L,
    val downBytes: Long = 0L
)

data class StreamInfo(
    val token: String,
    val baseUrl: String
)

data class AuthResult(
    val success: Boolean,
    val nextStep: String? = null
)

data class ConfirmOptions(
    val title: String,
    val message: String,
    val confirmText: String = "Confirm",
    val cancelText: String = "Cancel",
    val variant: ConfirmVariant = ConfirmVariant.INFO
)

data class UpdateState(
    val checking: Boolean = false,
    val available: Boolean = false,
    val downloading: Boolean = false,
    val progress: Int = 0,
    val error: String? = null,
    val version: String? = null
)

data class ProgressPayload(
    val id: String,
    val percent: Float,
    val uploadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long
)

data class ToastMessage(
    val id: Long,
    val text: String,
    val isError: Boolean = false
)
