package com.telebox.app.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.telebox.app.data.ItemType
import com.telebox.app.data.TelegramFile
import com.telebox.app.ui.components.FileIconSize
import com.telebox.app.ui.components.FileTypeIcon
import com.telebox.app.ui.theme.TeleBoxTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileCard(
    file: TelegramFile,
    isSelected: Boolean,
    thumbnail: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit,
    onPreview: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val colors = TeleBoxTheme.colors
    val isFolder = file.type == ItemType.FOLDER
    val borderColor = if (isSelected) colors.primary else colors.border
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) colors.primary.copy(alpha = 0.05f) else colors.surface)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        if (thumbnail != null) {
            AsyncImage(
                model = thumbnail,
                contentDescription = file.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isFolder) {
                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(48.dp))
                } else {
                    FileTypeIcon(filename = file.name, size = FileIconSize.LG)
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(8.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isSelected) colors.primary else Color.Black.copy(alpha = 0.3f))
                .border(1.dp, if (isSelected) colors.primary else Color.White.copy(alpha = 0.5f), CircleShape)
                .combinedClickable(onClick = onToggleSelection, onLongClick = {}),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Black))
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(file.name, color = if (thumbnail != null) Color.White else colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(file.sizeStr, color = if (thumbnail != null) Color.White.copy(alpha = 0.7f) else colors.subtext, fontSize = 12.sp)
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            MiniAction(Icons.Outlined.Visibility, onPreview)
            MiniAction(Icons.Outlined.Download, onDownload)
            MiniAction(Icons.Outlined.Delete, onDelete)
        }
    }
}

@Composable
private fun MiniAction(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(start = 4.dp)
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
    }
}
