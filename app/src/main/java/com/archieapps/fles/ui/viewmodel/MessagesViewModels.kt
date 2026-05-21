package com.archieapps.fles.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.fles.data.repository.ChannelsRepository
import com.archieapps.fles.data.repository.MessagesRepository
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.ui.model.Message
import com.archieapps.fles.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
}

class ThreadViewModel(
    private val folderId: String,
    private val channelsRepository: ChannelsRepository,
    private val messagesRepository: MessagesRepository
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<ThreadUiState>>(UiState.Loading)
    val state: StateFlow<UiState<ThreadUiState>> = _state.asStateFlow()

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
}

data class ThreadUiState(
    val channel: Channel,
    val messages: List<Message>
)
