package com.archieapps.fles.ui.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.fles.data.repository.AttachmentIo
import com.archieapps.fles.data.repository.CryptoRepository
import com.archieapps.fles.data.repository.MessagesRepository
import com.archieapps.fles.data.repository.SearchResult
import com.archieapps.fles.data.repository.SecureFoldersRepository
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

    fun save(onSaved: () -> Unit = {}) {
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
                    onSaved()
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.userMessage()) }
                }
        }
    }

    fun generate() {
        _state.update { it.copy(value = cryptoRepository.generateKey(), error = null) }
    }

    fun clear(onCleared: () -> Unit = {}) {
        viewModelScope.launch {
            cryptoRepository.clearKey()
            _state.update { it.copy(configured = false, value = "", fingerprint = null, error = null) }
            onCleared()
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

class SecureFoldersViewModel(private val repository: SecureFoldersRepository) : ViewModel() {
    private val _state = MutableStateFlow(SecureFoldersUiState(loading = true))
    val state: StateFlow<SecureFoldersUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onPinChange(value: String) {
        _state.update { it.copy(pin = value.filter(Char::isDigit).take(4), error = null) }
    }

    fun setup(onUnlocked: () -> Unit) {
        submit(onUnlocked) { pin -> repository.setup(pin) }
    }

    fun unlock(onUnlocked: () -> Unit) {
        submit(onUnlocked) { pin -> repository.unlock(pin) }
    }

    fun reset(onUnlocked: () -> Unit) {
        submit(onUnlocked) { pin -> repository.reset(pin) }
    }

    fun lock() {
        repository.lock()
        _state.update { it.copy(unlocked = false, pin = "") }
    }

    private fun refresh() {
        viewModelScope.launch {
            runCatching { repository.refreshMeta() }
                .onSuccess { meta ->
                    _state.update {
                        it.copy(
                            configured = meta?.configured == true,
                            unlocked = repository.state.value.unlocked,
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

    private fun submit(onUnlocked: () -> Unit, block: suspend (String) -> Unit) {
        val pin = _state.value.pin
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { block(pin) }
                .onSuccess {
                    _state.update { it.copy(configured = true, unlocked = true, pin = "", loading = false, error = null) }
                    onUnlocked()
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.userMessage()) }
                }
        }
    }
}

data class SecureFoldersUiState(
    val configured: Boolean = false,
    val unlocked: Boolean = false,
    val pin: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

data class SharePayload(
    val text: String? = null,
    val webUrl: String? = null,
    val attachments: List<com.archieapps.fles.ui.model.PendingAttachment> = emptyList()
) {
    fun content(): String {
        return listOfNotNull(text, webUrl?.takeIf { it != text }).joinToString("\n").trim()
    }

    fun summary(): String {
        return when {
            attachments.size == 1 -> attachments.first().fileName
            attachments.size > 1 -> "${attachments.size} arquivos"
            !webUrl.isNullOrBlank() -> webUrl
            !text.isNullOrBlank() -> text
            else -> "Conteúdo compartilhado"
        }
    }

    companion object {
        fun fromIntent(intent: Intent?, attachmentIo: AttachmentIo): SharePayload? {
            if (intent == null) return null
            val action = intent.action ?: return null
            if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return null
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            val uris = mutableListOf<Uri>()
            if (action == Intent.ACTION_SEND) {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.add(it) }
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { list ->
                    uris.addAll(list)
                }
            }
            intent.clipData?.let { clip ->
                repeat(clip.itemCount) { index ->
                    clip.getItemAt(index).uri?.let { uris.add(it) }
                }
            }
            val attachments = uris.distinct().mapNotNull { uri ->
                runCatching { attachmentIo.pendingFromUri(uri) }.getOrNull()
            }
            val payload = SharePayload(text = text ?: subject, webUrl = null, attachments = attachments)
            return if (payload.text.isNullOrBlank() && payload.attachments.isEmpty()) null else payload
        }
    }
}
