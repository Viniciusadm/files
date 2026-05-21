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
import kotlinx.coroutines.launch
import java.io.File

class MessagesViewModel(private val channelsRepository: ChannelsRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Channel>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Channel>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { channelsRepository.getChannels() }
                .onSuccess { _state.value = if (it.isEmpty()) UiState.Empty else UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(it.userMessage()) }
        }
    }

    fun createChannel(name: String, icon: String, color: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { channelsRepository.createChannel(name, icon, color) }
                .onSuccess {
                    load()
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
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                val channel = channelsRepository.getChannel(folderId)
                val messages = messagesRepository.getMessages(folderId)
                if (channel == null) null else ThreadUiState(channel, messages)
            }.onSuccess {
                _state.value = if (it == null) UiState.Empty else UiState.Success(it)
            }.onFailure {
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
                    load()
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
                    load()
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
        return messagesRepository.cachePlainFile(
            attachment.fileName,
            messagesRepository.downloadAttachment(attachment.url)
        )
    }

    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { load() }
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
