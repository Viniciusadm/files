package com.archieapps.fles.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.fles.data.repository.ChannelsRepository
import com.archieapps.fles.data.repository.FilesRepository
import com.archieapps.fles.data.repository.MessagesRepository
import com.archieapps.fles.ui.components.FilesViewMode
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.ui.model.FileItem
import com.archieapps.fles.ui.model.FileKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class FilesViewModel(
    private val folderId: String?,
    favoritesOnlyInitial: Boolean,
    private val channelsRepository: ChannelsRepository,
    private val filesRepository: FilesRepository,
    private val messagesRepository: MessagesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(FilesUiState(favoritesOnly = favoritesOnlyInitial, loading = true))
    val state: StateFlow<FilesUiState> = _state.asStateFlow()
    private val _action = MutableStateFlow<FileActionState?>(null)
    val action: StateFlow<FileActionState?> = _action.asStateFlow()

    init {
        viewModelScope.launch {
            filesRepository.observeFiles(folderId, favoritesOnlyInitial).collectLatest { (folder, files) ->
                _state.update { it.copy(folder = folder, files = files, loading = false, error = null) }
            }
        }
        load()
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        load()
    }

    fun onModeChange(value: FilesViewMode) {
        _state.update { it.copy(mode = value) }
    }

    fun onFavoritesToggle() {
        _state.update { it.copy(favoritesOnly = !it.favoritesOnly) }
        load()
    }

    fun onKindChange(value: FileKind?) {
        _state.update { it.copy(activeKind = value) }
        load()
    }

    fun onGalleryGroupModeChange(value: GalleryGroupMode) {
        _state.update { it.copy(galleryGroupMode = value, selectedGalleryFolderId = null) }
    }

    fun onSelectGalleryFolder(folderId: String?) {
        _state.update { it.copy(selectedGalleryFolderId = folderId) }
    }

    fun load() {
        viewModelScope.launch {
            runCatching {
                val current = _state.value
                val folder = folderId?.let { channelsRepository.getChannel(it) }
                val files = filesRepository.getFiles(
                    folderId = folderId,
                    favoritesOnly = current.favoritesOnly,
                    query = current.query,
                    kind = current.activeKind
                )
                folder to files
            }.onSuccess { (folder, files) ->
                _state.update { it.copy(folder = folder, files = files, loading = false, error = null) }
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun clearAction() {
        _action.value = null
    }

    fun toggleSelection(attachmentId: String) {
        _state.update { current ->
            val next = current.selectedIds.toMutableSet().apply {
                if (!add(attachmentId)) remove(attachmentId)
            }
            current.copy(selectedIds = next)
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedIds = emptySet()) }
    }

    fun bulkDelete() {
        val ids = _state.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching { messagesRepository.bulkDeleteAttachments(ids) }
                .onSuccess { _action.value = FileActionState("${ids.size} arquivo(s) excluído(s).") }
                .onFailure { _action.value = FileActionState(it.userMessage(), isError = true) }
            clearSelection()
        }
    }

    fun bulkMove(targetFolderId: String) {
        val ids = _state.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching { messagesRepository.bulkMoveAttachments(ids, targetFolderId) }
                .onSuccess { _action.value = FileActionState("${ids.size} arquivo(s) movido(s).") }
                .onFailure { _action.value = FileActionState(it.userMessage(), isError = true) }
            clearSelection()
        }
    }

    fun deleteFile(attachmentId: String) {
        viewModelScope.launch {
            runCatching { messagesRepository.deleteAttachment(attachmentId) }
                .onSuccess { _action.value = FileActionState("Arquivo excluído.") }
                .onFailure { _action.value = FileActionState(it.userMessage(), isError = true) }
        }
    }

    fun moveFile(attachmentId: String, targetFolderId: String) {
        viewModelScope.launch {
            runCatching { messagesRepository.moveAttachment(attachmentId, targetFolderId) }
                .onSuccess { _action.value = FileActionState("Arquivo movido.") }
                .onFailure { _action.value = FileActionState(it.userMessage(), isError = true) }
        }
    }

    fun toggleFavorite(attachmentId: String) {
        viewModelScope.launch {
            runCatching { messagesRepository.favoriteAttachment(attachmentId) }
                .onFailure { _action.value = FileActionState(it.userMessage(), isError = true) }
        }
    }

    fun renameFile(attachmentId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            runCatching { messagesRepository.renameAttachment(attachmentId, trimmed) }
                .onSuccess { _action.value = FileActionState("Arquivo renomeado.") }
                .onFailure { _action.value = FileActionState(it.userMessage(), isError = true) }
        }
    }

    suspend fun downloadToCache(file: FileItem): File {
        return filesRepository.downloadToCache(file)
    }

    suspend fun ensureLocalFile(file: FileItem): File {
        if (file.isSecure) return filesRepository.downloadToCache(file)
        file.localPath?.removePrefix("file://")?.let {
            val localFile = File(it)
            if (localFile.exists()) return localFile
        }
        return filesRepository.downloadToCache(file)
    }
}

enum class GalleryGroupMode { Folders, All }

data class FilesUiState(
    val folder: Channel? = null,
    val files: List<FileItem> = emptyList(),
    val query: String = "",
    val mode: FilesViewMode = FilesViewMode.Grid,
    val favoritesOnly: Boolean = false,
    val activeKind: FileKind? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val galleryGroupMode: GalleryGroupMode = GalleryGroupMode.Folders,
    val selectedGalleryFolderId: String? = null,
    val selectedIds: Set<String> = emptySet()
) {
    val isEmpty: Boolean = !loading && error == null && files.isEmpty()
    val selectionMode: Boolean = selectedIds.isNotEmpty()
}

data class FileActionState(
    val message: String,
    val isError: Boolean = false
)
