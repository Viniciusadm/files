package com.archieapps.fles.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.components.EmptyState
import com.archieapps.fles.ui.components.FileDetailsDialog
import com.archieapps.fles.ui.components.FileGridCard
import com.archieapps.fles.ui.components.FileListRow
import com.archieapps.fles.ui.components.FilesViewControls
import com.archieapps.fles.ui.components.FilesViewMode
import com.archieapps.fles.ui.components.FolderAvatar
import com.archieapps.fles.ui.components.FolderGridCard
import com.archieapps.fles.ui.components.IconButtonBox
import com.archieapps.fles.ui.components.SharedSearchBar
import com.archieapps.fles.ui.components.folderIcon
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.ui.model.FileItem
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.viewmodel.FilesUiState
import java.io.File

@Composable
fun FilesExplorerScreen(
    state: FilesUiState,
    allChannels: List<Channel>,
    onOpenSettings: () -> Unit,
    onBack: (() -> Unit)?,
    onOpenFolder: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onEnsureLocalFile: suspend (FileItem) -> File,
    onOpenFile: suspend (FileItem, File) -> Unit
) {
    val colors = FlesTheme.colors
    var selectedFile by remember { mutableStateOf<FileItem?>(null) }
    var mode by remember { mutableStateOf(FilesViewMode.List) }

    val currentFolderId = state.folder?.id
    val isRoot = currentFolderId == null
    val query = state.query.trim()
    val subfolders = remember(allChannels, currentFolderId, query) {
        allChannels
            .filter { it.parentId == currentFolderId }
            .filter { query.isEmpty() || it.name.contains(query, ignoreCase = true) }
            .sortedBy { it.name.lowercase() }
    }
    val files = if (isRoot) emptyList() else state.files

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        ExplorerHeader(
            folder = state.folder,
            onOpenSettings = onOpenSettings,
            onBack = onBack
        )
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            SharedSearchBar(
                query = state.query,
                onQueryChange = onQueryChange,
                placeholder = if (isRoot) "Buscar pasta..." else "Buscar nesta pasta..."
            )
        }
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp)) {
            FilesViewControls(mode = mode, onModeChange = { mode = it }, onSortClick = {})
        }
        if (state.loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else if (state.error != null) {
            EmptyState(title = "Erro ao carregar", description = state.error, icon = Icons.Filled.ErrorOutline, modifier = Modifier.fillMaxSize())
        } else if (subfolders.isEmpty() && files.isEmpty()) {
            EmptyState(
                title = if (isRoot) "Sem pastas por aqui" else "Pasta vazia",
                description = null,
                icon = Icons.Filled.FolderOpen,
                modifier = Modifier.fillMaxSize()
            )
        } else if (mode == FilesViewMode.Grid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                gridItems(subfolders, key = { "f-${it.id}" }, span = { GridItemSpan(1) }) { channel ->
                    FolderGridCard(channel = channel, onClick = { onOpenFolder(channel.id) })
                }
                gridItems(files, key = { "file-${it.id}" }, span = { GridItemSpan(1) }) {
                    FileGridCard(file = it, onEnsureLocalFile = onEnsureLocalFile, onClick = { selectedFile = it })
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(subfolders, key = { "f-${it.id}" }) { channel ->
                    FolderRow(channel = channel, onClick = { onOpenFolder(channel.id) })
                }
                items(files, key = { "file-${it.id}" }) {
                    FileListRow(file = it, onClick = { selectedFile = it })
                }
            }
        }
    }
    selectedFile?.let { file ->
        FileDetailsDialog(
            file = file,
            onClose = { selectedFile = null },
            onEnsureLocalFile = onEnsureLocalFile,
            onOpenFile = onOpenFile
        )
    }
}

@Composable
private fun ExplorerHeader(folder: Channel?, onOpenSettings: () -> Unit, onBack: (() -> Unit)?) {
    val colors = FlesTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .border(BorderStroke(0.5.dp, colors.border))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButtonBox(Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = onBack, iconSize = 22.dp)
            Spacer(Modifier.width(4.dp))
        }
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.FolderOpen, null, tint = colors.mutedForeground)
            Text("Arquivos", modifier = Modifier.padding(start = 6.dp), color = colors.mutedForeground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (folder != null) {
                Icon(Icons.Filled.ChevronRight, null, tint = colors.mutedForeground, modifier = Modifier.padding(start = 4.dp))
                FolderAvatar(folder.name, folder.color, size = 28.dp, icon = folderIcon(folder.icon))
                Text(
                    folder.name,
                    modifier = Modifier.padding(start = 6.dp),
                    color = colors.foreground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButtonBox(Icons.Filled.Settings, onClick = onOpenSettings)
    }
}

@Composable
private fun FolderRow(channel: Channel, onClick: () -> Unit) {
    val colors = FlesTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(FlesTheme.radius.lg))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FolderAvatar(channel.name, channel.color, size = 40.dp, icon = folderIcon(channel.icon))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                channel.name,
                color = colors.foreground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("Pasta", color = colors.mutedForeground, fontSize = 12.sp)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = colors.mutedForeground, modifier = Modifier.size(20.dp))
    }
}
