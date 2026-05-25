package com.archieapps.fles.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.components.EmptyState
import com.archieapps.fles.ui.components.FileDetailsDialog
import com.archieapps.fles.ui.components.FileGridCard
import com.archieapps.fles.ui.components.FileListRow
import com.archieapps.fles.ui.components.FilesToolbar
import com.archieapps.fles.ui.components.FilesViewMode
import com.archieapps.fles.ui.components.IconButtonBox
import com.archieapps.fles.ui.components.MediaPagerOverlay
import com.archieapps.fles.ui.model.FileItem
import com.archieapps.fles.ui.model.FileKind
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.viewmodel.FilesUiState
import java.io.File

@Composable
fun GalleryScreen(
    state: FilesUiState,
    onOpenSettings: () -> Unit,
    onQueryChange: (String) -> Unit,
    onModeChange: (FilesViewMode) -> Unit,
    onFavoritesToggle: () -> Unit,
    onKindChange: (com.archieapps.fles.ui.model.FileKind?) -> Unit,
    onEnsureLocalFile: suspend (FileItem) -> File,
    onOpenFile: suspend (FileItem, File) -> Unit
) {
    val colors = FlesTheme.colors
    var selectedFile by remember { mutableStateOf<FileItem?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        GalleryHeader(onOpenSettings = onOpenSettings)
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
            EmptyState(title = "Sem mídias por aqui", description = null, icon = Icons.Filled.PhotoLibrary, modifier = Modifier.fillMaxSize())
        } else if (state.mode == FilesViewMode.Grid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.files, key = { it.id }) {
                    FileGridCard(file = it, onEnsureLocalFile = onEnsureLocalFile, onClick = { selectedFile = it })
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(state.files, key = { it.id }) {
                    FileListRow(file = it, onClick = { selectedFile = it })
                }
            }
        }
    }
    selectedFile?.let { file ->
        if (file.kind == FileKind.Image || file.kind == FileKind.Video) {
            val mediaList = remember(state.files) {
                state.files.filter { it.kind == FileKind.Image || it.kind == FileKind.Video }
            }
            val startIndex = remember(file.id, mediaList) {
                mediaList.indexOfFirst { it.id == file.id }.coerceAtLeast(0)
            }
            MediaPagerOverlay(
                files = mediaList,
                initialIndex = startIndex,
                onEnsureLocalFile = onEnsureLocalFile,
                onClose = { selectedFile = null }
            )
        } else {
            FileDetailsDialog(
                file = file,
                onClose = { selectedFile = null },
                onEnsureLocalFile = onEnsureLocalFile,
                onOpenFile = onOpenFile
            )
        }
    }
}

@Composable
private fun GalleryHeader(onOpenSettings: () -> Unit) {
    val colors = FlesTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .border(BorderStroke(0.5.dp, colors.border))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.PhotoLibrary, null, tint = colors.mutedForeground)
            Spacer(Modifier.width(6.dp))
            Text("Galeria", color = colors.mutedForeground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        IconButtonBox(Icons.Filled.Settings, onClick = onOpenSettings)
    }
}
