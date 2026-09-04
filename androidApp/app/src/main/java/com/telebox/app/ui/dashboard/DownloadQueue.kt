package com.telebox.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.data.DownloadItem
import com.telebox.app.data.TransferStatus
import com.telebox.app.ui.theme.TeleBoxTheme

@Composable
fun DownloadQueuePanel(
    items: List<DownloadItem>,
    onClearFinished: () -> Unit,
    onCancelAll: () -> Unit,
    onCancelItem: (String) -> Unit,
    onRetryItem: (String) -> Unit
) {
    if (items.isEmpty()) return
    val colors = TeleBoxTheme.colors
    val activeCount = items.count { it.status == TransferStatus.PENDING || it.status == TransferStatus.DOWNLOADING }
    val completedCount = items.count { it.status == TransferStatus.SUCCESS }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Download, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(16.dp))
                Text("Downloads", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp))
                if (activeCount > 0) {
                    Text(
                        "$activeCount active",
                        color = colors.secondary,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(colors.secondary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Row {
                if (activeCount > 0) {
                    TextButton(onClick = onCancelAll) {
                        Text("Cancel All", color = colors.danger, fontSize = 12.sp)
                    }
                }
                if (completedCount > 0) {
                    TextButton(onClick = onClearFinished) {
                        Text("Clear Finished", color = colors.primary, fontSize = 12.sp)
                    }
                }
            }
        }
        LazyColumn(
            modifier = Modifier.heightIn(max = 240.dp).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.id }) { item ->
                QueueRow(
                    name = item.filename,
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
