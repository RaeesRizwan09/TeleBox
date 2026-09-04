package com.telebox.app.util

import com.telebox.app.data.ItemType
import com.telebox.app.data.TelegramFile
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "ogg", "mov", "mkv", "avi")
private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "aac", "flac", "m4a", "opus")
private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif")
private val THUMBNAIL_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

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
