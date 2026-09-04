package com.telebox.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebox.app.data.AppThemeMode
import com.telebox.app.data.ItemType
import com.telebox.app.ui.dashboard.DownloadQueuePanel
import com.telebox.app.ui.dashboard.DragDropOverlay
import com.telebox.app.ui.dashboard.ExternalDropBlocker
import com.telebox.app.ui.dashboard.FileContextMenu
import com.telebox.app.ui.dashboard.FileExplorer
import com.telebox.app.ui.dashboard.MediaPlayerDialog
import com.telebox.app.ui.dashboard.MoveToFolderModal
import com.telebox.app.ui.dashboard.PdfViewerDialog
import com.telebox.app.ui.dashboard.PreviewModal
import com.telebox.app.ui.dashboard.Sidebar
import com.telebox.app.ui.dashboard.TopBar
import com.telebox.app.ui.dashboard.UploadQueuePanel
import com.telebox.app.ui.theme.TeleBoxTheme
import com.telebox.app.viewmodel.DashboardUiState
import com.telebox.app.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    viewModel: DashboardViewModel,
    theme: AppThemeMode,
    onToggleTheme: () -> Unit
) {
    val colors = TeleBoxTheme.colors
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        viewModel.queueUploads(uris) { uri ->
            uri.lastPathSegment ?: uri.toString()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .clickable { viewModel.clearSelection() }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val modalOpen = state.previewFile != null || state.playingFile != null || state.pdfFile != null || state.showMoveModal
                if (modalOpen) {
                    when (event.key) {
                        Key.Escape -> { viewModel.closePreview(); true }
                        Key.DirectionRight, Key.L -> { viewModel.navigatePreview(1); true }
                        Key.DirectionLeft, Key.J -> { viewModel.navigatePreview(-1); true }
                        else -> false
                    }
                } else {
                    val mod = event.isCtrlPressed || event.isMetaPressed
                    when {
                        mod && event.key == Key.A -> { viewModel.selectAll(); true }
                        mod && event.key == Key.F -> true
                        event.key == Key.Delete || event.key == Key.Backspace -> { viewModel.bulkDelete(); true }
                        event.key == Key.Escape -> { viewModel.handleEscape(); true }
                        event.key == Key.Enter -> { viewModel.handleEnter(); true }
                        else -> false
                    }
                }
            }
    ) {
        if (state.showDropBlocker) {
            ExternalDropBlocker {
                viewModel.setDropBlocker(false)
                picker.launch(arrayOf("*/*"))
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Sidebar(
                folders = state.folders,
                activeFolderId = state.activeFolderId,
                isSyncing = state.isSyncing,
                isConnected = state.isConnected,
                bandwidth = state.bandwidth,
                showNewFolderInput = state.showNewFolderInput,
                newFolderName = state.newFolderName,
                onActiveFolderChange = viewModel::setActiveFolder,
                onDeleteFolder = viewModel::deleteFolder,
                onShowNewFolder = { viewModel.setShowNewFolderInput(true) },
                onNewFolderNameChange = viewModel::onNewFolderNameChange,
                onCreateFolder = viewModel::createFolder,
                onSync = viewModel::syncFolders,
                onLogout = viewModel::logout
            )
            Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                TopBar(
                    currentFolderName = viewModel.currentFolderName(),
                    selectedCount = state.selectedIds.size,
                    viewMode = state.viewMode,
                    searchTerm = state.searchTerm,
                    theme = theme,
                    onSearchChange = viewModel::onSearchChange,
                    onShowMoveModal = { viewModel.showMoveModal(true) },
                    onBulkDownload = {
                        state.displayedFiles.filter { it.id in state.selectedIds }.forEach {
                            viewModel.queueDownload(it.id, it.name)
                        }
                    },
                    onBulkDelete = viewModel::bulkDelete,
                    onDownloadFolder = {
                        state.displayedFiles.forEach { viewModel.queueDownload(it.id, it.name) }
                    },
                    onToggleViewMode = viewModel::toggleViewMode,
                    onToggleTheme = onToggleTheme
                )
                if (state.searchTerm.length > 2) {
                    Text(
                        text = "Search Results for \"${state.searchTerm}\"",
                        color = colors.subtext,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                }
                FileExplorer(
                    files = viewModel.sortedFiles(),
                    loading = state.filesLoading || state.isSearching,
                    error = state.filesError,
                    viewMode = state.viewMode,
                    selectedIds = state.selectedIds,
                    sortField = state.sortField,
                    sortDirection = state.sortDirection,
                    onSort = viewModel::setSort,
                    onFileClick = { id, additive -> viewModel.onFileClick(id, additive) },
                    onToggleSelection = viewModel::toggleSelection,
                    onDelete = viewModel::deleteFile,
                    onDownload = { id, name -> viewModel.queueDownload(id, name) },
                    onPreview = { file ->
                        if (file.type == ItemType.FOLDER) viewModel.setActiveFolder(file.id)
                        else viewModel.openPreview(file)
                    },
                    onManualUpload = { picker.launch(arrayOf("*/*")) },
                    onContextMenu = { viewModel.showContextMenu(0f, 0f, it) },
                    onSelectionClear = viewModel::clearSelection
                )
            }
        }

        if (state.showMoveModal) {
            MoveToFolderModal(
                folders = state.folders,
                activeFolderId = state.activeFolderId,
                onClose = { viewModel.showMoveModal(false) },
                onSelect = viewModel::bulkMove
            )
        }

        state.playingFile?.let { file ->
            val url = state.streamInfo?.let { info ->
                val folderParam = state.activeFolderId?.toString() ?: "home"
                "${info.baseUrl}/stream/$folderParam/${file.id}?token=${info.token}"
            }
            MediaPlayerDialog(
                file = file,
                streamInfo = state.streamInfo,
                currentIndex = state.previewContextIndex,
                totalItems = state.previewContextFiles.size,
                streamUrl = url,
                onClose = viewModel::closePreview,
                onNext = { viewModel.navigatePreview(1) },
                onPrev = { viewModel.navigatePreview(-1) }
            )
        }

        state.pdfFile?.let { file ->
            PdfViewerDialog(
                file = file,
                loading = state.pdfLoading,
                error = state.pdfError,
                pageCount = state.pdfPageCount,
                scale = state.pdfScale,
                currentIndex = state.previewContextIndex,
                totalItems = state.previewContextFiles.size,
                onClose = viewModel::closePreview,
                onNext = { viewModel.navigatePreview(1) },
                onPrev = { viewModel.navigatePreview(-1) },
                onZoomIn = { viewModel.zoomPdf(0.2f) },
                onZoomOut = { viewModel.zoomPdf(-0.2f) },
                onFitWidth = { viewModel.setPdfScale(1.2f) }
            )
        }

        state.previewFile?.let { file ->
            PreviewModal(
                file = file,
                src = state.previewSrc,
                loading = state.previewLoading,
                error = state.previewError,
                currentIndex = state.previewContextIndex,
                totalItems = state.previewContextFiles.size,
                onClose = viewModel::closePreview,
                onNext = { viewModel.navigatePreview(1) },
                onPrev = { viewModel.navigatePreview(-1) }
            )
        }

        if (state.isExternalDragging) {
            DragDropOverlay()
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            if (state.uploadQueue.isNotEmpty()) {
                UploadQueuePanel(
                    items = state.uploadQueue,
                    onClearFinished = viewModel::clearFinishedUploads,
                    onCancelAll = viewModel::cancelAllUploads,
                    onCancelItem = viewModel::cancelUploadItem,
                    onRetryItem = viewModel::retryUploadItem
                )
            }
            if (state.downloadQueue.isNotEmpty()) {
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    DownloadQueuePanel(
                        items = state.downloadQueue,
                        onClearFinished = viewModel::clearFinishedDownloads,
                        onCancelAll = viewModel::cancelAllDownloads,
                        onCancelItem = viewModel::cancelDownloadItem,
                        onRetryItem = viewModel::retryDownloadItem
                    )
                }
            }
        }

        state.contextMenu?.let { menu ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable { viewModel.hideContextMenu() }
            ) {
                FileContextMenu(
                    file = menu.file,
                    onPreview = {
                        if (menu.file.type == ItemType.FOLDER) viewModel.setActiveFolder(menu.file.id)
                        else viewModel.openPreview(menu.file)
                        viewModel.hideContextMenu()
                    },
                    onDownload = {
                        viewModel.queueDownload(menu.file.id, menu.file.name)
                        viewModel.hideContextMenu()
                    },
                    onDelete = {
                        viewModel.deleteFile(menu.file.id)
                        viewModel.hideContextMenu()
                    },
                    onDismiss = viewModel::hideContextMenu
                )
            }
        }
    }
}
