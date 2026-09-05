package com.telebox.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
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
import com.telebox.app.ui.theme.isCompact
import com.telebox.app.ui.theme.rememberWindowWidthSize
import com.telebox.app.ui.theme.useModalDrawer
import com.telebox.app.viewmodel.DashboardUiState
import com.telebox.app.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    viewModel: DashboardViewModel,
    theme: AppThemeMode,
    onToggleTheme: () -> Unit
) {
    val windowSize = rememberWindowWidthSize()
    val compact = windowSize.isCompact
    val useModalDrawer = windowSize.useModalDrawer
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var searchExpanded by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        viewModel.queueUploads(uris) { uri ->
            uri.lastPathSegment ?: uri.toString()
        }
    }

    BackHandler(enabled = useModalDrawer && drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    BackHandler(enabled = state.selectedIds.isNotEmpty()) {
        viewModel.clearSelection()
    }
    BackHandler(enabled = searchExpanded) {
        searchExpanded = false
        viewModel.onSearchChange("")
    }

    val sidebar: @Composable () -> Unit = {
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
            onLogout = viewModel::logout,
            onNavigate = {
                if (useModalDrawer) scope.launch { drawerState.close() }
            }
        )
    }

    val content: @Composable () -> Unit = {
        DashboardScaffold(
            state = state,
            viewModel = viewModel,
            theme = theme,
            compact = compact,
            showMenu = useModalDrawer,
            searchExpanded = searchExpanded,
            onSearchExpandedChange = { searchExpanded = it },
            onToggleTheme = onToggleTheme,
            onMenuClick = { scope.launch { drawerState.open() } },
            onUpload = { picker.launch(arrayOf("*/*")) }
        )
    }

    if (useModalDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.88f)) {
                    sidebar()
                }
            }
        ) {
            content()
        }
    } else {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(modifier = Modifier.width(280.dp)) {
                    sidebar()
                }
            }
        ) {
            content()
        }
    }

    if (state.showDropBlocker) {
        ExternalDropBlocker {
            viewModel.setDropBlocker(false)
            picker.launch(arrayOf("*/*"))
        }
    }
}

@Composable
private fun DashboardScaffold(
    state: DashboardUiState,
    viewModel: DashboardViewModel,
    theme: AppThemeMode,
    compact: Boolean,
    showMenu: Boolean,
    searchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    onToggleTheme: () -> Unit,
    onMenuClick: () -> Unit,
    onUpload: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
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
                        event.key == Key.Delete || event.key == Key.Backspace -> { viewModel.bulkDelete(); true }
                        event.key == Key.Escape -> { viewModel.handleEscape(); true }
                        event.key == Key.Enter -> { viewModel.handleEnter(); true }
                        else -> false
                    }
                }
            },
        topBar = {
            TopBar(
                currentFolderName = viewModel.currentFolderName(),
                selectedCount = state.selectedIds.size,
                viewMode = state.viewMode,
                searchTerm = state.searchTerm,
                theme = theme,
                showMenu = showMenu,
                searchExpanded = searchExpanded,
                onSearchExpandedChange = onSearchExpandedChange,
                onSearchChange = viewModel::onSearchChange,
                onMenuClick = onMenuClick,
                onShowMoveModal = { viewModel.showMoveModal(true) },
                onBulkDownload = {
                    state.displayedFiles.filter { it.id in state.selectedIds }.forEach {
                        viewModel.queueDownload(it.id, it.name, it.folderId)
                    }
                },
                onBulkDelete = viewModel::bulkDelete,
                onDownloadFolder = {
                    state.displayedFiles.forEach { viewModel.queueDownload(it.id, it.name, it.folderId) }
                },
                onToggleViewMode = viewModel::toggleViewMode,
                onToggleTheme = onToggleTheme,
                onClearSelection = viewModel::clearSelection
            )
        },
        contentWindowInsets = WindowInsets.navigationBars,
        floatingActionButton = {
            if (state.selectedIds.isEmpty() && !searchExpanded) {
                if (compact) {
                    FloatingActionButton(onClick = onUpload) {
                        Icon(Icons.Outlined.Upload, contentDescription = "Upload files")
                    }
                } else {
                    ExtendedFloatingActionButton(
                        onClick = onUpload,
                        icon = { Icon(Icons.Outlined.Upload, contentDescription = null) },
                        text = { Text("Upload") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (state.searchTerm.length > 2) {
                    Text(
                        text = "Results for \"${state.searchTerm}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = if (compact) 16.dp else 24.dp, vertical = 8.dp)
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
                    compact = compact,
                    onSort = viewModel::setSort,
                    onOpen = { file ->
                        if (file.type == ItemType.FOLDER) viewModel.setActiveFolder(file.id)
                        else viewModel.openPreview(file)
                    },
                    onToggleSelection = viewModel::toggleSelection,
                    onContextMenu = { viewModel.showContextMenu(0f, 0f, it) }
                )
            }

            Column(
                modifier = Modifier
                    .align(if (compact) Alignment.BottomCenter else Alignment.BottomEnd)
                    .fillMaxWidth(if (compact) 1f else 0.42f)
                    .padding(
                        start = if (compact) 12.dp else 16.dp,
                        end = if (compact) 12.dp else 16.dp,
                        bottom = if (compact) 80.dp else 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.uploadQueue.isNotEmpty()) {
                    UploadQueuePanel(
                        items = state.uploadQueue,
                        compact = compact,
                        onClearFinished = viewModel::clearFinishedUploads,
                        onCancelAll = viewModel::cancelAllUploads,
                        onCancelItem = viewModel::cancelUploadItem,
                        onRetryItem = viewModel::retryUploadItem
                    )
                }
                if (state.downloadQueue.isNotEmpty()) {
                    DownloadQueuePanel(
                        items = state.downloadQueue,
                        compact = compact,
                        onClearFinished = viewModel::clearFinishedDownloads,
                        onCancelAll = viewModel::cancelAllDownloads,
                        onCancelItem = viewModel::cancelDownloadItem,
                        onRetryItem = viewModel::retryDownloadItem
                    )
                }
            }
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
        val url = state.streamInfo?.let { info -> viewModel.streamUrlFor(file, info) }
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

    state.contextMenu?.let { menu ->
        FileContextMenu(
            file = menu.file,
            onPreview = {
                if (menu.file.type == ItemType.FOLDER) viewModel.setActiveFolder(menu.file.id)
                else viewModel.openPreview(menu.file)
                viewModel.hideContextMenu()
            },
            onDownload = {
                viewModel.queueDownload(menu.file.id, menu.file.name, menu.file.folderId)
                viewModel.hideContextMenu()
            },
            onDelete = {
                viewModel.deleteFile(menu.file.id)
                viewModel.hideContextMenu()
            },
            onSelect = {
                viewModel.toggleSelection(menu.file.id)
                viewModel.hideContextMenu()
            },
            onDismiss = viewModel::hideContextMenu
        )
    }
}
