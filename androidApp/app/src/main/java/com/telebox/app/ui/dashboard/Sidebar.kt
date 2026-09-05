package com.telebox.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
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
    onLogout: () -> Unit,
    onNavigate: () -> Unit = {}
) {
    val colors = TeleBoxTheme.colors
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(scheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Cloud,
                    contentDescription = null,
                    tint = scheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    "TeleBox",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onSurface
                )
                Text(
                    "Telegram Drive",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
                    label = { Text("Saved Messages") },
                    selected = activeFolderId == null,
                    onClick = {
                        onActiveFolderChange(null)
                        onNavigate()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = scheme.secondaryContainer,
                        selectedIconColor = scheme.onSecondaryContainer,
                        selectedTextColor = scheme.onSecondaryContainer
                    )
                )
            }
            if (folders.isNotEmpty()) {
                item {
                    Text(
                        "Folders",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
            }
            items(folders, key = { it.id }) { folder ->
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                    label = { Text(folder.name, maxLines = 1) },
                    selected = activeFolderId == folder.id,
                    onClick = {
                        onActiveFolderChange(folder.id)
                        onNavigate()
                    },
                    badge = {
                        IconButton(
                            onClick = { onDeleteFolder(folder.id, folder.name) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Delete ${folder.name}",
                                tint = scheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = scheme.secondaryContainer,
                        selectedIconColor = scheme.onSecondaryContainer,
                        selectedTextColor = scheme.onSecondaryContainer
                    )
                )
            }
        }

        HorizontalDivider(color = scheme.outlineVariant)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (showNewFolderInput) {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = onNewFolderNameChange,
                    placeholder = { Text("Folder name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onCreateFolder() }),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                FilledTonalButton(
                    onClick = onShowNewFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Create folder", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) colors.success else colors.danger)
                )
                Text(
                    text = if (isConnected) "Connected" else "Disconnected",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onSync,
                    enabled = !isSyncing,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        if (isSyncing) "Syncing" else "Sync",
                        modifier = Modifier.padding(start = 6.dp),
                        maxLines = 1
                    )
                }
                TextButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        Icons.Outlined.Logout,
                        contentDescription = null,
                        tint = scheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Logout",
                        color = scheme.error,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            BandwidthWidget(bandwidth)
        }
    }
}
