package com.archieapps.fles.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.components.EmptyState
import com.archieapps.fles.ui.components.FileDetailsDialog
import com.archieapps.fles.ui.components.FileGridCard
import com.archieapps.fles.ui.components.FileListRow
import com.archieapps.fles.ui.components.FilesToolbar
import com.archieapps.fles.ui.components.FilesViewMode
import com.archieapps.fles.ui.components.FolderAvatar
import com.archieapps.fles.ui.components.IconButtonBox
import com.archieapps.fles.ui.components.folderIcon
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.ui.model.FileItem
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.viewmodel.FilesUiState
import java.io.File

@Composable
fun FilesScreen(
    state: FilesUiState,
    openDrawer: () -> Unit,
    onQueryChange: (String) -> Unit,
    onModeChange: (FilesViewMode) -> Unit,
    onFavoritesToggle: () -> Unit,
    onKindChange: (com.archieapps.fles.ui.model.FileKind?) -> Unit,
    onEnsureLocalFile: suspend (FileItem) -> File,
    onOpenFile: suspend (FileItem, File) -> Unit,
    favoritesOnlyInitial: Boolean = false
) {
    FilesScaffold(
        openDrawer = openDrawer,
        state = state,
        onQueryChange = onQueryChange,
        onModeChange = onModeChange,
        onFavoritesToggle = onFavoritesToggle,
        onKindChange = onKindChange,
        onEnsureLocalFile = onEnsureLocalFile,
        onOpenFile = onOpenFile,
        emptyTitle = when {
            favoritesOnlyInitial -> "Sem favoritos"
            state.folder != null -> "Sem arquivos nesta pasta"
            else -> "Sem arquivos por aqui"
        }
    )
}

@Composable
private fun FilesScaffold(
    openDrawer: () -> Unit,
    state: FilesUiState,
    onQueryChange: (String) -> Unit,
    onModeChange: (FilesViewMode) -> Unit,
    onFavoritesToggle: () -> Unit,
    onKindChange: (com.archieapps.fles.ui.model.FileKind?) -> Unit,
    onEnsureLocalFile: suspend (FileItem) -> File,
    onOpenFile: suspend (FileItem, File) -> Unit,
    emptyTitle: String
) {
    val colors = FlesTheme.colors
    var selectedFile by remember { mutableStateOf<FileItem?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        FilesHeader(openDrawer = openDrawer, folder = state.folder)
        FilesToolbar(
            query = state.query,
            onQueryChange = onQueryChange,
            mode = state.mode,
            onModeChange = onModeChange,
            favoritesOnly = state.favoritesOnly,
            onFavoritesToggle = onFavoritesToggle,
            activeKind = state.activeKind,
            onKindChange = onKindChange
        )
        if (state.loading) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else if (state.error != null) {
            EmptyState(title = "Erro ao carregar", description = state.error, icon = Icons.Filled.ErrorOutline, modifier = Modifier.fillMaxSize())
        } else if (state.files.isEmpty()) {
            EmptyState(title = emptyTitle, description = null, icon = Icons.Filled.FolderOpen, modifier = Modifier.fillMaxSize())
        } else if (state.mode == FilesViewMode.Grid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.files, key = { it.id }) {
                    FileGridCard(file = it, onClick = { selectedFile = it })
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.files, key = { it.id }) {
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
private fun FilesHeader(openDrawer: () -> Unit, folder: Channel?) {
    val colors = FlesTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .border(BorderStroke(0.5.dp, colors.border))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButtonBox(Icons.Filled.Menu, onClick = openDrawer)
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.FolderOpen, null, tint = colors.mutedForeground)
            Text("Arquivos", modifier = Modifier.padding(start = 6.dp), color = colors.mutedForeground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        if (folder != null) {
            Icon(Icons.Filled.ChevronRight, null, tint = colors.mutedForeground)
            Row(modifier = Modifier.padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                FolderAvatar(folder.name, folder.color, size = 32.dp, icon = folderIcon(folder.icon))
                Text(folder.name, modifier = Modifier.padding(start = 6.dp), color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
