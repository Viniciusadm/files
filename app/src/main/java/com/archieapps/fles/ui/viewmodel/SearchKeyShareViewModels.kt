package com.archieapps.fles.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.fles.data.repository.ChannelsRepository
import com.archieapps.fles.data.repository.CryptoRepository
import com.archieapps.fles.data.repository.MessagesRepository
import com.archieapps.fles.data.repository.SearchResult
import com.archieapps.fles.ui.model.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    initialQuery: String,
    private val messagesRepository: MessagesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState(query = initialQuery))
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        search(initialQuery)
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        search(value)
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { messagesRepository.search(query) }
                .onSuccess { results -> _state.update { it.copy(results = results, loading = false) } }
                .onFailure { error -> _state.update { it.copy(loading = false, error = error.userMessage()) } }
        }
    }
}

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class KeyViewModel(private val cryptoRepository: CryptoRepository) : ViewModel() {
    private val _state = MutableStateFlow(KeyUiState(loading = true))
    val state: StateFlow<KeyUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onValueChange(value: String) {
        _state.update { it.copy(value = value, error = null) }
    }

    fun save() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { cryptoRepository.saveKey(_state.value.value) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            configured = true,
                            value = "",
                            fingerprint = cryptoRepository.fingerprint(),
                            loading = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.userMessage()) }
                }
        }
    }

    fun generate() {
        _state.update { it.copy(value = cryptoRepository.generateKey(), error = null) }
    }

    fun clear() {
        viewModelScope.launch {
            cryptoRepository.clearKey()
            _state.update { it.copy(configured = false, value = "", fingerprint = null, error = null) }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    configured = cryptoRepository.hasKey(),
                    fingerprint = cryptoRepository.fingerprint(),
                    loading = false,
                    error = null
                )
            }
        }
    }
}

data class KeyUiState(
    val value: String = "",
    val configured: Boolean = false,
    val fingerprint: String? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class ShareViewModel(private val channelsRepository: ChannelsRepository) : ViewModel() {
    private val _state = MutableStateFlow(ShareUiState(loading = true))
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        load()
    }

    fun markSent() {
        _state.update { it.copy(toast = "Enviado!") }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val query = _state.value.query
                channelsRepository.getChannels().filter { it.name.contains(query, ignoreCase = true) }
            }.onSuccess { channels ->
                _state.update { it.copy(channels = channels, loading = false) }
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }
}

data class ShareUiState(
    val channels: List<Channel> = emptyList(),
    val query: String = "",
    val summary: String = "Conteúdo compartilhado",
    val previewSite: String? = null,
    val previewTitle: String? = null,
    val previewDescription: String? = null,
    val toast: String? = null,
    val loading: Boolean = false,
    val error: String? = null
)
