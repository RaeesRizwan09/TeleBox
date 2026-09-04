package com.telebox.app.ui.dashboard

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.data.SortDirection
import com.telebox.app.data.SortField
import com.telebox.app.data.TelegramFile
import com.telebox.app.data.ViewMode
import com.telebox.app.ui.theme.TeleBoxTheme

@Composable
fun FileExplorer(
    files: List<TelegramFile>,
    loading: Boolean,
    error: String?,
    viewMode: ViewMode,
    selectedIds: Set<Long>,
    sortField: SortField,
    sortDirection: SortDirection,
    onSort: (SortField) -> Unit,
    onFileClick: (Long, Boolean) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onDownload: (Long, String, Long?) -> Unit,
    onPreview: (TelegramFile) -> Unit,
    onManualUpload: () -> Unit,
    onContextMenu: (TelegramFile) -> Unit,
    onSelectionClear: () -> Unit
) {
    val colors = TeleBoxTheme.colors
    when {
        loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = colors.primary, strokeWidth = 4.dp, modifier = Modifier.size(32.dp))
                    Text("Loading your files...", color = colors.subtext, modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
        error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error loading files", color = colors.danger)
            }
        }
        files.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                EmptyState(onUpload = onManualUpload)
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .clickable(onClick = onSelectionClear)
            ) {
                if (viewMode == ViewMode.GRID) {
                    Row(
                        modifier = Modifier.padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Sort by:", color = colors.subtext, fontSize = 12.sp)
                        SortChip("Name", SortField.NAME, sortField, sortDirection, onSort)
                        SortChip("Size", SortField.SIZE, sortField, sortDirection, onSort)
                        SortChip("Date", SortField.DATE, sortField, sortDirection, onSort)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(files, key = { it.id }) { file ->
                            Box(modifier = Modifier.aspectRatio(4f / 3f)) {
                                FileCard(
                                    file = file,
                                    isSelected = file.id in selectedIds,
                                    onClick = { onFileClick(file.id, false) },
                                    onLongClick = { onContextMenu(file) },
                                    onDelete = { onDelete(file.id) },
                                    onDownload = { onDownload(file.id, file.name, file.folderId) },
                                    onPreview = { onPreview(file) },
                                    onToggleSelection = { onToggleSelection(file.id) }
                                )
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(4f / 3f)
                                    .border(2.dp, colors.border, RoundedCornerShape(12.dp))
                                    .clickable(onClick = onManualUpload),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.subtext, modifier = Modifier.size(32.dp))
                                    Text("Upload File", color = colors.subtext, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", color = colors.subtext, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.size(32.dp))
                        SortChip("Name", SortField.NAME, sortField, sortDirection, onSort, modifier = Modifier.weight(2f))
                        SortChip("Size", SortField.SIZE, sortField, sortDirection, onSort)
                        SortChip("Date", SortField.DATE, sortField, sortDirection, onSort)
                    }
                    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                        items(files, key = { it.id }) { file ->
                            FileListItem(
                                file = file,
                                selected = file.id in selectedIds,
                                onClick = { onFileClick(file.id, false) },
                                onLongClick = { onContextMenu(file) },
                                onPreview = { onPreview(file) },
                                onDownload = { onDownload(file.id, file.name, file.folderId) },
                                onDelete = { onDelete(file.id) }
                            )
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                    .clickable(onClick = onManualUpload)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.subtext, modifier = Modifier.size(16.dp))
                                Text("Upload File...", color = colors.subtext, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    field: SortField,
    current: SortField,
    direction: SortDirection,
    onSort: (SortField) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TeleBoxTheme.colors
    val active = current == field
    Row(
        modifier = modifier
            .clickable { onSort(field) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (active) colors.primary else colors.subtext, fontSize = 12.sp)
        Icon(
            imageVector = when {
                !active -> Icons.Outlined.SwapVert
                direction == SortDirection.ASC -> Icons.Outlined.ArrowUpward
                else -> Icons.Outlined.ArrowDownward
            },
            contentDescription = null,
            tint = if (active) colors.primary else colors.subtext.copy(alpha = 0.3f),
            modifier = Modifier.size(12.dp).padding(start = 2.dp)
        )
    }
}
