package com.telebox.app.ui.dashboard

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.telebox.app.data.TelegramFile
import com.telebox.app.ui.theme.TeleBoxTheme
import com.telebox.app.util.isImageFile

/** Decode a `data:image/...;base64,...` URL into an [ImageBitmap]; null for any other input. */
internal fun base64ToImageBitmap(dataUrl: String): ImageBitmap? {
    if (!dataUrl.startsWith("data:image", ignoreCase = true)) return null
    val comma = dataUrl.indexOf(',')
    if (comma < 0) return null
    val bytes = runCatching { Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT) }.getOrNull() ?: return null
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    return bitmap.asImageBitmap()
}

@Composable
fun PreviewModal(
    file: TelegramFile,
    src: String?,
    loading: Boolean,
    error: String?,
    currentIndex: Int,
    totalItems: Int,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    val colors = TeleBoxTheme.colors
    var imageError by remember(src) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(onClick = onClose)
            .systemBarsPadding()
            .padding(12.dp)
    ) {
        IconButton(
            onClick = onPrev,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
        ) { Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(24.dp)) }
        IconButton(
            onClick = onNext,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
        ) { Icon(Icons.Outlined.ChevronRight, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(24.dp)) }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
        ) { Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White) }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                loading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(40.dp), strokeWidth = 4.dp)
                    Text("Loading preview...", color = Color.White, modifier = Modifier.padding(top = 16.dp))
                    Text("Downloading from Telegram...", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
                error != null -> Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(16.dp)
                ) {
                    Text("Preview Error", color = colors.danger, fontWeight = FontWeight.Bold)
                    Text(error, color = colors.danger, fontSize = 14.sp)
                }
                src != null && isImageFile(file.name) -> {
                    val bitmap = remember(src) { base64ToImageBitmap(src) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = file.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 48.dp)
                        )
                    } else if (!imageError) {
                        AsyncImage(
                            model = src,
                            contentDescription = file.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 48.dp),
                            onError = { imageError = true }
                        )
                    } else {
                        ImageErrorRetry(onRetry = { imageError = false })
                    }
                }
                else -> Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C1C))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.InsertDriveFile, contentDescription = null, tint = colors.primary, modifier = Modifier.size(64.dp))
                    Text(file.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    Text("Preview not supported in app.", color = Color.Gray, modifier = Modifier.padding(bottom = 24.dp))
                    Text("File type: ${file.name.substringAfterLast('.')}", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        Text(
            text = if (totalItems > 0) "${file.name}  ${currentIndex + 1}/$totalItems" else file.name,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun ImageErrorRetry(onRetry: () -> Unit) {
    val colors = TeleBoxTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
        Text("Couldn't load image", color = Color.White, modifier = Modifier.padding(top = 12.dp))
        Box(
            Modifier
                .clickable(onClick = onRetry)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.primary.copy(alpha = 0.2f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Retry", color = colors.primary, fontWeight = FontWeight.Medium)
        }
    }
}
