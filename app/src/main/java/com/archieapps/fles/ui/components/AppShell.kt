package com.archieapps.fles.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.archieapps.fles.ui.theme.FlesTheme
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
    onLogout: () -> Unit,
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
                content { scope.launch { drawerState.open() } }
            }
        }
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
    onLogout: () -> Unit
) {
    val colors = FlesTheme.colors
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
            IconButtonBox(Icons.Filled.Add, onClick = {}, tint = colors.sidebarForeground)
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
    }
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
