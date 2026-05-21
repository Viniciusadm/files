package com.archieapps.fles.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.sync.GlobalSyncState
import com.archieapps.fles.sync.SyncRuntimeState
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.theme.withAlpha
import kotlinx.coroutines.launch

enum class MainSection {
    Messages,
    Files,
    Favorites,
    Key,
    Search,
    Share
}

@Composable
fun AppShell(
    section: MainSection,
    activeFolderId: String?,
    channels: List<Channel>,
    onNavigateMessages: () -> Unit,
    onNavigateThread: (String) -> Unit,
    onNavigateFiles: () -> Unit,
    onNavigateFolderFiles: (String) -> Unit,
    onNavigateFavorites: () -> Unit,
    onNavigateKey: () -> Unit,
    onNavigateSearch: (String) -> Unit,
    onCreateChannel: (String, String, String) -> Unit,
    onLogout: () -> Unit,
    syncState: SyncRuntimeState = SyncRuntimeState(),
    content: @Composable (openDrawer: () -> Unit) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp).safeDrawingPadding(),
                drawerContainerColor = FlesTheme.colors.sidebar
            ) {
                Sidebar(
                    section = section,
                    activeFolderId = activeFolderId,
                    channels = channels,
                    onNavigateMessages = {
                        scope.launch { drawerState.close() }
                        onNavigateMessages()
                    },
                    onNavigateThread = {
                        scope.launch { drawerState.close() }
                        onNavigateThread(it)
                    },
                    onNavigateFiles = {
                        scope.launch { drawerState.close() }
                        onNavigateFiles()
                    },
                    onNavigateFolderFiles = {
                        scope.launch { drawerState.close() }
                        onNavigateFolderFiles(it)
                    },
                    onNavigateFavorites = {
                        scope.launch { drawerState.close() }
                        onNavigateFavorites()
                    },
                    onNavigateKey = {
                        scope.launch { drawerState.close() }
                        onNavigateKey()
                    },
                    onNavigateSearch = {
                        scope.launch { drawerState.close() }
                        onNavigateSearch(it)
                    },
                    onCreateChannel = onCreateChannel,
                    onLogout = onLogout
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(FlesTheme.colors.background)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SyncStatusBanner(syncState)
                    Box(modifier = Modifier.weight(1f)) {
                        content { scope.launch { drawerState.open() } }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusBanner(syncState: SyncRuntimeState) {
    val colors = FlesTheme.colors
    val text = when {
        !syncState.isOnline || syncState.global == GlobalSyncState.Offline ->
            if (syncState.pendingOps > 0) "Offline · ${syncState.pendingOps} aguardando sincronização" else "Sem conexão"
        syncState.global == GlobalSyncState.WaitingKey -> "Configure sua chave E2EE para sincronizar"
        syncState.global == GlobalSyncState.Syncing -> "Sincronizando..."
        syncState.global == GlobalSyncState.Failed -> "Erro de sincronização"
        syncState.pendingOps > 0 -> "${syncState.pendingOps} aguardando sincronização"
        else -> null
    } ?: return
    val bg = when (syncState.global) {
        GlobalSyncState.WaitingKey,
        GlobalSyncState.Failed -> colors.destructive
        GlobalSyncState.Syncing -> colors.primary
        else -> colors.favorite
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(bg).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Sidebar(
    section: MainSection,
    activeFolderId: String?,
    channels: List<Channel>,
    onNavigateMessages: () -> Unit,
    onNavigateThread: (String) -> Unit,
    onNavigateFiles: () -> Unit,
    onNavigateFolderFiles: (String) -> Unit,
    onNavigateFavorites: () -> Unit,
    onNavigateKey: () -> Unit,
    onNavigateSearch: (String) -> Unit,
    onCreateChannel: (String, String, String) -> Unit,
    onLogout: () -> Unit
) {
    val colors = FlesTheme.colors
    var createOpen by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxHeight().background(colors.sidebar)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Message, null, modifier = Modifier.size(20.dp), tint = colors.primaryForeground)
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Meu Drive Chat", color = colors.sidebarForeground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("usuario@exemplo.com", color = colors.mutedForeground, fontSize = 12.sp, maxLines = 1)
            }
            IconButtonBox(Icons.Filled.Key, onClick = onNavigateKey, tint = colors.sidebarForeground)
            IconButtonBox(Icons.Filled.Add, onClick = { createOpen = true }, tint = colors.sidebarForeground)
        }
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            var query = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
            AppInput(
                value = query.value,
                onValueChange = {
                    query.value = it
                    if (it.trim().length >= 2) onNavigateSearch(it.trim())
                },
                placeholder = "Buscar",
                leadingIcon = Icons.Filled.Search,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(FlesTheme.radius.lg))
                .background(colors.background)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NavChip("Conversas", Icons.Filled.ChatBubbleOutline, section == MainSection.Messages, onNavigateMessages, Modifier.weight(1f))
            NavChip("Arquivos", Icons.Filled.FolderOpen, section == MainSection.Files, onNavigateFiles, Modifier.weight(1f))
            NavChip("Favoritos", Icons.Filled.Star, section == MainSection.Favorites, onNavigateFavorites, Modifier.weight(1f))
        }
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                FolderListItem(
                    channel = channel,
                    active = activeFolderId == channel.id,
                    onClick = {
                        if (section == MainSection.Files || section == MainSection.Favorites) {
                            onNavigateFolderFiles(channel.id)
                        } else {
                            onNavigateThread(channel.id)
                        }
                    }
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(FlesTheme.radius.lg),
            color = Color.Transparent,
            border = BorderStroke(0.dp, Color.Transparent)
        ) {
            Row(
                modifier = Modifier.clickable(onClick = onLogout).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("U", color = colors.primaryForeground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("usuario", color = colors.sidebarForeground, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("usuario@exemplo.com", color = colors.mutedForeground, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Filled.Logout, null, modifier = Modifier.size(18.dp), tint = colors.mutedForeground)
            }
        }
        if (createOpen) {
            CreateChannelDialog(
                onClose = { createOpen = false },
                onCreate = { name, icon, color ->
                    onCreateChannel(name, icon, color)
                    createOpen = false
                }
            )
        }
    }
}

@Composable
private fun CreateChannelDialog(onClose: () -> Unit, onCreate: (String, String, String) -> Unit) {
    val colors = FlesTheme.colors
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("Folder") }
    var color by remember { mutableStateOf("#3390ec") }
    val icons = listOf("Folder", "Inbox", "Briefcase", "GraduationCap", "Image", "Star")
    val colorsList = listOf("#3390ec", "#22c55e", "#eab308", "#ef4444", "#8b5cf6", "#06b6d4")
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Nova pasta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppInput(name, { name = it }, "Ex.: Receitas")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    icons.forEach { item ->
                        IconButtonBox(
                            icon = folderIcon(item),
                            onClick = { icon = item },
                            background = if (icon == item) colors.primary.withAlpha(0.12f) else Color.Transparent,
                            tint = if (icon == item) colors.primary else colors.mutedForeground
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorsList.forEach { item ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(parseHexColor(item))
                                .border(BorderStroke(if (color == item) 3.dp else 0.dp, colors.foreground), androidx.compose.foundation.shape.CircleShape)
                                .clickable { color = item }
                        )
                    }
                }
            }
        },
        confirmButton = { AppButton("Criar pasta", enabled = name.trim().isNotBlank(), onClick = { onCreate(name, icon, color) }) },
        dismissButton = { AppButton("Cancelar", variant = AppButtonVariant.Ghost, onClick = onClose) }
    )
}

@Composable
private fun NavChip(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FlesTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(FlesTheme.radius.md))
            .background(if (active) colors.sidebarAccent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = if (active) colors.sidebarForeground else colors.mutedForeground)
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (active) colors.sidebarForeground else colors.mutedForeground, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

private fun parseHexColor(value: String): Color {
    return runCatching { Color(("FF" + value.removePrefix("#")).toLong(16)) }.getOrDefault(Color(0xFF3390EC))
}
