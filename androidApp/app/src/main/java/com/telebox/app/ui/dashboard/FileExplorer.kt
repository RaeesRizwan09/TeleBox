package com.telebox.app.ui.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.telebox.app.data.LibrarySection
import com.telebox.app.data.SortDirection
import com.telebox.app.data.SortField
import com.telebox.app.data.TelegramFile
import com.telebox.app.data.ViewMode

@Composable
fun FileExplorer(
    files: List<TelegramFile>,
    loading: Boolean,
    error: String?,
    viewMode: ViewMode,
    selectedIds: Set<Long>,
    sortField: SortField,
    sortDirection: SortDirection,
    compact: Boolean,
    librarySection: LibrarySection = LibrarySection.ALL,
    onSort: (SortField) -> Unit,
    onOpen: (TelegramFile) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onContextMenu: (TelegramFile) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val selecting = selectedIds.isNotEmpty()
    val horizontal = if (compact) 12.dp else 20.dp
    val gridMin = if (compact) 148.dp else 176.dp

    when {
        loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = scheme.primary, modifier = Modifier.size(40.dp))
                    Text(
                        "Loading your files",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
        error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = scheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        "Could not load files",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.error,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp)
                    )
                }
            }
        }
        files.isEmpty() -> {
            val (title, subtitle) = when (librarySection) {
                LibrarySection.VIDEOS -> "No videos here" to "Video files in this folder will appear in this library."
                LibrarySection.PICTURES -> "No pictures here" to "Images in this folder will appear in this library."
                LibrarySection.DOCUMENTS -> "No documents here" to "PDFs, office files, and text documents will appear here."
                LibrarySection.OTHERS -> "Nothing in Others" to "Archives, audio, code, and unclassified files will appear here."
                LibrarySection.ALL -> "This folder is empty" to "Tap the upload button to add files from your device."
            }
            Box(modifier = Modifier.fillMaxSize()) {
                EmptyState(title = title, subtitle = subtitle)
            }
        }
        else -> {
            Column(modifier = Modifier.fillMaxSize()) {
                SortRow(
                    sortField = sortField,
                    sortDirection = sortDirection,
                    onSort = onSort,
                    modifier = Modifier.padding(horizontal = horizontal, vertical = 8.dp)
                )
                if (viewMode == ViewMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(gridMin),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            start = horizontal,
                            end = horizontal,
                            top = 4.dp,
                            bottom = 96.dp
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(files, key = { it.id }) { file ->
                            FileCard(
                                file = file,
                                isSelected = file.id in selectedIds,
                                selecting = selecting,
                                onClick = {
                                    if (selecting) onToggleSelection(file.id) else onOpen(file)
                                },
                                onLongClick = { onToggleSelection(file.id) },
                                onToggleSelection = { onToggleSelection(file.id) },
                                onMore = { onContextMenu(file) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = if (compact) 4.dp else 8.dp,
                            end = if (compact) 4.dp else 8.dp,
                            top = 4.dp,
                            bottom = 96.dp
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(files, key = { it.id }) { file ->
                            FileListItem(
                                file = file,
                                selected = file.id in selectedIds,
                                compact = compact,
                                selecting = selecting,
                                onClick = {
                                    if (selecting) onToggleSelection(file.id) else onOpen(file)
                                },
                                onLongClick = { onToggleSelection(file.id) },
                                onMore = { onContextMenu(file) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortRow(
    sortField: SortField,
    sortDirection: SortDirection,
    onSort: (SortField) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SortChip("Name", SortField.NAME, sortField, sortDirection, onSort)
        SortChip("Size", SortField.SIZE, sortField, sortDirection, onSort)
        SortChip("Date", SortField.DATE, sortField, sortDirection, onSort)
    }
}

@Composable
private fun SortChip(
    label: String,
    field: SortField,
    current: SortField,
    direction: SortDirection,
    onSort: (SortField) -> Unit
) {
    val active = current == field
    FilterChip(
        selected = active,
        onClick = { onSort(field) },
        label = { Text(label) },
        trailingIcon = if (active) {
            {
                Icon(
                    imageVector = if (direction == SortDirection.ASC) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                    contentDescription = if (direction == SortDirection.ASC) "Ascending" else "Descending",
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null
    )
}
