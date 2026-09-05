package com.telebox.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File

fun copyPickedUriToCache(context: Context, uri: Uri): String? {
    val resolver = context.contentResolver
    runCatching {
        resolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
    var displayName: String? = null
    runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) displayName = cursor.getString(index)
            }
        }
    }
    if (displayName.isNullOrBlank()) {
        displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
    }
    displayName = displayName
        .substringAfterLast(':')
        .substringAfterLast('/')
        .ifBlank { "file" }
    if (!displayName.contains('.')) {
        val mime = resolver.getType(uri)
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        if (!ext.isNullOrBlank()) displayName = "$displayName.$ext"
    }
    val safeName = displayName.replace(Regex("[^A-Za-z0-9._\\- ()+]"), "_")
    val destDir = File(context.cacheDir, "uploads").apply { mkdirs() }
    val dest = File(destDir, "${System.currentTimeMillis()}_$safeName")
    return try {
        resolver.openInputStream(uri)?.use { input ->
            dest.outputStream().buffered().use { output -> input.copyTo(output) }
        } ?: return null
        dest.absolutePath
    } catch (_: Exception) {
        dest.delete()
        null
    }
}

fun mediaPlaybackUri(src: String): Uri {
    return when {
        src.startsWith("http://") ||
            src.startsWith("https://") ||
            src.startsWith("file://") ||
            src.startsWith("content://") -> Uri.parse(src)
        else -> Uri.fromFile(File(src))
    }
}
