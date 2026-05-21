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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.components.EmptyState
import com.archieapps.fles.ui.components.FileGridCard
import com.archieapps.fles.ui.components.FileListRow
import com.archieapps.fles.ui.components.FilesToolbar
import com.archieapps.fles.ui.components.FilesViewMode
import com.archieapps.fles.ui.components.FolderAvatar
import com.archieapps.fles.ui.components.IconButtonBox
import com.archieapps.fles.ui.components.folderIcon
import com.archieapps.fles.ui.model.FileKind
import com.archieapps.fles.ui.model.MockChannel
import com.archieapps.fles.ui.model.MockData
import com.archieapps.fles.ui.model.MockFile
import com.archieapps.fles.ui.theme.FlesTheme

@Composable
fun FilesScreen(openDrawer: () -> Unit, folderId: String? = null, favoritesOnlyInitial: Boolean = false) {
    val folder = folderId?.let { id -> MockData.channels.firstOrNull { it.id == id } }
    val baseFiles = MockData.files.filter { file ->
        (folder == null || file.folderId == folder.id)
    }
    FilesScaffold(
        openDrawer = openDrawer,
        folder = folder,
        files = baseFiles,
        favoritesOnlyInitial = favoritesOnlyInitial,
        emptyTitle = when {
            favoritesOnlyInitial -> "Sem favoritos"
            folder != null -> "Sem arquivos nesta pasta"
            else -> "Sem arquivos por aqui"
        }
    )
}

@Composable
private fun FilesScaffold(
    openDrawer: () -> Unit,
    folder: MockChannel?,
    files: List<MockFile>,
    favoritesOnlyInitial: Boolean,
    emptyTitle: String
) {
    val colors = FlesTheme.colors
    var query by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(FilesViewMode.Grid) }
    var favoritesOnly by remember { mutableStateOf(favoritesOnlyInitial) }
    var activeKind by remember { mutableStateOf<FileKind?>(null) }
    val filtered = files
        .filter { !favoritesOnly || it.isFavorite }
        .filter { activeKind == null || it.kind == activeKind }
        .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) || it.folderName.contains(query, ignoreCase = true) }
        .sortedByDescending { it.createdAt }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        FilesHeader(openDrawer = openDrawer, folder = folder)
        FilesToolbar(
            query = query,
            onQueryChange = { query = it },
            mode = mode,
            onModeChange = { mode = it },
            favoritesOnly = favoritesOnly,
            onFavoritesToggle = { favoritesOnly = !favoritesOnly },
            activeKind = activeKind,
            onKindChange = { activeKind = it }
        )
        if (filtered.isEmpty()) {
            EmptyState(title = emptyTitle, description = null, icon = Icons.Filled.FolderOpen, modifier = Modifier.fillMaxSize())
        } else if (mode == FilesViewMode.Grid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) {
                    FileGridCard(file = it)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.id }) {
                    FileListRow(file = it)
                }
            }
        }
    }
}

@Composable
private fun FilesHeader(openDrawer: () -> Unit, folder: MockChannel?) {
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
