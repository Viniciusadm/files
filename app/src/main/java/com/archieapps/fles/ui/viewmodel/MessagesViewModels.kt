package com.archieapps.fles.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.fles.data.repository.ChannelsRepository
import com.archieapps.fles.data.repository.MessagesRepository
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.ui.model.Attachment
import com.archieapps.fles.ui.model.Message
import com.archieapps.fles.ui.model.PendingAttachment
import com.archieapps.fles.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class MessagesViewModel(private val channelsRepository: ChannelsRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Channel>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Channel>>> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            channelsRepository.observeChannels().collectLatest {
                _state.value = if (it.isEmpty()) UiState.Empty else UiState.Success(it)
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            runCatching { channelsRepository.refreshFromRemote() }
                .onFailure { _state.value = UiState.Error(it.userMessage()) }
        }
    }

    fun createChannel(name: String, icon: String, color: String, isSecure: Boolean = false, parentLocalId: String? = null, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { channelsRepository.createChannel(name, icon, color, isSecure, parentLocalId) }
                .onSuccess {
                    onDone()
                }
                .onFailure { _state.value = UiState.Error(it.userMessage()) }
        }
    }
}

class ThreadViewModel(
    private val folderId: String,
    private val channelsRepository: ChannelsRepository,
    private val messagesRepository: MessagesRepository
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<ThreadUiState>>(UiState.Loading)
    val state: StateFlow<UiState<ThreadUiState>> = _state.asStateFlow()
    private val _action = MutableStateFlow(ThreadActionState())
    val action: StateFlow<ThreadActionState> = _action.asStateFlow()

    init {
        viewModelScope.launch {
            messagesRepository.observeMessages(folderId).collectLatest { messages ->
                val channel = channelsRepository.getChannel(folderId)
                _state.value = if (channel == null) UiState.Empty else UiState.Success(ThreadUiState(channel, messages))
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            runCatching { messagesRepository.refreshMessagesFromRemote(folderId) }.onFailure {
                _state.value = UiState.Error(it.userMessage())
            }
        }
    }

    fun send(content: String, attachments: List<PendingAttachment>, onDone: () -> Unit) {
        viewModelScope.launch {
            _action.value = ThreadActionState(sending = true)
            runCatching { messagesRepository.send(folderId, content, attachments) }
                .onSuccess {
                    _action.value = ThreadActionState(success = "Enviado")
                    onDone()
                }
                .onFailure { _action.value = ThreadActionState(error = it.userMessage()) }
        }
    }

    fun createChecklist(title: String, items: List<String>, onDone: () -> Unit) {
        viewModelScope.launch {
            _action.value = ThreadActionState(sending = true)
            runCatching { messagesRepository.createChecklist(folderId, title, items) }
                .onSuccess {
                    _action.value = ThreadActionState(success = "Lista criada")
                    onDone()
                }
                .onFailure { _action.value = ThreadActionState(error = it.userMessage()) }
        }
    }

    fun deleteThread(onDone: () -> Unit) {
        viewModelScope.launch {
            _action.value = ThreadActionState(sending = true)
            runCatching { channelsRepository.deleteChannel(folderId) }
                .onSuccess {
                    _action.value = ThreadActionState(success = "Conversa excluída")
                    onDone()
                }
                .onFailure { _action.value = ThreadActionState(error = it.userMessage()) }
        }
    }

    fun toggleFavorite(messageId: String) = mutate { messagesRepository.toggleFavorite(messageId) }
    fun deleteMessage(messageId: String) = mutate { messagesRepository.deleteMessage(messageId) }
    fun moveMessage(messageId: String, targetFolderId: String) = mutate { messagesRepository.moveMessage(messageId, targetFolderId) }
    fun toggleListItem(messageId: String, itemId: String) = mutate { messagesRepository.toggleListItem(messageId, itemId) }
    fun addListItem(messageId: String, text: String) = mutate { messagesRepository.addListItem(messageId, text) }
    fun updateListItem(messageId: String, itemId: String, text: String) = mutate { messagesRepository.updateListItem(messageId, itemId, text = text) }
    fun deleteListItem(messageId: String, itemId: String) = mutate { messagesRepository.deleteListItem(messageId, itemId) }
    fun reorderListItems(messageId: String, orderedIds: List<String>) = mutate { messagesRepository.reorderListItems(messageId, orderedIds) }

    suspend fun downloadToCache(attachment: Attachment): File {
        return messagesRepository.downloadAttachment(attachment)
    }

    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { _action.value = ThreadActionState() }
                .onFailure { _action.value = ThreadActionState(error = it.userMessage()) }
        }
    }
}

data class ThreadUiState(
    val channel: Channel,
    val messages: List<Message>
)

data class ThreadActionState(
    val sending: Boolean = false,
    val success: String? = null,
    val error: String? = null
)
