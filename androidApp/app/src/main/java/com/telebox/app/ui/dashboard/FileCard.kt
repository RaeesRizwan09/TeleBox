package com.telebox.app.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.telebox.app.data.ItemType
import com.telebox.app.data.TelegramFile
import com.telebox.app.ui.components.FileIconSize
import com.telebox.app.ui.components.FileTypeIcon
import com.telebox.app.ui.components.FolderTypeIcon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileCard(
    file: TelegramFile,
    isSelected: Boolean,
    selecting: Boolean,
    thumbnail: String? = null,
    onRequestThumbnail: () -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onMore: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isFolder = file.type == ItemType.FOLDER

    LaunchedEffect(file.id) { onRequestThumbnail() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) scheme.secondaryContainer else scheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                val bitmap = remember(thumbnail) { base64ToImageBitmap(thumbnail) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (isFolder) {
                FolderTypeIcon(size = FileIconSize.LG)
            } else {
                FileTypeIcon(filename = file.name, size = FileIconSize.LG)
            }
            if (selecting || isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) scheme.primary else scheme.surface.copy(alpha = 0.8f))
                        .combinedClickable(onClick = onToggleSelection, onLongClick = {}),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            if (!selecting) {
                IconButton(
                    onClick = onMore,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "More actions",
                        tint = scheme.onSurfaceVariant
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                file.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (isSelected) scheme.onSecondaryContainer else scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                file.sizeStr,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
