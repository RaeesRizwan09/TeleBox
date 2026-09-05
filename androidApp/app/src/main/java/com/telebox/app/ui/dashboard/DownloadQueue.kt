package com.telebox.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.telebox.app.data.DownloadItem
import com.telebox.app.data.QueueItem
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
        shadowElevation = 8.dp
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(scheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            tint = scheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text("Downloads", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                        Text(
                            when {
                                activeCount > 0 -> "$activeCount in progress"
                                completedCount > 0 -> "$completedCount complete"
                                else -> "${items.size} items"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    if (activeCount > 0) {
                        TextButton(onClick = onCancelAll) {
                            Text("Cancel all", color = scheme.error)
                        }
                    }
                    if (completedCount > 0 || items.any { it.status == TransferStatus.ERROR || it.status == TransferStatus.CANCELLED }) {
                        TextButton(onClick = onClearFinished) {
                            Text("Clear")
                        }
                    }
                }
            }
            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 260.dp)
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
                        activeColor = scheme.tertiary,
                        onCancel = { onCancelItem(item.id) },
                        onRetry = { onRetryItem(item.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersSheet(
    uploads: List<QueueItem>,
    downloads: List<DownloadItem>,
    onDismiss: () -> Unit,
    onClearFinishedUploads: () -> Unit,
    onCancelAllUploads: () -> Unit,
    onCancelUpload: (String) -> Unit,
    onRetryUpload: (String) -> Unit,
    onClearFinishedDownloads: () -> Unit,
    onCancelAllDownloads: () -> Unit,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableIntStateOf(if (downloads.isNotEmpty() && uploads.isEmpty()) 1 else 0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Download, contentDescription = null, tint = scheme.primary)
                Text(
                    "Transfer queue",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("Uploads (${uploads.size})") },
                    icon = { Icon(Icons.Outlined.CloudUpload, contentDescription = null) }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("Downloads (${downloads.size})") },
                    icon = { Icon(Icons.Outlined.CloudDownload, contentDescription = null) }
                )
            }
            val uploadsActive = uploads.any { it.status == TransferStatus.PENDING || it.status == TransferStatus.UPLOADING }
            val downloadsActive = downloads.any { it.status == TransferStatus.PENDING || it.status == TransferStatus.DOWNLOADING }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (tab == 0) {
                    if (uploadsActive) {
                        TextButton(onClick = onCancelAllUploads) {
                            Text("Cancel all", color = scheme.error)
                        }
                    }
                    if (uploads.any { it.status == TransferStatus.SUCCESS || it.status == TransferStatus.ERROR || it.status == TransferStatus.CANCELLED }) {
                        TextButton(onClick = onClearFinishedUploads) { Text("Clear finished") }
                    }
                } else {
                    if (downloadsActive) {
                        TextButton(onClick = onCancelAllDownloads) {
                            Text("Cancel all", color = scheme.error)
                        }
                    }
                    if (downloads.any { it.status == TransferStatus.SUCCESS || it.status == TransferStatus.ERROR || it.status == TransferStatus.CANCELLED }) {
                        TextButton(onClick = onClearFinishedDownloads) { Text("Clear finished") }
                    }
                }
            }
            if (tab == 0) {
                if (uploads.isEmpty()) {
                    EmptyQueueHint("No uploads in the queue")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uploads, key = { it.id }) { item ->
                            QueueRow(
                                name = item.path.substringAfterLast('/'),
                                status = item.status,
                                progress = item.progress,
                                uploadedBytes = item.uploadedBytes,
                                totalBytes = item.totalBytes,
                                speed = item.speedBytesPerSec,
                                error = item.error,
                                activeColor = scheme.primary,
                                onCancel = { onCancelUpload(item.id) },
                                onRetry = { onRetryUpload(item.id) }
                            )
                        }
                    }
                }
            } else {
                if (downloads.isEmpty()) {
                    EmptyQueueHint("No downloads in the queue")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(downloads, key = { it.id }) { item ->
                            QueueRow(
                                name = item.filename,
                                status = item.status,
                                progress = item.progress,
                                uploadedBytes = item.uploadedBytes,
                                totalBytes = item.totalBytes,
                                speed = item.speedBytesPerSec,
                                error = item.error,
                                activeColor = scheme.tertiary,
                                onCancel = { onCancelDownload(item.id) },
                                onRetry = { onRetryDownload(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyQueueHint(message: String) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    }
}
