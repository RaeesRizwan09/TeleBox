package com.telebox.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.data.QueueItem
import com.telebox.app.data.TransferStatus
import com.telebox.app.ui.theme.TeleBoxTheme
import com.telebox.app.util.formatBytesShort

@Composable
fun UploadQueuePanel(
    items: List<QueueItem>,
    onClearFinished: () -> Unit,
    onCancelAll: () -> Unit,
    onCancelItem: (String) -> Unit,
    onRetryItem: (String) -> Unit
) {
    if (items.isEmpty()) return
    val colors = TeleBoxTheme.colors
    val hasPendingOrActive = items.any { it.status == TransferStatus.PENDING || it.status == TransferStatus.UPLOADING }
    Column(
        modifier = Modifier
            .width(320.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.hover)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Uploads", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Row {
                if (hasPendingOrActive) {
                    TextButton(onClick = onCancelAll) {
                        Text("Cancel All", color = colors.danger, fontSize = 12.sp)
                    }
                }
                TextButton(onClick = onClearFinished) {
                    Text("Clear Finished", color = colors.primary, fontSize = 12.sp)
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 240.dp)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.id }) { item ->
                QueueRow(
                    name = item.path.substringAfterLast('/'),
                    status = item.status,
                    progress = item.progress,
                    uploadedBytes = item.uploadedBytes,
                    totalBytes = item.totalBytes,
                    speed = item.speedBytesPerSec,
                    error = item.error,
                    activeColor = colors.secondary,
                    onCancel = { onCancelItem(item.id) },
                    onRetry = { onRetryItem(item.id) }
                )
            }
        }
    }
}

@Composable
fun QueueRow(
    name: String,
    status: TransferStatus,
    progress: Float?,
    uploadedBytes: Long?,
    totalBytes: Long?,
    speed: Long?,
    error: String?,
    activeColor: androidx.compose.ui.graphics.Color,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val colors = TeleBoxTheme.colors
    val dot = when (status) {
        TransferStatus.PENDING -> colors.warning
        TransferStatus.UPLOADING, TransferStatus.DOWNLOADING -> activeColor
        TransferStatus.CANCELLED -> colors.subtext
        TransferStatus.ERROR -> colors.danger
        TransferStatus.SUCCESS -> colors.success
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.hover)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dot))
            Text(
                name,
                color = colors.subtext,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 12.dp)
            )
            when (status) {
                TransferStatus.UPLOADING, TransferStatus.DOWNLOADING, TransferStatus.PENDING -> {
                    IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cancel", tint = colors.subtext, modifier = Modifier.size(14.dp))
                    }
                }
                TransferStatus.ERROR, TransferStatus.CANCELLED -> {
                    IconButton(onClick = onRetry, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.Replay, contentDescription = "Retry", tint = colors.subtext, modifier = Modifier.size(14.dp))
                    }
                }
                else -> Unit
            }
        }
        if (status == TransferStatus.UPLOADING || status == TransferStatus.DOWNLOADING) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(colors.border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(((progress ?: 0f) / 100f).coerceIn(0f, 1f))
                        .background(activeColor)
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (uploadedBytes != null && totalBytes != null) "${formatBytesShort(uploadedBytes)} / ${formatBytesShort(totalBytes)}"
                    else if (progress != null) "${progress.toInt()}%" else "",
                    color = colors.subtext,
                    fontSize = 10.sp
                )
                Text(
                    if (speed != null && speed > 0) "${formatBytesShort(speed)}/s" else "",
                    color = colors.subtext,
                    fontSize = 10.sp
                )
            }
        }
        if (status == TransferStatus.ERROR && error != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = colors.danger, modifier = Modifier.size(12.dp))
                Text(error, color = colors.danger, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 4.dp))
            }
        }
        if (status == TransferStatus.CANCELLED) {
            Text("Cancelled", color = colors.subtext, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
