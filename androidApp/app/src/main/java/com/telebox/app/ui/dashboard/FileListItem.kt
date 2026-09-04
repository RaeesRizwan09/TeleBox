package com.telebox.app.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.data.ItemType
import com.telebox.app.data.TelegramFile
import com.telebox.app.ui.components.FileIconSize
import com.telebox.app.ui.components.FileTypeIcon
import com.telebox.app.ui.theme.TeleBoxTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    file: TelegramFile,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = TeleBoxTheme.colors
    val isFolder = file.type == ItemType.FOLDER
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, if (selected) colors.primary.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoxCentered {
            if (isFolder) {
                Icon(Icons.Outlined.Folder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            } else {
                FileTypeIcon(filename = file.name, size = FileIconSize.SM)
            }
        }
        Text(
            file.name,
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f).padding(horizontal = 16.dp)
        )
        Row {
            IconButton(onClick = onPreview, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Visibility, contentDescription = "Preview", tint = colors.subtext, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDownload, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Storage, contentDescription = "Download", tint = colors.subtext, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Add, contentDescription = "Delete", tint = colors.danger, modifier = Modifier.size(16.dp).rotate(45f))
            }
        }
        Text(file.sizeStr, color = colors.subtext, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.width(72.dp), maxLines = 1)
        Text(file.createdAt ?: "-", color = colors.subtext.copy(alpha = 0.5f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.End, modifier = Modifier.width(96.dp), maxLines = 1)
    }
}

@Composable
private fun BoxCentered(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
        content()
    }
}
