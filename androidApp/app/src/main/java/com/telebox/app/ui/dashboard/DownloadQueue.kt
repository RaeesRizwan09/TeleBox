package com.telebox.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.telebox.app.data.DownloadItem
import com.telebox.app.data.TransferStatus

@Composable
fun DownloadQueuePanel(
    items: List<DownloadItem>,
    compact: Boolean,
    onClearFinished: () -> Unit,
    onCancelAll: () -> Unit,
    onCancelItem: (String) -> Unit,
    onRetryItem: (String) -> Unit
) {
    if (items.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val activeCount = items.count { it.status == TransferStatus.PENDING || it.status == TransferStatus.DOWNLOADING }
    val completedCount = items.count { it.status == TransferStatus.SUCCESS }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = null,
                        tint = scheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Downloads",
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    if (activeCount > 0) {
                        Text(
                            "$activeCount active",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.secondary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Row {
                    if (activeCount > 0) {
                        TextButton(onClick = onCancelAll) {
                            Text("Cancel all", color = scheme.error)
                        }
                    }
                    if (completedCount > 0) {
                        TextButton(onClick = onClearFinished) {
                            Text("Clear")
                        }
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
                        name = item.filename,
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
