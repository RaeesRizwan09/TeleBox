package com.telebox.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.data.StreamInfo
import com.telebox.app.data.TelegramFile
import com.telebox.app.ui.theme.TeleBoxTheme
import com.telebox.app.util.isAudioFile
import com.telebox.app.util.isVideoFile

@Composable
fun MediaPlayerDialog(
    file: TelegramFile,
    streamInfo: StreamInfo?,
    currentIndex: Int,
    totalItems: Int,
    streamUrl: String?,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    val colors = TeleBoxTheme.colors
    val isVideo = isVideoFile(file.name)
    val isAudio = isAudioFile(file.name)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(onClick = onClose)
            .padding(16.dp)
    ) {
        IconButton(
            onClick = onPrev,
            modifier = Modifier.align(Alignment.CenterStart).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous", tint = Color.White)
        }
        IconButton(
            onClick = onNext,
            modifier = Modifier.align(Alignment.CenterEnd).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Next", tint = Color.White)
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                when {
                    streamUrl == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(40.dp), strokeWidth = 4.dp)
                        Text("Preparing stream...", color = Color.White, modifier = Modifier.padding(top = 16.dp))
                    }
                    isVideo -> Text("Video stream ready\n$streamUrl", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    isAudio -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.2f), Color.Black))),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 48.dp)
                                .size(128.dp)
                                .clip(CircleShape)
                                .background(colors.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Audiotrack, contentDescription = null, tint = colors.primary, modifier = Modifier.size(48.dp))
                        }
                        Text("Audio stream ready", color = Color.White, modifier = Modifier.padding(top = 24.dp))
                    }
                    else -> Text("Unsupported media type", color = Color.White)
                }
            }
            Text(file.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 16.dp))
            Text(
                text = if (totalItems > 0) "Streaming from Telegram Drive  •  ${currentIndex + 1}/$totalItems" else "Streaming from Telegram Drive",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }
    }
}
