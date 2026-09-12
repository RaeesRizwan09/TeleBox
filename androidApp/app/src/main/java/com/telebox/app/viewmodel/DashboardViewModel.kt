package com.telebox.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telebox.app.data.BandwidthStats
import com.telebox.app.data.ConfirmOptions
import com.telebox.app.data.ConfirmVariant
import com.telebox.app.data.DownloadItem
import com.telebox.app.data.ItemType
import com.telebox.app.data.LibrarySection
import com.telebox.app.data.LibraryStats
import com.telebox.app.data.LocalDownload
import com.telebox.app.data.PreferencesStore
import com.telebox.app.data.QueueItem
import com.telebox.app.data.SortDirection
import com.telebox.app.data.SortField
import com.telebox.app.data.StreamInfo
import com.telebox.app.data.TelegramFile
import com.telebox.app.data.TelegramFolder
import com.telebox.app.data.TelegramRepository
import com.telebox.app.data.TransferStatus
import com.telebox.app.data.ViewMode
import com.telebox.app.util.computeLibraryStats
import com.telebox.app.util.formatBytes
import com.telebox.app.util.isImageFile
import com.telebox.app.util.isMediaFile
import com.telebox.app.util.isPdfFile
import com.telebox.app.util.librarySectionFor
import com.telebox.app.util.randomId
import com.telebox.app.util.withFormattedSize
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val folders: List<TelegramFolder> = emptyList(),
    val activeFolderId: Long? = null,
    val files: List<TelegramFile> = emptyList(),
    val displayedFiles: List<TelegramFile> = emptyList(),
    val filesLoading: Boolean = false,
    val filesError: String? = null,
    val isSyncing: Boolean = false,
    val isConnected: Boolean = true,
    val viewMode: ViewMode = ViewMode.LIST,
    val selectedIds: Set<Long> = emptySet(),
    val searchTerm: String = "",
    val searchResults: List<TelegramFile> = emptyList(),
    val isSearching: Boolean = false,
    val showMoveModal: Boolean = false,
    val previewFile: TelegramFile? = null,
    val playingFile: TelegramFile? = null,
    val pdfFile: TelegramFile? = null,
    val previewContextFiles: List<TelegramFile> = emptyList(),
    val previewContextIndex: Int = -1,
    val uploadQueue: List<QueueItem> = emptyList(),
    val downloadQueue: List<DownloadItem> = emptyList(),
    val bandwidth: BandwidthStats? = null,
    val showNewFolderInput: Boolean = false,
    val newFolderName: String = "",
    val sortField: SortField = SortField.NAME,
    val sortDirection: SortDirection = SortDirection.ASC,
    val contextMenu: ContextMenuState? = null,
    val streamInfo: StreamInfo? = null,
    val mediaStreamUrl: String? = null,
    val mediaLoading: Boolean = false,
    val mediaError: String? = null,
    val previewSrc: String? = null,
    val previewLoading: Boolean = false,
    val previewError: String? = null,
    val pdfLoading: Boolean = false,
    val pdfError: String? = null,
    val pdfPageCount: Int = 0,
    val pdfScale: Float = 1.2f,
    val showDropBlocker: Boolean = false,
    val isExternalDragging: Boolean = false,
    val librarySection: LibrarySection = LibrarySection.ALL,
    val libraryStats: LibraryStats = LibraryStats(),
    val showTransfers: Boolean = false,
    val showDownloadsWindow: Boolean = false,
    val localDownloads: List<LocalDownload> = emptyList(),
    val isClearingCache: Boolean = false,
    val thumbnails: Map<Long, String> = emptyMap()
)

data class ContextMenuState(
    val x: Float,
    val y: Float,
    val file: TelegramFile
)

