package com.telebox.app.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.telebox.app.data.ItemType
import com.telebox.app.data.TelegramFile
import com.telebox.app.util.isMediaFile
import com.telebox.app.util.isPdfFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileContextMenu(
    file: TelegramFile,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                file.name,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Text(
                file.sizeStr,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 0.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            if (file.type != ItemType.FOLDER) {
                val (icon, label) = when {
                    isMediaFile(file.name) -> Icons.Outlined.PlayArrow to "Play"
                    isPdfFile(file.name) -> Icons.Outlined.PictureAsPdf to "View PDF"
                    else -> Icons.Outlined.Visibility to "Preview"
                }
                SheetAction(icon, label) { onPreview() }
            } else {
                SheetAction(Icons.Outlined.FolderOpen, "Open") { onPreview() }
            }
            SheetAction(Icons.Outlined.CheckCircle, "Select") { onSelect() }
            SheetAction(Icons.Outlined.Download, "Download") { onDownload() }
            SheetAction(Icons.Outlined.Delete, "Delete", destructive = true) { onDelete() }
        }
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (destructive) scheme.error else scheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (destructive) scheme.error else scheme.onSurface,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
