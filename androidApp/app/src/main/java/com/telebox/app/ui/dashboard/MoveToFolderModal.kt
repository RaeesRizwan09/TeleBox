package com.telebox.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.data.TelegramFolder
import com.telebox.app.ui.theme.TeleBoxTheme

@Composable
fun MoveToFolderModal(
    folders: List<TelegramFolder>,
    activeFolderId: Long?,
    onClose: () -> Unit,
    onSelect: (Long?) -> Unit
) {
    val colors = TeleBoxTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .heightIn(max = 480.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .clickable(enabled = false) {}
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Move to Folder", color = colors.text, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Add, contentDescription = "Close", tint = colors.subtext, modifier = Modifier.rotate(45f))
                }
            }
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                if (activeFolderId != null) {
                    item {
                        FolderChoice("Saved Messages", isRoot = true) { onSelect(null) }
                    }
                }
                items(folders.filter { it.id != activeFolderId }, key = { it.id }) { folder ->
                    FolderChoice(folder.name, isRoot = false) { onSelect(folder.id) }
                }
                if (folders.isEmpty() && activeFolderId == null) {
                    item {
                        Text(
                            "No other folders available. Create one first!",
                            color = colors.subtext,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderChoice(name: String, isRoot: Boolean, onClick: () -> Unit) {
    val colors = TeleBoxTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isRoot) colors.primary.copy(alpha = 0.2f) else colors.hover),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRoot) Icons.Outlined.Cloud else Icons.Outlined.Folder,
                contentDescription = null,
                tint = if (isRoot) colors.primary else colors.text,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(name, color = colors.text, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
    }
}
