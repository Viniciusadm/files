package com.archieapps.fles.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.fles.data.repository.ChannelsRepository
import com.archieapps.fles.data.repository.MessagesRepository
import com.archieapps.fles.data.repository.SecureFoldersRepository
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.ui.model.PendingAttachment
import com.archieapps.fles.ui.viewmodel.SharePayload
import com.archieapps.fles.ui.viewmodel.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShareTargetViewModel(
    private val channelsRepository: ChannelsRepository,
    private val messagesRepository: MessagesRepository,
    private val secureFoldersRepository: SecureFoldersRepository
) : ViewModel() {

    private var payload: SharePayload? = null
    private val _state = MutableStateFlow(ShareTargetUiState())
    val state: StateFlow<ShareTargetUiState> = _state.asStateFlow()

    fun setPayload(value: SharePayload) {
        payload = value
        _state.update {
            it.copy(
                summary = value.summary(),
                attachmentsPreview = value.attachments,
                error = null,
                toast = null,
                sending = false
            )
        }
        load()
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        load()
    }

    fun sendTo(channelId: String, onDone: () -> Unit) {
        val current = payload ?: return
        if (_state.value.sending) return
        viewModelScope.launch {
            _state.update { it.copy(sending = true, toast = "Enviando...", error = null) }
            runCatching {
                messagesRepository.send(
                    folderId = channelId,
                    content = current.content(),
                    attachments = current.attachments
                )
            }.onSuccess {
                _state.update { it.copy(sending = false, toast = "Enviado!") }
                onDone()
            }.onFailure { error ->
                _state.update { it.copy(sending = false, toast = null, error = error.userMessage()) }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val query = _state.value.query
                val normal = channelsRepository.getChannels()
                val secure = if (secureFoldersRepository.state.value.unlocked) {
                    channelsRepository.getSecureChannels()
                } else {
                    emptyList()
                }
                (normal + secure).filter { it.name.contains(query, ignoreCase = true) }
            }.onSuccess { channels ->
                _state.update { it.copy(channels = channels, loading = false) }
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }
}

data class ShareTargetUiState(
    val channels: List<Channel> = emptyList(),
    val query: String = "",
    val summary: String = "Conteúdo compartilhado",
    val attachmentsPreview: List<PendingAttachment> = emptyList(),
    val toast: String? = null,
    val loading: Boolean = true,
    val sending: Boolean = false,
    val error: String? = null
)
