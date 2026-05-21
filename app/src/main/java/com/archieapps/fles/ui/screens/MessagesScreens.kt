package com.archieapps.fles.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.archieapps.fles.ui.components.ChatHeader
import com.archieapps.fles.ui.components.Composer
import com.archieapps.fles.ui.components.EmptyState
import com.archieapps.fles.ui.components.FolderListItem
import com.archieapps.fles.ui.components.IconButtonBox
import com.archieapps.fles.ui.components.MessageList
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.ui.model.Attachment
import com.archieapps.fles.ui.model.PendingAttachment
import com.archieapps.fles.ui.state.UiState
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.data.repository.AttachmentIo
import com.archieapps.fles.ui.viewmodel.ThreadUiState
import com.archieapps.fles.ui.viewmodel.ThreadActionState
import java.io.File

@Composable
fun MessagesScreen(state: UiState<List<Channel>>, openDrawer: () -> Unit, onThreadClick: (String) -> Unit) {
    val colors = FlesTheme.colors
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(0.5.dp, colors.border))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButtonBox(Icons.Filled.Menu, onClick = openDrawer)
        }
        when (state) {
            UiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            UiState.Empty -> EmptyState(title = "Sem conversas", description = null, icon = Icons.Filled.Message, modifier = Modifier.fillMaxSize())
            is UiState.Error -> EmptyState(title = "Erro ao carregar", description = state.message, icon = Icons.Filled.ErrorOutline, modifier = Modifier.fillMaxSize())
            is UiState.Success -> LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
                items(state.data, key = { it.id }) {
                    FolderListItem(channel = it, active = false, onClick = { onThreadClick(it.id) })
                }
            }
        }
    }
}

@Composable
fun ThreadScreen(
    state: UiState<ThreadUiState>,
    action: ThreadActionState,
    channels: List<Channel>,
    attachmentIo: AttachmentIo,
    keyConfigured: Boolean,
    openDrawer: () -> Unit,
    onBack: () -> Unit,
    onDeleteThread: () -> Unit,
    onSend: (String, List<PendingAttachment>) -> Unit,
    onCreateList: (String, List<String>) -> Unit,
    onConfigureKey: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onMoveMessage: (String, String) -> Unit,
    onToggleListItem: (String, String) -> Unit,
    onAddListItem: (String, String) -> Unit,
    onUpdateListItem: (String, String, String) -> Unit,
    onDeleteListItem: (String, String) -> Unit,
    onReorderListItems: (String, List<String>) -> Unit,
    onDownload: suspend (Attachment) -> File
) {
    val density = LocalDensity.current
    var composerHeight by remember { mutableStateOf(0.dp) }

    Box(modifier = Modifier.fillMaxSize().background(FlesTheme.colors.chatBg)) {
        when (state) {
            UiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FlesTheme.colors.primary)
            }
            UiState.Empty -> EmptyThreadScreen()
            is UiState.Error -> EmptyState(title = "Erro ao carregar", description = state.message, icon = Icons.Filled.ErrorOutline, modifier = Modifier.fillMaxSize())
            is UiState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    ChatHeader(channel = state.data.channel, itemCount = state.data.messages.size, onMenuClick = openDrawer, onBack = onBack, onDeleteClick = onDeleteThread)
                    Box(modifier = Modifier.weight(1f)) {
                        MessageList(
                            messages = state.data.messages,
                            folderId = state.data.channel.id,
                            channels = channels,
                            modifier = Modifier.fillMaxSize(),
                            bottomPadding = composerHeight + 16.dp,
                            onToggleFavorite = onToggleFavorite,
                            onDelete = onDeleteMessage,
                            onMove = onMoveMessage,
                            onToggleListItem = onToggleListItem,
                            onAddListItem = onAddListItem,
                            onUpdateListItem = onUpdateListItem,
                            onDeleteListItem = onDeleteListItem,
                            onReorderListItems = onReorderListItems,
                            onDownload = onDownload
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    Composer(
                        folderId = state.data.channel.id,
                        keyConfigured = keyConfigured,
                        attachmentIo = attachmentIo,
                        sending = action.sending,
                        onSend = onSend,
                        onCreateList = onCreateList,
                        onConfigureKey = onConfigureKey,
                        modifier = Modifier.onSizeChanged {
                            composerHeight = with(density) { it.height.toDp() }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyThreadScreen() {
    EmptyState(
        title = "Selecione uma conversa",
        description = "Escolha uma pasta na barra lateral para abrir.",
        icon = Icons.Filled.Message,
        modifier = Modifier.fillMaxSize()
    )
}