class DashboardViewModel(
    application: Application,
    private val repository: TelegramRepository,
    private val store: PreferencesStore,
    private val confirm: ConfirmViewModel,
    private val app: AppViewModel
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var uploadProcessing = false
    private var downloadProcessing = false
    private val cancelledUploads = mutableSetOf<String>()
    private val cancelledDownloads = mutableSetOf<String>()
    private var initializedQueues = false

    init {
        viewModelScope.launch { bootstrap() }
        viewModelScope.launch { pollBandwidth() }
        viewModelScope.launch { pollNetwork() }
        viewModelScope.launch {
            repository.uploadProgress.collect { payload ->
                _state.update { current ->
                    current.copy(
                        uploadQueue = current.uploadQueue.map { item ->
                            if (item.id == payload.id) item.copy(
                                progress = payload.percent,
                                uploadedBytes = payload.uploadedBytes,
                                totalBytes = payload.totalBytes,
                                speedBytesPerSec = payload.speedBytesPerSec
                            ) else item
                        }
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.downloadProgress.collect { payload ->
                _state.update { current ->
                    current.copy(
                        downloadQueue = current.downloadQueue.map { item ->
                            if (item.id == payload.id) item.copy(
                                progress = payload.percent,
                                uploadedBytes = payload.uploadedBytes,
                                totalBytes = payload.totalBytes,
                                speedBytesPerSec = payload.speedBytesPerSec
                            ) else item
                        }
                    )
                }
            }
        }
    }

    private suspend fun bootstrap() {
        val folders = store.getFolders()
        val active = store.getActiveFolderId()
        val viewMode = store.getViewMode()
        _state.update {
            it.copy(
                folders = folders,
                activeFolderId = active,
                viewMode = viewMode,
                isConnected = true
            )
        }
        val pendingUploads = store.getPendingUploads()
        val pendingDownloads = store.getPendingDownloads()
        if (pendingUploads.isNotEmpty()) {
            _state.update { it.copy(uploadQueue = pendingUploads) }
            app.showToast("Restored ${pendingUploads.size} pending uploads")
        }
        if (pendingDownloads.isNotEmpty()) {
            _state.update { it.copy(downloadQueue = pendingDownloads) }
            app.showToast("Restored ${pendingDownloads.size} pending downloads")
        }
        initializedQueues = true
        refreshLocalDownloads()
        refreshFiles()
        processNextUpload()
        processNextDownload()
    }

    private suspend fun pollBandwidth() {
        while (true) {
            runCatching { repository.getBandwidth() }.onSuccess { stats ->
                _state.update { it.copy(bandwidth = stats) }
            }
            delay(5_000)
        }
    }

    private suspend fun pollNetwork() {
        while (true) {
            val online = runCatching { repository.isNetworkAvailable() }.getOrDefault(true)
            _state.update { it.copy(isConnected = online) }
            delay(10_000)
        }
    }

    fun refreshFiles() {
        viewModelScope.launch {
            val folderId = _state.value.activeFolderId
            _state.update { it.copy(filesLoading = true, filesError = null) }
            try {
                val files = repository.getFiles(folderId).map { it.withFormattedSize() }
                _state.update {
                    it.copy(
                        files = files,
                        filesLoading = false,
                        libraryStats = computeLibraryStats(files),
                        displayedFiles = computeDisplayed(files, it.searchTerm, it.searchResults, it.librarySection)
                    )
                }
            } catch (err: Throwable) {
                _state.update { it.copy(filesLoading = false, filesError = err.message ?: "Error loading files") }
            }
        }
    }

    private fun computeDisplayed(
        files: List<TelegramFile>,
        searchTerm: String,
        searchResults: List<TelegramFile>,
        section: LibrarySection
    ): List<TelegramFile> {
        val source = if (searchTerm.length > 2) searchResults else files
        val filtered = source.filter { file ->
            searchTerm.length <= 2 || file.name.contains(searchTerm, ignoreCase = true)
        }
        return if (section == LibrarySection.ALL) {
            filtered
        } else {
            filtered.filter { file ->
                file.type != ItemType.FOLDER && librarySectionFor(file.name) == section
            }
        }
    }

    fun setLibrarySection(section: LibrarySection) {
        _state.update {
            it.copy(
                librarySection = section,
                selectedIds = emptySet(),
                displayedFiles = computeDisplayed(it.files, it.searchTerm, it.searchResults, section)
            )
        }
    }

    fun setActiveFolder(id: Long?) {
        viewModelScope.launch {
            store.saveActiveFolderId(id)
            _state.update {
                it.copy(
                    activeFolderId = id,
                    librarySection = LibrarySection.ALL,
                    selectedIds = emptySet(),
                    showMoveModal = false,
                    searchTerm = "",
                    searchResults = emptyList(),
                    previewFile = null,
                    playingFile = null,
                    pdfFile = null,
                    previewContextFiles = emptyList(),
                    previewContextIndex = -1
                )
            }
            refreshFiles()
        }
    }

    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch {
            store.saveViewMode(mode)
            _state.update { it.copy(viewMode = mode) }
        }
    }

    fun toggleViewMode() {
        val next = if (_state.value.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
        setViewMode(next)
    }

    fun onSearchChange(term: String) {
        _state.update { it.copy(searchTerm = term) }
        searchJob?.cancel()
        if (term.length <= 2) {
            _state.update {
                it.copy(
                    searchResults = emptyList(),
                    isSearching = false,
                    displayedFiles = computeDisplayed(it.files, term, emptyList(), it.librarySection)
                )
            }
            return
        }
        searchJob = viewModelScope.launch {
            delay(500)
            _state.update { it.copy(isSearching = true) }
            val results = runCatching { repository.searchGlobal(term) }.getOrDefault(emptyList())
                .map { it.withFormattedSize() }
            _state.update {
                it.copy(
                    searchResults = results,
                    isSearching = false,
                    displayedFiles = computeDisplayed(it.files, term, results, it.librarySection)
                )
            }
        }
    }

    fun onFileClick(id: Long, additive: Boolean) {
        _state.update { current ->
            val next = if (additive) {
                if (id in current.selectedIds) current.selectedIds - id else current.selectedIds + id
            } else setOf(id)
            current.copy(selectedIds = next)
        }
    }

    fun toggleSelection(id: Long) {
        _state.update { current ->
            val next = if (id in current.selectedIds) current.selectedIds - id else current.selectedIds + id
            current.copy(selectedIds = next)
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedIds = emptySet()) }
    }

    fun selectAll() {
        _state.update { it.copy(selectedIds = it.displayedFiles.map { file -> file.id }.toSet()) }
    }

    fun handleEscape() {
        _state.update {
            it.copy(
                selectedIds = emptySet(),
                searchTerm = "",
                previewFile = null,
                playingFile = null,
                pdfFile = null,
                contextMenu = null,
                mediaStreamUrl = null,
                mediaLoading = false,
                mediaError = null
            )
        }
    }

    fun handleEnter() {
        val current = _state.value
        if (current.selectedIds.size != 1) return
        val selected = current.displayedFiles.find { it.id == current.selectedIds.first() } ?: return
        if (selected.type == ItemType.FOLDER) setActiveFolder(selected.id)
        else openPreview(selected, current.displayedFiles)
    }

    fun openPreview(file: TelegramFile, orderedFiles: List<TelegramFile> = _state.value.displayedFiles) {
        val contextFiles = orderedFiles.filter { it.type != ItemType.FOLDER }
        val contextIndex = contextFiles.indexOfFirst { it.id == file.id }
        when {
            isMediaFile(file.name) -> {
                _state.update {
                    it.copy(
                        playingFile = file,
                        previewFile = null,
                        pdfFile = null,
                        previewContextFiles = contextFiles,
                        previewContextIndex = contextIndex,
                        mediaStreamUrl = null,
                        mediaLoading = true,
                        mediaError = null
                    )
                }
                loadMedia(file)
            }
            isPdfFile(file.name) -> {
                _state.update {
                    it.copy(
                        pdfFile = file,
                        previewFile = null,
                        playingFile = null,
                        previewContextFiles = contextFiles,
                        previewContextIndex = contextIndex,
                        pdfLoading = true,
                        pdfError = null
                    )
                }
                loadStream()
            }
            else -> {
                _state.update {
                    it.copy(
                        previewFile = file,
                        playingFile = null,
                        pdfFile = null,
                        previewContextFiles = contextFiles,
                        previewContextIndex = contextIndex,
                        previewLoading = true,
                        previewError = null,
                        previewSrc = null
                    )
                }
                loadPreview(file)
            }
        }
    }

    private fun loadPreview(file: TelegramFile) {
        viewModelScope.launch {
            try {
                val src = repository.getPreview(file.id, file.folderId ?: _state.value.activeFolderId)
                _state.update {
                    it.copy(previewSrc = src, previewLoading = false, previewError = if (src == null) "Preview not available" else null)
                }
            } catch (err: Throwable) {
                _state.update { it.copy(previewLoading = false, previewError = err.message) }
            }
        }
    }

    private fun loadStream() {
        viewModelScope.launch {
            runCatching { repository.getStreamInfo() }
                .onSuccess { info -> _state.update { it.copy(streamInfo = info, pdfLoading = false) } }
                .onFailure { err -> _state.update { it.copy(pdfLoading = false, pdfError = err.message) } }
        }
    }

    private fun loadMedia(file: TelegramFile) {
        viewModelScope.launch {
            _state.update { it.copy(mediaLoading = true, mediaError = null, mediaStreamUrl = null) }
            runCatching {
                repository.prepareMediaPlayback(
                    messageId = file.id,
                    folderId = file.folderId ?: _state.value.activeFolderId,
                    fileName = file.name
                )
            }.onSuccess { src ->
                _state.update { it.copy(mediaStreamUrl = src, mediaLoading = false, mediaError = null) }
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        mediaLoading = false,
                        mediaError = err.message ?: "Unable to start media playback"
                    )
                }
            }
        }
    }

    private val thumbnailJobs = mutableMapOf<Long, Job>()

    /**
     * Lazily fetches an inline thumbnail for an image file and stores the resulting
     * model (base64 data URL) in [DashboardUiState.thumbnails]. This is what makes image
     * thumbnails actually render in the file grid / list — previously nothing requested them.
     */
    fun requestThumbnail(file: TelegramFile) {
        if (file.type == ItemType.FOLDER) return
        if (!isImageFile(file.name)) return
        val id = file.id
        if (_state.value.thumbnails.containsKey(id)) return
        if (thumbnailJobs.containsKey(id)) return
        thumbnailJobs[id] = viewModelScope.launch {
            runCatching { repository.getThumbnail(file.id, file.folderId ?: _state.value.activeFolderId) }
                .onSuccess { src ->
                    if (!src.isNullOrEmpty()) {
                        _state.update { it.copy(thumbnails = it.thumbnails + (id to src)) }
                    }
                }
            thumbnailJobs.remove(id)
        }
    }

    fun navigatePreview(step: Int) {
        val current = _state.value
        if (current.previewContextFiles.isEmpty()) return
        val currentId = current.previewFile?.id ?: current.playingFile?.id ?: current.pdfFile?.id ?: return
        val currentIndex = current.previewContextFiles.indexOfFirst { it.id == currentId }
        if (currentIndex < 0) return
        val nextIndex = (currentIndex + step + current.previewContextFiles.size) % current.previewContextFiles.size
        val nextFile = current.previewContextFiles[nextIndex]
        openPreview(nextFile, current.previewContextFiles)
    }

    fun closePreview() {
        _state.update {
            it.copy(
                previewFile = null,
                playingFile = null,
                pdfFile = null,
                previewSrc = null,
                previewError = null,
                mediaStreamUrl = null,
                mediaLoading = false,
                mediaError = null
            )
        }
    }

    fun setPdfScale(scale: Float) {
        _state.update { it.copy(pdfScale = scale.coerceIn(0.5f, 3f)) }
    }

    fun zoomPdf(delta: Float) {
        setPdfScale(_state.value.pdfScale + delta)
    }

    fun showMoveModal(show: Boolean) {
        _state.update { it.copy(showMoveModal = show) }
    }

    fun showContextMenu(x: Float, y: Float, file: TelegramFile) {
        _state.update { it.copy(contextMenu = ContextMenuState(x, y, file)) }
    }

    fun hideContextMenu() {
        _state.update { it.copy(contextMenu = null) }
    }

    fun setSort(field: SortField) {
        _state.update { current ->
            if (current.sortField == field) {
                current.copy(
                    sortDirection = if (current.sortDirection == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
                )
            } else {
                current.copy(sortField = field, sortDirection = SortDirection.ASC)
            }
        }
    }

    fun sortedFiles(): List<TelegramFile> {
        val current = _state.value
        val comparator = when (current.sortField) {
            SortField.NAME -> compareBy<TelegramFile> { it.name.lowercase() }
            SortField.SIZE -> compareBy { it.size }
            SortField.DATE -> compareBy { it.createdAt.orEmpty() }
        }
        return if (current.sortDirection == SortDirection.ASC) {
            current.displayedFiles.sortedWith(comparator)
        } else {
            current.displayedFiles.sortedWith(comparator.reversed())
        }
    }

    fun logout() {
        viewModelScope.launch {
            val ok = confirm.confirm(
                ConfirmOptions(
                    title = "Sign Out",
                    message = "Are you sure you want to sign out? This will disconnect your active session.",
                    confirmText = "Sign Out",
                    variant = ConfirmVariant.DANGER
                )
            )
            if (!ok) return@launch
            runCatching {
                repository.logout()
                repository.cleanCache()
                store.clearCredentials()
            }
            app.onLogout()
        }
    }

    fun syncFolders() {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            try {
                val found = repository.scanFolders()
                val merged = _state.value.folders.toMutableList()
                var added = 0
                found.forEach { folder ->
                    if (merged.none { it.id == folder.id }) {
                        merged += folder
                        added++
                    }
                }
                _state.update { it.copy(folders = merged, isSyncing = false) }
                store.saveFolders(merged)
                if (added > 0) app.showToast("Scan complete. Found $added new folders.")
                else app.showToast("Scan complete. No new folders found.")
            } catch (_: Throwable) {
                _state.update { it.copy(isSyncing = false) }
                app.showToast("Sync failed", isError = true)
            }
        }
    }

    fun setShowNewFolderInput(show: Boolean) {
        _state.update { it.copy(showNewFolderInput = show, newFolderName = if (show) it.newFolderName else "") }
    }

    fun onNewFolderNameChange(name: String) {
        _state.update { it.copy(newFolderName = name) }
    }

    fun createFolder() {
        val name = _state.value.newFolderName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            try {
                val created = repository.createFolder(name)
                val updated = _state.value.folders + created
                _state.update { it.copy(folders = updated, showNewFolderInput = false, newFolderName = "") }
                store.saveFolders(updated)
                app.showToast("Folder \"$name\" created.")
            } catch (err: Throwable) {
                app.showToast("Failed to create folder: $err", isError = true)
            }
        }
    }

    fun deleteFolder(folderId: Long, folderName: String) {
        viewModelScope.launch {
            val ok = confirm.confirm(
                ConfirmOptions(
                    title = "Delete Folder",
                    message = "Are you sure you want to delete \"$folderName\"?\nThis will delete the channel on Telegram.",
                    confirmText = "Delete",
                    variant = ConfirmVariant.DANGER
                )
            )
            if (!ok) return@launch
            try {
                repository.deleteFolder(folderId)
                removeFolderLocally(folderId, folderName)
            } catch (err: Throwable) {
                val errStr = err.message.orEmpty()
                if (errStr.contains("not found", ignoreCase = true)) {
                    val remove = confirm.confirm(
                        ConfirmOptions(
                            title = "Folder Not Found",
                            message = "Folder \"$folderName\" not found on Telegram (it may have been deleted externally).\nRemove from this app?",
                            confirmText = "Remove",
                            variant = ConfirmVariant.INFO
                        )
                    )
                    if (remove) removeFolderLocally(folderId, folderName)
                } else {
                    app.showToast("Failed to delete folder: $err", isError = true)
                }
            }
        }
    }

    private suspend fun removeFolderLocally(folderId: Long, folderName: String) {
        val updated = _state.value.folders.filterNot { it.id == folderId }
        _state.update {
            it.copy(
                folders = updated,
                activeFolderId = if (it.activeFolderId == folderId) null else it.activeFolderId
            )
        }
        store.saveFolders(updated)
        app.showToast("Folder \"$folderName\" deleted.")
        if (_state.value.activeFolderId == null) refreshFiles()
    }

    fun deleteFile(id: Long) {
        viewModelScope.launch {
            val ok = confirm.confirm(
                ConfirmOptions(
                    title = "Delete File",
                    message = "Are you sure you want to delete this file?",
                    confirmText = "Delete",
                    variant = ConfirmVariant.DANGER
                )
            )
            if (!ok) return@launch
            try {
                val file = _state.value.displayedFiles.find { it.id == id }
                repository.deleteFile(id, file?.folderId ?: _state.value.activeFolderId)
                app.showToast("File deleted")
                refreshFiles()
            } catch (err: Throwable) {
                app.showToast("Delete failed: $err", isError = true)
            }
        }
    }

    fun bulkDelete() {
        viewModelScope.launch {
            val ids = _state.value.selectedIds.toList()
            if (ids.isEmpty()) return@launch
            val ok = confirm.confirm(
                ConfirmOptions(
                    title = "Delete Files",
                    message = "Are you sure you want to delete ${ids.size} files?",
                    confirmText = "Delete All",
                    variant = ConfirmVariant.DANGER
                )
            )
            if (!ok) return@launch
            var success = 0
            var fail = 0
            ids.forEach { id ->
                val file = _state.value.displayedFiles.find { it.id == id }
                runCatching { repository.deleteFile(id, file?.folderId ?: _state.value.activeFolderId) }
                    .onSuccess { success++ }
                    .onFailure { fail++ }
            }
            _state.update { it.copy(selectedIds = emptySet()) }
            refreshFiles()
            if (success > 0) app.showToast("Deleted $success files.")
            if (fail > 0) app.showToast("Failed to delete $fail files.", isError = true)
        }
    }

    fun bulkMove(targetFolderId: Long?) {
        viewModelScope.launch {
            val ids = _state.value.selectedIds.toList()
            if (ids.isEmpty()) return@launch
            try {
                repository.moveFiles(ids, _state.value.activeFolderId, targetFolderId)
                app.showToast("Moved ${ids.size} files.")
                _state.update { it.copy(selectedIds = emptySet(), showMoveModal = false) }
                refreshFiles()
            } catch (_: Throwable) {
                app.showToast("Failed to move files", isError = true)
            }
        }
    }

    fun dropOnFolder(fileId: Long, targetFolderId: Long?) {
        viewModelScope.launch {
            val current = _state.value
            if (current.activeFolderId == targetFolderId) return@launch
            val ids = if (fileId in current.selectedIds) current.selectedIds.toList() else listOf(fileId)
            try {
                repository.moveFiles(ids, current.activeFolderId, targetFolderId)
                if (fileId in current.selectedIds) _state.update { it.copy(selectedIds = emptySet()) }
                app.showToast("Moved ${ids.size} file(s).")
                refreshFiles()
            } catch (_: Throwable) {
                app.showToast("Failed to move file(s).", isError = true)
            }
        }
    }

    fun queueDownload(messageId: Long, filename: String, folderId: Long? = _state.value.activeFolderId) {
        val item = DownloadItem(
            id = randomId(),
            messageId = messageId,
            filename = filename,
            folderId = folderId,
            status = TransferStatus.PENDING
        )
        _state.update { it.copy(downloadQueue = it.downloadQueue + item) }
        persistDownloads()
        processNextDownload()
    }

    fun queueUploads(uris: List<Uri>, resolverPath: (Uri) -> String?) {
        viewModelScope.launch {
            val items = uris.mapNotNull { uri ->
                val path = resolverPath(uri) ?: return@mapNotNull null
                QueueItem(
                    id = randomId(),
                    path = path,
                    folderId = _state.value.activeFolderId,
                    status = TransferStatus.PENDING
                )
            }
            if (items.isEmpty()) {
                app.showToast("Could not read the selected files", isError = true)
                return@launch
            }
            _state.update { it.copy(uploadQueue = it.uploadQueue + items) }
            persistUploads()
            app.showToast("Queued ${items.size} files for upload")
            processNextUpload()
        }
    }

    fun cancelAllUploads() {
        val uploading = _state.value.uploadQueue.find { it.status == TransferStatus.UPLOADING }
        if (uploading != null) {
            cancelledUploads += uploading.id
            viewModelScope.launch { runCatching { repository.cancelTransfer(uploading.id) } }
        }
        _state.update { current ->
            current.copy(
                uploadQueue = current.uploadQueue
                    .filter { it.status != TransferStatus.PENDING }
                    .map { if (it.status == TransferStatus.UPLOADING) it.copy(status = TransferStatus.CANCELLED) else it }
            )
        }
        persistUploads()
        app.showToast("All uploads cancelled")
    }

    fun cancelUploadItem(id: String) {
        val item = _state.value.uploadQueue.find { it.id == id } ?: return
        if (item.status == TransferStatus.UPLOADING) {
            cancelledUploads += id
            viewModelScope.launch { runCatching { repository.cancelTransfer(id) } }
            _state.update { current ->
                current.copy(uploadQueue = current.uploadQueue.map { if (it.id == id) it.copy(status = TransferStatus.CANCELLED) else it })
            }
        } else if (item.status == TransferStatus.PENDING) {
            _state.update { current -> current.copy(uploadQueue = current.uploadQueue.filterNot { it.id == id }) }
        }
        persistUploads()
    }

    fun retryUploadItem(id: String) {
        _state.update { current ->
            current.copy(
                uploadQueue = current.uploadQueue.map {
                    if (it.id == id && (it.status == TransferStatus.ERROR || it.status == TransferStatus.CANCELLED)) {
                        it.copy(status = TransferStatus.PENDING, error = null, progress = null, uploadedBytes = null, totalBytes = null, speedBytesPerSec = null)
                    } else it
                }
            )
        }
        persistUploads()
        processNextUpload()
    }

    fun clearFinishedUploads() {
        _state.update { current ->
            current.copy(
                uploadQueue = current.uploadQueue.filter {
                    it.status != TransferStatus.SUCCESS && it.status != TransferStatus.ERROR && it.status != TransferStatus.CANCELLED
                }
            )
        }
        persistUploads()
    }

    fun cancelAllDownloads() {
        val downloading = _state.value.downloadQueue.find { it.status == TransferStatus.DOWNLOADING }
        if (downloading != null) {
            cancelledDownloads += downloading.id
            viewModelScope.launch { runCatching { repository.cancelTransfer(downloading.id) } }
        }
        _state.update { current ->
            current.copy(
                downloadQueue = current.downloadQueue
                    .filter { it.status != TransferStatus.PENDING }
                    .map { if (it.status == TransferStatus.DOWNLOADING) it.copy(status = TransferStatus.CANCELLED) else it }
            )
        }
        persistDownloads()
        app.showToast("All downloads cancelled")
    }

    fun cancelDownloadItem(id: String) {
        val item = _state.value.downloadQueue.find { it.id == id } ?: return
        if (item.status == TransferStatus.DOWNLOADING) {
            cancelledDownloads += id
            viewModelScope.launch { runCatching { repository.cancelTransfer(id) } }
            _state.update { current ->
                current.copy(downloadQueue = current.downloadQueue.map { if (it.id == id) it.copy(status = TransferStatus.CANCELLED) else it })
            }
        } else if (item.status == TransferStatus.PENDING) {
            _state.update { current -> current.copy(downloadQueue = current.downloadQueue.filterNot { it.id == id }) }
        }
        persistDownloads()
    }

    fun retryDownloadItem(id: String) {
        _state.update { current ->
            current.copy(
                downloadQueue = current.downloadQueue.map {
                    if (it.id == id && (it.status == TransferStatus.ERROR || it.status == TransferStatus.CANCELLED)) {
                        it.copy(status = TransferStatus.PENDING, error = null, progress = null, uploadedBytes = null, totalBytes = null, speedBytesPerSec = null)
                    } else it
                }
            )
        }
        persistDownloads()
        processNextDownload()
    }

    fun clearFinishedDownloads() {
        _state.update { current ->
            current.copy(downloadQueue = current.downloadQueue.filter { it.status != TransferStatus.SUCCESS })
        }
        persistDownloads()
    }

    private fun processNextUpload() {
        if (uploadProcessing) return
        val next = _state.value.uploadQueue.find { it.status == TransferStatus.PENDING } ?: return
        uploadProcessing = true
        viewModelScope.launch {
            _state.update { current ->
                current.copy(uploadQueue = current.uploadQueue.map {
                    if (it.id == next.id) it.copy(status = TransferStatus.UPLOADING, progress = 0f) else it
                })
            }
            try {
                repository.uploadFile(next.path, next.folderId, next.id)
                if (cancelledUploads.remove(next.id)) {
                    // cancelled mid-flight
                } else {
                    _state.update { current ->
                        current.copy(uploadQueue = current.uploadQueue.map {
                            if (it.id == next.id) it.copy(status = TransferStatus.SUCCESS, progress = 100f) else it
                        })
                    }
                    refreshFiles()
                }
            } catch (err: Throwable) {
                val msg = err.message.orEmpty()
                if (msg.contains("Transfer cancelled")) {
                    _state.update { current ->
                        current.copy(uploadQueue = current.uploadQueue.map {
                            if (it.id == next.id) it.copy(status = TransferStatus.CANCELLED) else it
                        })
                    }
                } else if (!cancelledUploads.remove(next.id)) {
                    _state.update { current ->
                        current.copy(uploadQueue = current.uploadQueue.map {
                            if (it.id == next.id) it.copy(status = TransferStatus.ERROR, error = msg) else it
                        })
                    }
                    app.showToast("Upload failed for ${next.path.substringAfterLast('/')}: $err", isError = true)
                }
            } finally {
                uploadProcessing = false
                persistUploads()
                processNextUpload()
            }
        }
    }

    private fun processNextDownload() {
        if (downloadProcessing) return
        val next = _state.value.downloadQueue.find { it.status == TransferStatus.PENDING } ?: return
        downloadProcessing = true
        viewModelScope.launch {
            _state.update { current ->
                current.copy(downloadQueue = current.downloadQueue.map {
                    if (it.id == next.id) it.copy(status = TransferStatus.DOWNLOADING, progress = 0f) else it
                })
            }
            val savePath = downloadFileFor(next.filename).absolutePath
            try {
                repository.downloadFile(next.messageId, savePath, next.folderId, next.id)
                if (cancelledDownloads.remove(next.id)) {
                    // cancelled
                } else {
                    _state.update { current ->
                        current.copy(downloadQueue = current.downloadQueue.map {
                            if (it.id == next.id) {
                                it.copy(status = TransferStatus.SUCCESS, progress = 100f, localPath = savePath)
                            } else it
                        })
                    }
                    app.showToast("Downloaded: ${next.filename}")
                    refreshLocalDownloads()
                }
            } catch (err: Throwable) {
                val msg = err.message.orEmpty()
                if (msg.contains("Transfer cancelled")) {
                    _state.update { current ->
                        current.copy(downloadQueue = current.downloadQueue.map {
                            if (it.id == next.id) it.copy(status = TransferStatus.CANCELLED) else it
                        })
                    }
                } else if (!cancelledDownloads.remove(next.id)) {
                    _state.update { current ->
                        current.copy(downloadQueue = current.downloadQueue.map {
                            if (it.id == next.id) it.copy(status = TransferStatus.ERROR, error = msg) else it
                        })
                    }
                    app.showToast("Download failed: ${next.filename}", isError = true)
                }
            } finally {
                downloadProcessing = false
                persistDownloads()
                processNextDownload()
            }
        }
    }

    private fun persistUploads() {
        if (!initializedQueues) return
        viewModelScope.launch { store.savePendingUploads(_state.value.uploadQueue) }
    }

    private fun persistDownloads() {
        if (!initializedQueues) return
        viewModelScope.launch { store.savePendingDownloads(_state.value.downloadQueue) }
    }

    fun currentFolderName(): String {
        val sectionTitle = when (_state.value.librarySection) {
            LibrarySection.ALL -> null
            LibrarySection.VIDEOS -> "Videos"
            LibrarySection.PICTURES -> "Pictures"
            LibrarySection.DOCUMENTS -> "Documents"
            LibrarySection.OTHERS -> "Others"
        }
        if (sectionTitle != null) return sectionTitle
        val id = _state.value.activeFolderId ?: return "Saved Messages"
        return _state.value.folders.find { it.id == id }?.name ?: "Folder"
    }

    fun downloadsDir(): File {
        val dir = File(getApplication<Application>().cacheDir, "downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun downloadFileFor(filename: String): File {
        val dir = downloadsDir()
        val safe = filename.ifBlank { "download" }.replace(Regex("[\\\\/]+"), "_")
        var candidate = File(dir, safe)
        if (!candidate.exists()) return candidate
        val stem = safe.substringBeforeLast('.', safe)
        val ext = safe.substringAfterLast('.', "")
        var index = 1
        while (candidate.exists()) {
            val nextName = if (ext.isBlank()) "${stem}_$index" else "${stem}_$index.$ext"
            candidate = File(dir, nextName)
            index++
        }
        return candidate
    }

    fun refreshLocalDownloads() {
        val files = downloadsDir()
            .listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                LocalDownload(
                    path = file.absolutePath,
                    name = file.name,
                    size = file.length(),
                    sizeStr = formatBytes(file.length()),
                    modifiedAt = file.lastModified()
                )
            }
            .orEmpty()
        _state.update { it.copy(localDownloads = files) }
    }

    fun setShowTransfers(show: Boolean) {
        _state.update { it.copy(showTransfers = show) }
    }

    fun setShowDownloadsWindow(show: Boolean) {
        if (show) refreshLocalDownloads()
        _state.update { it.copy(showDownloadsWindow = show, showTransfers = if (show) false else it.showTransfers) }
    }

    fun clearCache() {
        viewModelScope.launch {
            val ok = confirm.confirm(
                ConfirmOptions(
                    title = "Clear cache",
                    message = "This removes cached previews and temporary files. Downloaded files in the cache folder will also be removed.",
                    confirmText = "Clear",
                    variant = ConfirmVariant.DANGER
                )
            )
            if (!ok) return@launch
            _state.update { it.copy(isClearingCache = true) }
            runCatching { repository.cleanCache() }
            runCatching {
                getApplication<Application>().cacheDir.listFiles()?.forEach { child ->
                    child.deleteRecursively()
                }
            }
            downloadsDir()
            refreshLocalDownloads()
            _state.update { it.copy(isClearingCache = false) }
            app.showToast("Cache cleared")
        }
    }

    fun streamUrlFor(file: TelegramFile, info: StreamInfo): String {
        return repository.streamUrl(file.folderId ?: _state.value.activeFolderId, file.id, info)
    }

    fun setDropBlocker(show: Boolean) {
        _state.update { it.copy(showDropBlocker = show) }
    }

    fun neighborFiles(): Pair<TelegramFile?, TelegramFile?> {
        val current = _state.value
        if (current.previewContextFiles.isEmpty()) return null to null
        val currentId = current.previewFile?.id ?: current.playingFile?.id ?: current.pdfFile?.id ?: return null to null
        val idx = current.previewContextFiles.indexOfFirst { it.id == currentId }
        if (idx < 0) return null to null
        val nextIdx = (idx + 1) % current.previewContextFiles.size
        val prevIdx = (idx - 1 + current.previewContextFiles.size) % current.previewContextFiles.size
        return current.previewContextFiles.getOrNull(nextIdx) to current.previewContextFiles.getOrNull(prevIdx)
    }
}
