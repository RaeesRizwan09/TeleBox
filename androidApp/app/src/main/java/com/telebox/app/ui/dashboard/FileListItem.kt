package com.telebox.app.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.telebox.app.data.ItemType
import com.telebox.app.data.TelegramFile
import com.telebox.app.ui.components.FileIconSize
import com.telebox.app.ui.components.FileTypeIcon
import com.telebox.app.ui.components.FolderTypeIcon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    file: TelegramFile,
    selected: Boolean,
    compact: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isFolder = file.type == ItemType.FOLDER
    val meta = buildString {
        append(file.sizeStr)
        if (!compact && !file.createdAt.isNullOrBlank()) {
            append("  •  ")
            append(file.createdAt)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) scheme.secondaryContainer else scheme.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selecting || selected) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (selected) scheme.primary else scheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (isFolder) {
                        FolderTypeIcon(size = FileIconSize.SM)
                    } else {
                        FileTypeIcon(filename = file.name, size = FileIconSize.SM)
                    }
                }
            } else if (isFolder) {
                FolderTypeIcon(size = FileIconSize.MD)
            } else {
                FileTypeIcon(filename = file.name, size = FileIconSize.MD)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                file.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) scheme.onSecondaryContainer else scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                meta,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!selecting) {
            IconButton(onClick = onMore) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "More actions",
                    tint = scheme.onSurfaceVariant
                )
            }
        }
    }
}
