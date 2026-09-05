package com.telebox.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.telebox.app.data.TelegramFolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToFolderModal(
    folders: List<TelegramFolder>,
    activeFolderId: Long?,
    onClose: () -> Unit,
    onSelect: (Long?) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val destinations = folders.filter { it.id != activeFolderId }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                "Move to folder",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            LazyColumn {
                if (activeFolderId != null) {
                    item {
                        FolderChoice("Saved Messages", isRoot = true) { onSelect(null) }
                    }
                }
                items(destinations, key = { it.id }) { folder ->
                    FolderChoice(folder.name, isRoot = false) { onSelect(folder.id) }
                }
                if (destinations.isEmpty() && activeFolderId == null) {
                    item {
                        Text(
                            "No other folders available. Create one first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderChoice(name: String, isRoot: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isRoot) Icons.Outlined.Cloud else Icons.Outlined.Folder,
            contentDescription = null,
            tint = if (isRoot) scheme.primary else scheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurface,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
