package com.telebox.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.data.BandwidthStats
import com.telebox.app.data.TelegramFolder
import com.telebox.app.ui.theme.TeleBoxTheme

@Composable
fun Sidebar(
    folders: List<TelegramFolder>,
    activeFolderId: Long?,
    isSyncing: Boolean,
    isConnected: Boolean,
    bandwidth: BandwidthStats?,
    showNewFolderInput: Boolean,
    newFolderName: String,
    onActiveFolderChange: (Long?) -> Unit,
    onDeleteFolder: (Long, String) -> Unit,
    onShowNewFolder: () -> Unit,
    onNewFolderNameChange: (String) -> Unit,
    onCreateFolder: () -> Unit,
    onSync: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = TeleBoxTheme.colors
    Column(
        modifier = Modifier
            .width(256.dp)
            .fillMaxHeight()
            .background(colors.surface)
            .border(width = 1.dp, color = colors.border, shape = RoundedCornerShape(0.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Cloud, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
            }
            Text(
                "Telegram Drive",
                color = colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                SidebarItem(
                    icon = Icons.Outlined.Cloud,
                    label = "Saved Messages",
                    active = activeFolderId == null,
                    onClick = { onActiveFolderChange(null) }
                )
            }
            items(folders, key = { it.id }) { folder ->
                SidebarItem(
                    icon = Icons.Outlined.Folder,
                    label = folder.name,
                    active = activeFolderId == folder.id,
                    onClick = { onActiveFolderChange(folder.id) },
                    onDelete = { onDeleteFolder(folder.id, folder.name) }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .border(width = 0.dp, color = colors.border, shape = RoundedCornerShape(0.dp))
        ) {
            if (showNewFolderInput) {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = onNewFolderNameChange,
                    placeholder = { Text("Folder Name", fontSize = 14.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onCreateFolder() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            } else {
                TextButton(
                    onClick = onShowNewFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.subtext, modifier = Modifier.size(16.dp))
                    Text("Create Folder", color = colors.subtext, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) colors.success else colors.danger)
                )
                Text(
                    text = if (isConnected) "Connected to Telegram" else "Disconnected from Telegram",
                    color = colors.subtext,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onSync,
                    enabled = !isSyncing,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.secondary.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Outlined.Sync, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(12.dp))
                    Text(
                        if (isSyncing) "Syncing..." else "Sync",
                        color = colors.secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                TextButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.danger.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Outlined.Logout, contentDescription = null, tint = colors.danger, modifier = Modifier.size(12.dp))
                    Text("Logout", color = colors.danger, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 6.dp))
                }
            }
            BandwidthWidget(bandwidth)
        }
    }
}
