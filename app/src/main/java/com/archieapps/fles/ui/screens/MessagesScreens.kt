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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Message
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
import com.archieapps.fles.ui.model.MockData
import com.archieapps.fles.ui.theme.FlesTheme

@Composable
fun MessagesScreen(openDrawer: () -> Unit, onThreadClick: (String) -> Unit) {
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
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
            items(MockData.channels, key = { it.id }) {
                FolderListItem(channel = it, active = false, onClick = { onThreadClick(it.id) })
            }
        }
    }
}

@Composable
fun ThreadScreen(folderId: String, openDrawer: () -> Unit, onBack: () -> Unit) {
    val channel = MockData.channels.firstOrNull { it.id == folderId } ?: MockData.channels.first()
    val messages = MockData.messages.filter { it.folderId == channel.id }
    val density = LocalDensity.current
    var composerHeight by remember { mutableStateOf(0.dp) }

    Box(modifier = Modifier.fillMaxSize().background(FlesTheme.colors.chatBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatHeader(channel = channel, itemCount = messages.size, onMenuClick = openDrawer, onBack = onBack)
            Box(modifier = Modifier.weight(1f)) {
                MessageList(
                    messages = messages,
                    modifier = Modifier.fillMaxSize(),
                    bottomPadding = composerHeight + 16.dp
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
                modifier = Modifier.onSizeChanged {
                    composerHeight = with(density) { it.height.toDp() }
                }
            )
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
