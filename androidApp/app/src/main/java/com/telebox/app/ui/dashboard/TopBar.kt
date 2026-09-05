package com.telebox.app.ui.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.telebox.app.data.AppThemeMode
import com.telebox.app.data.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    currentFolderName: String,
    selectedCount: Int,
    viewMode: ViewMode,
    searchTerm: String,
    theme: AppThemeMode,
    showMenu: Boolean,
    searchExpanded: Boolean,
    transferCount: Int,
    downloadedCount: Int,
    onSearchExpandedChange: (Boolean) -> Unit,
    onSearchChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    onShowMoveModal: () -> Unit,
    onBulkDownload: () -> Unit,
    onBulkDelete: () -> Unit,
    onDownloadFolder: () -> Unit,
    onToggleViewMode: () -> Unit,
    onToggleTheme: () -> Unit,
    onClearSelection: () -> Unit,
    onOpenTransfers: () -> Unit,
    onOpenDownloads: () -> Unit,
    onClearCache: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var overflowOpen by remember { mutableStateOf(false) }

    if (searchExpanded) {
        TopAppBar(
            title = {
                TextField(
                    value = searchTerm,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search files") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = scheme.surface,
                        unfocusedContainerColor = scheme.surface,
                        focusedIndicatorColor = scheme.primary,
                        unfocusedIndicatorColor = scheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    onSearchChange("")
                    onSearchExpandedChange(false)
                }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close search")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = scheme.surfaceContainer,
                titleContentColor = scheme.onSurface,
                navigationIconContentColor = scheme.onSurface
            )
        )
        return
    }

    if (selectedCount > 0) {
        TopAppBar(
            title = {
                Text(
                    "$selectedCount selected",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear selection")
                }
            },
            actions = {
                IconButton(onClick = onShowMoveModal) {
                    Icon(Icons.Outlined.DriveFileMove, contentDescription = "Move")
                }
                IconButton(onClick = onBulkDownload) {
                    Icon(Icons.Outlined.Download, contentDescription = "Download selected")
                }
                IconButton(onClick = onBulkDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete selected")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = scheme.secondaryContainer,
                titleContentColor = scheme.onSecondaryContainer,
                navigationIconContentColor = scheme.onSecondaryContainer,
                actionIconContentColor = scheme.onSecondaryContainer
            )
        )
        return
    }

    TopAppBar(
        title = {
            Text(
                currentFolderName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1
            )
        },
        navigationIcon = {
            if (showMenu) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Outlined.Menu, contentDescription = "Open navigation")
                }
            }
        },
        actions = {
            IconButton(onClick = { onSearchExpandedChange(true) }) {
                Icon(Icons.Outlined.Search, contentDescription = "Search")
            }
            IconButton(onClick = onToggleViewMode) {
                Icon(
                    imageVector = if (viewMode == ViewMode.GRID) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                    contentDescription = "Toggle layout"
                )
            }
            IconButton(onClick = onOpenTransfers) {
                BadgedBox(
                    badge = {
                        if (transferCount > 0) {
                            Badge { Text(if (transferCount > 9) "9+" else "$transferCount") }
                        }
                    }
                ) {
                    Icon(Icons.Outlined.SwapVert, contentDescription = "Transfer queue")
                }
            }
            IconButton(onClick = { overflowOpen = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More")
            }
            DropdownMenu(
                expanded = overflowOpen,
                onDismissRequest = { overflowOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Download all") },
                    onClick = {
                        overflowOpen = false
                        onDownloadFolder()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Downloaded files${if (downloadedCount > 0) " ($downloadedCount)" else ""}") },
                    onClick = {
                        overflowOpen = false
                        onOpenDownloads()
                    },
                    leadingIcon = { Icon(Icons.Outlined.DownloadDone, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Clear cache") },
                    onClick = {
                        overflowOpen = false
                        onClearCache()
                    },
                    leadingIcon = { Icon(Icons.Outlined.CleaningServices, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = {
                        Text(if (theme == AppThemeMode.DARK) "Light theme" else "Dark theme")
                    },
                    onClick = {
                        overflowOpen = false
                        onToggleTheme()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (theme == AppThemeMode.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = null
                        )
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = scheme.surfaceContainer,
            titleContentColor = scheme.onSurface,
            navigationIconContentColor = scheme.onSurface,
            actionIconContentColor = scheme.onSurfaceVariant
        )
    )
}
