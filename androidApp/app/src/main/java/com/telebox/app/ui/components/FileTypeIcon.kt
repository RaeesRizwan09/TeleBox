package com.telebox.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.telebox.app.ui.theme.TeleBoxTheme
import com.telebox.app.util.fileExtension

enum class FileIconSize(val dp: Dp, val container: Dp, val radius: Dp) {
    SM(18.dp, 36.dp, 10.dp),
    MD(22.dp, 44.dp, 12.dp),
    LG(32.dp, 64.dp, 16.dp)
}

data class FileTypeInfo(
    val icon: ImageVector,
    val color: Color,
    val label: String
)

fun fileTypeInfo(
    filename: String,
    image: Color,
    video: Color,
    audio: Color,
    pdf: Color,
    document: Color,
    text: Color,
    spreadsheet: Color,
    presentation: Color,
    archive: Color,
    code: Color,
    fallback: Color
): FileTypeInfo {
    return when (fileExtension(filename)) {
        "jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "heic", "heif", "avif", "tiff", "tif" ->
            FileTypeInfo(Icons.Outlined.Image, image, "Image")
        "mp4", "mov", "avi", "mkv", "webm", "m4v", "3gp", "flv", "wmv" ->
            FileTypeInfo(Icons.Outlined.Movie, video, "Video")
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "opus", "wma", "aiff" ->
            FileTypeInfo(Icons.Outlined.AudioFile, audio, "Audio")
        "pdf" -> FileTypeInfo(Icons.Outlined.PictureAsPdf, pdf, "PDF")
        "doc", "docx", "odt", "pages" -> FileTypeInfo(Icons.Outlined.Description, document, "Document")
        "txt", "rtf", "md", "epub", "mobi" -> FileTypeInfo(Icons.Outlined.TextSnippet, text, "Text")
        "xls", "xlsx", "csv", "ods" -> FileTypeInfo(Icons.Outlined.TableChart, spreadsheet, "Spreadsheet")
        "ppt", "pptx", "key", "odp" -> FileTypeInfo(Icons.Outlined.Slideshow, presentation, "Presentation")
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso" ->
            FileTypeInfo(Icons.Outlined.FolderZip, archive, "Archive")
        "js", "ts", "jsx", "tsx", "py", "rs", "go", "java", "kt", "kts",
        "html", "css", "json", "xml", "yml", "yaml", "toml", "sh",
        "c", "cpp", "h", "hpp", "cs", "swift", "rb", "php", "sql" ->
            FileTypeInfo(Icons.Outlined.Code, code, "Code")
        else -> FileTypeInfo(Icons.Outlined.InsertDriveFile, fallback, "File")
    }
}

@Composable
fun rememberFileTypeInfo(filename: String): FileTypeInfo {
    val colors = TeleBoxTheme.colors
    return fileTypeInfo(
        filename = filename,
        image = colors.fileImage,
        video = colors.fileVideo,
        audio = colors.fileAudio,
        pdf = colors.filePdf,
        document = colors.fileDocument,
        text = colors.fileText,
        spreadsheet = colors.fileSpreadsheet,
        presentation = colors.filePresentation,
        archive = colors.fileArchive,
        code = colors.fileCode,
        fallback = colors.fileGeneric
    )
}

@Composable
fun FileTypeIcon(
    filename: String,
    modifier: Modifier = Modifier,
    size: FileIconSize = FileIconSize.MD,
    framed: Boolean = true
) {
    val info = rememberFileTypeInfo(filename)
    if (framed) {
        Box(
            modifier = modifier
                .size(size.container)
                .clip(RoundedCornerShape(size.radius))
                .background(info.color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = info.icon,
                contentDescription = info.label,
                tint = info.color,
                modifier = Modifier.size(size.dp)
            )
        }
    } else {
        Icon(
            imageVector = info.icon,
            contentDescription = info.label,
            tint = info.color,
            modifier = modifier.size(size.dp)
        )
    }
}

@Composable
fun FolderTypeIcon(
    modifier: Modifier = Modifier,
    size: FileIconSize = FileIconSize.MD
) {
    val colors = TeleBoxTheme.colors
    Box(
        modifier = modifier
            .size(size.container)
            .clip(RoundedCornerShape(size.radius))
            .background(colors.fileFolder.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Folder,
            contentDescription = "Folder",
            tint = colors.fileFolder,
            modifier = Modifier.size(size.dp)
        )
    }
}
