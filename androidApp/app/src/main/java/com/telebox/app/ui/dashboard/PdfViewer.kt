package com.telebox.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.data.TelegramFile
import com.telebox.app.ui.theme.TeleBoxTheme

@Composable
fun PdfViewerDialog(
    file: TelegramFile,
    loading: Boolean,
    error: String?,
    pageCount: Int,
    scale: Float,
    currentIndex: Int,
    totalItems: Int,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFitWidth: () -> Unit
) {
    val colors = TeleBoxTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(onClick = onClose)
            .systemBarsPadding()
            .padding(12.dp)
    ) {
        Text(
            file.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .widthIn(max = 280.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 72.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = onZoomOut) {
                Icon(Icons.Outlined.ZoomOut, contentDescription = "Zoom Out", tint = Color.White.copy(alpha = 0.8f))
            }
            Text("${(scale * 100).toInt()}%", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = onZoomIn) {
                Icon(Icons.Outlined.ZoomIn, contentDescription = "Zoom In", tint = Color.White.copy(alpha = 0.8f))
            }
            IconButton(onClick = onFitWidth) {
                Icon(Icons.Outlined.Fullscreen, contentDescription = "Fit Width", tint = Color.White.copy(alpha = 0.8f))
            }
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
        }
        IconButton(
            onClick = onPrev,
            modifier = Modifier.align(Alignment.CenterStart).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous", tint = Color.White)
        }
        IconButton(
            onClick = onNext,
            modifier = Modifier.align(Alignment.CenterEnd).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Next", tint = Color.White)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, bottom = 48.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                loading -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 80.dp)) {
                    CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(40.dp), strokeWidth = 4.dp)
                    Text("Loading document...", color = Color.White, modifier = Modifier.padding(top = 16.dp))
                    Text("Downloading from Telegram...", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
                error != null -> Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.danger.copy(alpha = 0.2f))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(error, color = Color.White, fontSize = 14.sp)
                }
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    val pages = if (pageCount > 0) pageCount else 1
                    repeat(pages) { index ->
                        Box(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                                .height((400 * scale).dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("PDF page ${index + 1}", color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
        Text(
            text = buildString {
                if (totalItems > 0) append("File ${currentIndex + 1} of $totalItems  ")
                append("$pageCount ${if (pageCount == 1) "page" else "pages"}")
            },
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}
