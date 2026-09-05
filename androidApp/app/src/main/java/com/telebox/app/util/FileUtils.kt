package com.telebox.app.util

import com.telebox.app.data.ItemType
import com.telebox.app.data.LibrarySection
import com.telebox.app.data.LibraryStats
import com.telebox.app.data.TelegramFile
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mov", "mkv", "avi", "m4v", "3gp", "flv", "wmv")
private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "aac", "flac", "m4a", "opus", "ogg", "wma", "aiff")
private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif", "avif", "tiff", "tif")
private val THUMBNAIL_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
private val DOCUMENT_EXTENSIONS = setOf(
    "pdf", "doc", "docx", "txt", "rtf", "md", "odt", "pages",
    "xls", "xlsx", "csv", "ods", "ppt", "pptx", "key", "odp",
    "epub", "mobi"
)

fun formatBytes(bytes: Long, decimals: Int = 2): String {
    if (bytes <= 0L) return "0 Bytes"
    val k = 1024.0
    val dm = decimals.coerceAtLeast(0)
    val sizes = arrayOf("Bytes", "KB", "MB", "GB", "TB")
    val i = floor(ln(bytes.toDouble()) / ln(k)).toInt().coerceIn(0, sizes.lastIndex)
    val value = bytes / k.pow(i)
    val formatted = if (dm == 0) value.toInt().toString() else "%.${dm}f".format(value)
    return "$formatted ${sizes[i]}"
}

fun formatBytesShort(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val k = 1024.0
    val sizes = arrayOf("B", "KB", "MB", "GB")
    val i = floor(ln(bytes.toDouble()) / ln(k)).toInt().coerceIn(0, sizes.lastIndex)
    val value = bytes / k.pow(i)
    return "${"%.1f".format(value)} ${sizes[i]}"
}

fun fileExtension(name: String): String =
    name.substringAfterLast('.', "").lowercase()

private fun endsWithAny(name: String, exts: Set<String>): Boolean {
    val lower = name.lowercase()
    return exts.any { lower.endsWith(".$it") || lower.endsWith(it) }
}

fun isMediaFile(name: String): Boolean =
    endsWithAny(name, VIDEO_EXTENSIONS + AUDIO_EXTENSIONS)

fun isVideoFile(name: String): Boolean = endsWithAny(name, VIDEO_EXTENSIONS)

fun isAudioFile(name: String): Boolean = endsWithAny(name, AUDIO_EXTENSIONS)

fun isImageFile(name: String): Boolean = endsWithAny(name, IMAGE_EXTENSIONS)

fun isThumbnailFile(name: String): Boolean = endsWithAny(name, THUMBNAIL_EXTENSIONS)

fun isPdfFile(name: String): Boolean = name.lowercase().endsWith(".pdf")

fun isDocumentFile(name: String): Boolean = endsWithAny(name, DOCUMENT_EXTENSIONS)

fun isArchiveFile(name: String): Boolean =
    endsWithAny(name, setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso"))

fun isCodeFile(name: String): Boolean =
    endsWithAny(
        name,
        setOf(
            "js", "ts", "jsx", "tsx", "py", "rs", "go", "java", "kt", "kts",
            "html", "css", "json", "xml", "yml", "yaml", "toml", "sh", "c",
            "cpp", "h", "hpp", "cs", "swift", "rb", "php", "sql"
        )
    )

fun mimeTypeForFile(name: String): String = when (fileExtension(name)) {
    "mp4", "m4v" -> "video/mp4"
    "webm" -> "video/webm"
    "mov" -> "video/quicktime"
    "mkv" -> "video/x-matroska"
    "avi" -> "video/x-msvideo"
    "3gp" -> "video/3gpp"
    "flv" -> "video/x-flv"
    "wmv" -> "video/x-ms-wmv"
    "mp3" -> "audio/mpeg"
    "wav" -> "audio/wav"
    "aac" -> "audio/aac"
    "flac" -> "audio/flac"
    "m4a" -> "audio/mp4"
    "opus", "ogg" -> "audio/ogg"
    "wma" -> "audio/x-ms-wma"
    "aiff" -> "audio/aiff"
    else -> "application/octet-stream"
}

fun librarySectionFor(name: String): LibrarySection = when {
    isVideoFile(name) -> LibrarySection.VIDEOS
    isImageFile(name) -> LibrarySection.PICTURES
    isDocumentFile(name) -> LibrarySection.DOCUMENTS
    else -> LibrarySection.OTHERS
}

fun computeLibraryStats(files: List<TelegramFile>): LibraryStats {
    var videosCount = 0
    var videosBytes = 0L
    var picturesCount = 0
    var picturesBytes = 0L
    var documentsCount = 0
    var documentsBytes = 0L
    var othersCount = 0
    var othersBytes = 0L
    files.filter { it.type != ItemType.FOLDER }.forEach { file ->
        when (librarySectionFor(file.name)) {
            LibrarySection.VIDEOS -> {
                videosCount++
                videosBytes += file.size
            }
            LibrarySection.PICTURES -> {
                picturesCount++
                picturesBytes += file.size
            }
            LibrarySection.DOCUMENTS -> {
                documentsCount++
                documentsBytes += file.size
            }
            LibrarySection.OTHERS, LibrarySection.ALL -> {
                othersCount++
                othersBytes += file.size
            }
        }
    }
    return LibraryStats(
        videosCount = videosCount,
        videosBytes = videosBytes,
        picturesCount = picturesCount,
        picturesBytes = picturesBytes,
        documentsCount = documentsCount,
        documentsBytes = documentsBytes,
        othersCount = othersCount,
        othersBytes = othersBytes
    )
}

fun TelegramFile.withFormattedSize(): TelegramFile =
    copy(
        sizeStr = formatBytes(size),
        type = when {
            iconType == "folder" || name.endsWith("/") -> ItemType.FOLDER
            else -> type
        }
    )

fun randomId(): String {
    val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
    return (1..9).map { alphabet.random() }.joinToString("")
}

fun formatFloodWait(seconds: Int): String {
    val minutes = seconds / 60
    val remainder = seconds % 60
    return "$minutes:${remainder.toString().padStart(2, '0')}"
}
