package com.telebox.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.telebox.app.ui.theme.FileArchive
import com.telebox.app.ui.theme.FileAudio
import com.telebox.app.ui.theme.FileCode
import com.telebox.app.ui.theme.FileDocument
import com.telebox.app.ui.theme.FileImage
import com.telebox.app.ui.theme.FilePdf
import com.telebox.app.ui.theme.FilePresentation
import com.telebox.app.ui.theme.FileSpreadsheet
import com.telebox.app.ui.theme.FileText
import com.telebox.app.ui.theme.FileVideo
import com.telebox.app.ui.theme.TeleBoxTheme
import com.telebox.app.util.fileExtension

enum class FileIconSize(val dp: Dp) {
    SM(20.dp),
    MD(40.dp),
    LG(48.dp)
}

data class FileTypeInfo(
    val icon: ImageVector,
    val color: Color
)

fun fileTypeInfo(filename: String, fallback: Color): FileTypeInfo {
    return when (fileExtension(filename)) {
        "jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "heic" ->
            FileTypeInfo(Icons.Outlined.Image, FileImage)
        "mp4", "mov", "avi", "mkv", "webm" ->
            FileTypeInfo(Icons.Outlined.VideoFile, FileVideo)
        "mp3", "wav", "flac", "aac", "ogg" ->
            FileTypeInfo(Icons.Outlined.AudioFile, FileAudio)
        "pdf" -> FileTypeInfo(Icons.Outlined.PictureAsPdf, FilePdf)
        "doc", "docx" -> FileTypeInfo(Icons.Outlined.Description, FileDocument)
        "txt", "rtf", "md" -> FileTypeInfo(Icons.Outlined.Description, FileText)
        "xls", "xlsx", "csv" -> FileTypeInfo(Icons.Outlined.TableChart, FileSpreadsheet)
        "ppt", "pptx", "key" -> FileTypeInfo(Icons.Outlined.Slideshow, FilePresentation)
        "zip", "rar", "7z", "tar", "gz" -> FileTypeInfo(Icons.Outlined.FolderZip, FileArchive)
        "js", "ts", "jsx", "tsx", "py", "rs", "go", "java", "html", "css", "json", "kt" ->
            FileTypeInfo(Icons.Outlined.Code, FileCode)
        else -> FileTypeInfo(Icons.Outlined.InsertDriveFile, fallback)
    }
}

@Composable
fun FileTypeIcon(
    filename: String,
    modifier: Modifier = Modifier,
    size: FileIconSize = FileIconSize.MD
) {
    val info = fileTypeInfo(filename, TeleBoxTheme.colors.subtext)
    Icon(
        imageVector = info.icon,
        contentDescription = filename,
        tint = info.color,
        modifier = modifier.size(size.dp)
    )
}
