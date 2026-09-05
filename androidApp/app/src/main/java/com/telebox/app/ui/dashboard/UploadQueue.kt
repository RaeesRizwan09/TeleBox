package com.telebox.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.telebox.app.data.QueueItem
import com.telebox.app.data.TransferStatus
import com.telebox.app.ui.theme.TeleBoxTheme
import com.telebox.app.util.formatBytesShort

@Composable
fun UploadQueuePanel(
    items: List<QueueItem>,
    compact: Boolean,
    onClearFinished: () -> Unit,
    onCancelAll: () -> Unit,
    onCancelItem: (String) -> Unit,
    onRetryItem: (String) -> Unit
) {
    if (items.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val hasPendingOrActive = items.any { it.status == TransferStatus.PENDING || it.status == TransferStatus.UPLOADING }

    Surface(
        modifier = Modifier.fillMaxWidth(if (compact) 1f else 0.92f),
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Uploads",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface
                )
                Row {
                    if (hasPendingOrActive) {
                        TextButton(onClick = onCancelAll) {
                            Text("Cancel all", color = scheme.error)
                        }
                    }
                    TextButton(onClick = onClearFinished) {
                        Text("Clear")
                    }
                }
            }
            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 220.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
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
                        activeColor = scheme.secondary,
                        onCancel = { onCancelItem(item.id) },
                        onRetry = { onRetryItem(item.id) }
                    )
                }
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
    val scheme = MaterialTheme.colorScheme
    val dot = when (status) {
        TransferStatus.PENDING -> colors.warning
        TransferStatus.UPLOADING, TransferStatus.DOWNLOADING -> activeColor
        TransferStatus.CANCELLED -> scheme.onSurfaceVariant
        TransferStatus.ERROR -> scheme.error
        TransferStatus.SUCCESS -> colors.success
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(scheme.surface)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dot))
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            when (status) {
                TransferStatus.UPLOADING, TransferStatus.DOWNLOADING, TransferStatus.PENDING -> {
                    IconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                    }
                }
                TransferStatus.ERROR, TransferStatus.CANCELLED -> {
                    IconButton(onClick = onRetry, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Replay, contentDescription = "Retry", modifier = Modifier.size(18.dp))
                    }
                }
                else -> Unit
            }
        }
        if (status == TransferStatus.UPLOADING || status == TransferStatus.DOWNLOADING) {
            LinearProgressIndicator(
                progress = { ((progress ?: 0f) / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = activeColor,
                trackColor = scheme.surfaceContainerHighest
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (uploadedBytes != null && totalBytes != null) {
                        "${formatBytesShort(uploadedBytes)} / ${formatBytesShort(totalBytes)}"
                    } else if (progress != null) {
                        "${progress.toInt()}%"
                    } else {
                        ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Text(
                    if (speed != null && speed > 0) "${formatBytesShort(speed)}/s" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        if (status == TransferStatus.ERROR && error != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = scheme.error, modifier = Modifier.size(14.dp))
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        if (status == TransferStatus.CANCELLED) {
            Text(
                "Cancelled",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
