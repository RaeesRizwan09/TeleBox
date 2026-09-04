package com.telebox.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.data.ItemType
import com.telebox.app.data.TelegramFile
import com.telebox.app.ui.theme.TeleBoxTheme
import com.telebox.app.util.isMediaFile
import com.telebox.app.util.isPdfFile

@Composable
fun FileContextMenu(
    file: TelegramFile,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = TeleBoxTheme.colors
    Column(
        modifier = Modifier
            .widthIn(min = 200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface.copy(alpha = 0.95f))
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        Text(
            file.name,
            color = colors.subtext,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
        HorizontalDivider(color = colors.border)
        if (file.type != ItemType.FOLDER) {
            val (icon, label, tint) = when {
                isMediaFile(file.name) -> Triple(Icons.Outlined.PlayArrow, "Play", colors.primary)
                isPdfFile(file.name) -> Triple(Icons.Outlined.PictureAsPdf, "View PDF", colors.danger)
                else -> Triple(Icons.Outlined.Visibility, "Preview", colors.secondary)
            }
            MenuRow(icon, label, tint, onPreview)
        } else {
            MenuRow(Icons.Outlined.FolderOpen, "Open", androidx.compose.ui.graphics.Color(0xFFEAB308), onPreview)
        }
        MenuRow(Icons.Outlined.Storage, "Download", androidx.compose.ui.graphics.Color(0xFF22C55E), onDownload)
        MenuRow(Icons.Outlined.Edit, "Rename", colors.subtext, onClick = {}, enabled = false)
        HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 4.dp))
        MenuRow(Icons.Outlined.Delete, "Delete", colors.danger, onDelete)
    }
}

@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val colors = TeleBoxTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) tint else colors.subtext, modifier = Modifier.size(16.dp))
        Text(
            label,
            color = if (enabled) colors.text else colors.subtext,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
