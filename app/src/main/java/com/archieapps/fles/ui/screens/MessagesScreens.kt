package com.archieapps.fles.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.components.ChatHeader
import com.archieapps.fles.ui.components.Composer
import com.archieapps.fles.ui.components.CreateChannelDialog
import com.archieapps.fles.ui.components.EmptyState
import com.archieapps.fles.ui.components.FolderListItem
import com.archieapps.fles.ui.components.IconButtonBox
import com.archieapps.fles.ui.components.MessageList
import com.archieapps.fles.ui.components.flattenChannelTree
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(0.5.dp, colors.border))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButtonBox(Icons.Filled.Menu, onClick = openDrawer)
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Message, null, tint = colors.mutedForeground)
                Spacer(Modifier.width(6.dp))
                Text("Conversas", color = colors.mutedForeground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        when (state) {
            UiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            UiState.Empty -> EmptyState(title = "Sem conversas", description = null, icon = Icons.Filled.Message, modifier = Modifier.fillMaxSize())
            is UiState.Error -> EmptyState(title = "Erro ao carregar", description = state.message, icon = Icons.Filled.ErrorOutline, modifier = Modifier.fillMaxSize())
            is UiState.Success -> LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
                items(flattenChannelTree(state.data), key = { it.channel.id }) {
                    FolderListItem(channel = it.channel, active = false, depth = it.depth, onClick = { onThreadClick(it.channel.id) })
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
    onCreateSubfolder: (String, String, String, Boolean) -> Unit,
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
    var createSubfolderOpen by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize().background(FlesTheme.colors.chatBg)) {
        when (state) {
            UiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FlesTheme.colors.primary)
            }
            UiState.Empty -> EmptyThreadScreen()
            is UiState.Error -> EmptyState(title = "Erro ao carregar", description = state.message, icon = Icons.Filled.ErrorOutline, modifier = Modifier.fillMaxSize())
            is UiState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    ChatHeader(
                        channel = state.data.channel,
                        itemCount = state.data.messages.size,
                        onMenuClick = openDrawer,
                        onBack = onBack,
                        onCreateSubfolderClick = { createSubfolderOpen = true },
                        onDeleteClick = onDeleteThread
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        MessageList(
                            messages = state.data.messages,
                            folderId = state.data.channel.id,
                            channels = channels,
                            modifier = Modifier.fillMaxSize(),
                            bottomPadding = 16.dp,
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
                    Composer(
                        folderId = state.data.channel.id,
                        keyConfigured = keyConfigured,
                        attachmentIo = attachmentIo,
                        sending = action.sending,
                        onSend = onSend,
                        onCreateList = onCreateList,
                        onConfigureKey = onConfigureKey,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                    )
                }
                if (createSubfolderOpen) {
                    CreateChannelDialog(
                        secureEnabled = false,
                        title = "Nova subpasta",
                        confirmLabel = "Criar subpasta",
                        secureOptionVisible = false,
                        initialSecure = state.data.channel.isSecure,
                        onClose = { createSubfolderOpen = false },
                        onCreate = { name, icon, color, isSecure ->
                            onCreateSubfolder(name, icon, color, isSecure)
                            createSubfolderOpen = false
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
