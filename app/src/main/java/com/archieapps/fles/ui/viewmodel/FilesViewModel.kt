package com.archieapps.fles.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.fles.data.repository.ChannelsRepository
import com.archieapps.fles.data.repository.FilesRepository
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
    private val filesRepository: FilesRepository
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

data class FilesUiState(
    val folder: Channel? = null,
    val files: List<FileItem> = emptyList(),
    val query: String = "",
    val mode: FilesViewMode = FilesViewMode.Grid,
    val favoritesOnly: Boolean = false,
    val activeKind: FileKind? = null,
    val loading: Boolean = false,
    val error: String? = null
) {
    val isEmpty: Boolean = !loading && error == null && files.isEmpty()
}

data class FileActionState(
    val message: String,
    val isError: Boolean = false
)
