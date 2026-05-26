package com.archieapps.fles.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.archieapps.fles.ui.model.FileKind
import com.archieapps.fles.ui.model.FileItem
import com.archieapps.fles.ui.model.SyncStatus
import com.archieapps.fles.ui.viewmodel.GalleryGroupMode
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.theme.withAlpha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class FilesViewMode {
    Grid,
    List
}

data class FileKindStyle(
    val icon: ImageVector,
    val label: String,
    val bg: Color,
    val fg: Color
)

fun kindStyle(kind: FileKind): FileKindStyle {
    return when (kind) {
        FileKind.Image -> FileKindStyle(Icons.Filled.Image, "Imagem", Color(0xFFFCE7F3), Color(0xFFDB2777))
        FileKind.Video -> FileKindStyle(Icons.Filled.Videocam, "Vídeo", Color(0xFFEDE9FE), Color(0xFF7C3AED))
        FileKind.Audio -> FileKindStyle(Icons.Filled.AudioFile, "Áudio", Color(0xFFD1FAE5), Color(0xFF059669))
        FileKind.Pdf -> FileKindStyle(Icons.Filled.PictureAsPdf, "PDF", Color(0xFFFEE2E2), Color(0xFFDC2626))
        FileKind.Archive -> FileKindStyle(Icons.Filled.Archive, "Compactado", Color(0xFFFEF3C7), Color(0xFFD97706))
        FileKind.Spreadsheet -> FileKindStyle(Icons.Filled.TableChart, "Planilha", Color(0xFFD1FAE5), Color(0xFF059669))
        FileKind.Presentation -> FileKindStyle(Icons.Filled.Slideshow, "Apresentação", Color(0xFFFFEDD5), Color(0xFFEA580C))
        FileKind.Document -> FileKindStyle(Icons.Filled.Description, "Documento", Color(0xFFDBEAFE), Color(0xFF2563EB))
        FileKind.Code -> FileKindStyle(Icons.Filled.Code, "Código", Color(0xFFE0F2FE), Color(0xFF0284C7))
        FileKind.Text -> FileKindStyle(Icons.Filled.Description, "Texto", Color(0xFFF1F5F9), Color(0xFF475569))
        FileKind.Link -> FileKindStyle(Icons.Filled.Link, "Link", Color(0xFFE0E7FF), Color(0xFF4F46E5))
        FileKind.Other -> FileKindStyle(Icons.Filled.InsertDriveFile, "Arquivo", Color(0xFFF1F5F9), Color(0xFF64748B))
    }
}

@Composable
fun FileIconBadge(kind: FileKind, modifier: Modifier = Modifier, large: Boolean = false) {
    val style = kindStyle(kind)
    Box(
        modifier = modifier
            .size(if (large) 56.dp else 36.dp)
            .clip(RoundedCornerShape(FlesTheme.radius.xl))
            .background(style.bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(style.icon, null, modifier = Modifier.size(if (large) 28.dp else 16.dp), tint = style.fg)
    }
}

@Composable
fun FilesToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    mode: FilesViewMode,
    onModeChange: (FilesViewMode) -> Unit,
    favoritesOnly: Boolean,
    onFavoritesToggle: () -> Unit,
    activeKind: FileKind?,
    onKindChange: (FileKind?) -> Unit,
    galleryGroupMode: GalleryGroupMode,
    onGroupModeChange: (GalleryGroupMode) -> Unit,
    showGroupToggle: Boolean,
) {
    val colors = FlesTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .border(BorderStroke(0.5.dp, colors.border))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SharedSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = "Buscar arquivos por nome, tipo ou pasta..."
        )
        FilesViewControls(mode = mode, onModeChange = onModeChange, onSortClick = {})
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(FlesTheme.radius.full),
                    color = if (favoritesOnly) Color(0xFFEAB308).withAlpha(0.1f) else Color.Transparent,
                    border = BorderStroke(1.dp, if (favoritesOnly) Color(0x66EAB308) else colors.border),
                    modifier = Modifier.clickable(onClick = onFavoritesToggle)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, modifier = Modifier.size(12.dp), tint = if (favoritesOnly) Color(0xFFA16207) else colors.mutedForeground)
                        Spacer(Modifier.width(6.dp))
                        Text("Favoritos", color = if (favoritesOnly) Color(0xFFA16207) else colors.mutedForeground, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                listOf(FileKind.Image, FileKind.Video).forEach { kind ->
                    val label = kindStyle(kind).label + "s"
                    Badge(
                        label,
                        active = activeKind == kind,
                        modifier = Modifier.clickable {
                            onKindChange(if (activeKind == kind) null else kind)
                        }
                    )
                }
            }
            if (showGroupToggle) {
                GalleryModeToggle(mode = galleryGroupMode, onChange = onGroupModeChange)
            }
        }
    }
}

@Composable
fun FilesViewControls(
    mode: FilesViewMode,
    onModeChange: (FilesViewMode) -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FlesTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppButton("Ordenar", onClick = onSortClick, variant = AppButtonVariant.Outline, leadingIcon = Icons.Filled.Sort)
        Row(
            modifier = Modifier
                .border(1.dp, colors.border, RoundedCornerShape(FlesTheme.radius.lg))
                .padding(2.dp)
        ) {
            IconButtonBox(
                icon = Icons.Filled.GridView,
                onClick = { onModeChange(FilesViewMode.Grid) },
                background = if (mode == FilesViewMode.Grid) colors.secondary else Color.Transparent,
                size = 32.dp,
                iconSize = 16.dp
            )
            IconButtonBox(
                icon = Icons.Filled.ViewList,
                onClick = { onModeChange(FilesViewMode.List) },
                background = if (mode == FilesViewMode.List) colors.secondary else Color.Transparent,
                size = 32.dp,
                iconSize = 16.dp
            )
        }
    }
}

@Composable
fun FileGridCard(
    file: FileItem,
    modifier: Modifier = Modifier,
    onEnsureLocalFile: (suspend (FileItem) -> File)? = null,
    onClick: () -> Unit = {}
) {
    val colors = FlesTheme.colors
    val style = kindStyle(file.kind)
    val previewBitmap = rememberGridPreviewBitmap(file, onEnsureLocalFile)
    val videoLocalPath = if (file.kind == FileKind.Video) {
        file.localPath?.removePrefix("file://")?.let { File(it).takeIf { f -> f.exists() }?.absolutePath }
    } else null
    val videoMeta = if (file.kind == FileKind.Video) rememberVideoMeta(videoLocalPath) else null
    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(FlesTheme.radius.x2l))
            .clip(RoundedCornerShape(FlesTheme.radius.x2l))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(FlesTheme.radius.x2l))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .background(file.previewColor ?: colors.muted)
        ) {
            val videoThumb = videoMeta?.thumbnail
            when {
                videoThumb != null -> {
                    Image(
                        bitmap = videoThumb,
                        contentDescription = file.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.withAlpha(0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(26.dp), tint = Color.White)
                    }
                }
                previewBitmap != null -> {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = file.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                file.kind == FileKind.Video -> {
                    FileIconBadge(file.kind, large = true, modifier = Modifier.align(Alignment.Center))
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.withAlpha(0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(24.dp), tint = Color.White)
                    }
                }
                else -> {
                    FileIconBadge(file.kind, large = true, modifier = Modifier.align(Alignment.Center))
                }
            }
            val durationLabel = videoMeta?.durationMs?.let { formatVideoDuration(it) }
            if (!durationLabel.isNullOrEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(FlesTheme.radius.sm),
                    color = Color.Black.withAlpha(0.65f)
                ) {
                    Text(durationLabel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(FlesTheme.radius.md),
                color = colors.background.withAlpha(0.9f)
            ) {
                Text(style.label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = colors.foreground, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (file.isFavorite) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(colors.background.withAlpha(0.9f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Star, null, modifier = Modifier.size(14.dp), tint = colors.favorite)
                    }
                }
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(colors.background.withAlpha(0.9f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MoreVert, null, modifier = Modifier.size(14.dp), tint = colors.foreground)
                }
            }
        }
        Text(
            file.name,
            modifier = Modifier.padding(10.dp),
            color = colors.foreground,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

data class GalleryFolderGroup(
    val folderId: String,
    val folderName: String,
    val count: Int,
    val cover: FileItem
)

@Composable
fun GalleryModeToggle(
    mode: GalleryGroupMode,
    onChange: (GalleryGroupMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FlesTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(FlesTheme.radius.full))
            .background(colors.muted)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GalleryModeSegment(
            label = "Pastas",
            icon = Icons.Filled.Folder,
            active = mode == GalleryGroupMode.Folders,
            onClick = { onChange(GalleryGroupMode.Folders) }
        )
        GalleryModeSegment(
            label = "Todos",
            icon = Icons.Filled.GridView,
            active = mode == GalleryGroupMode.All,
            onClick = { onChange(GalleryGroupMode.All) }
        )
    }
}

@Composable
private fun GalleryModeSegment(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    val colors = FlesTheme.colors
    val bg = if (active) colors.background else Color.Transparent
    val fg = if (active) colors.foreground else colors.mutedForeground
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(FlesTheme.radius.full))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = fg)
        Text(
            label,
            color = fg,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
fun GalleryFolderCard(
    group: GalleryFolderGroup,
    modifier: Modifier = Modifier,
    onEnsureLocalFile: (suspend (FileItem) -> File)? = null,
    onClick: () -> Unit = {}
) {
    val colors = FlesTheme.colors
    val cover = group.cover
    val previewBitmap = rememberGridPreviewBitmap(cover, onEnsureLocalFile)
    val videoLocalPath = if (cover.kind == FileKind.Video) {
        cover.localPath?.removePrefix("file://")?.let { File(it).takeIf { f -> f.exists() }?.absolutePath }
    } else null
    val videoMeta = if (cover.kind == FileKind.Video) rememberVideoMeta(videoLocalPath) else null
    Box(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(FlesTheme.radius.x2l))
            .clip(RoundedCornerShape(FlesTheme.radius.x2l))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(FlesTheme.radius.x2l))
            .clickable(onClick = onClick)
            .aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cover.previewColor ?: colors.muted)
        ) {
            val videoThumb = videoMeta?.thumbnail
            when {
                videoThumb != null -> Image(
                    bitmap = videoThumb,
                    contentDescription = group.folderName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                previewBitmap != null -> Image(
                    bitmap = previewBitmap,
                    contentDescription = group.folderName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                else -> FileIconBadge(cover.kind, large = true, modifier = Modifier.align(Alignment.Center))
            }
        }
        if (cover.kind == FileKind.Video) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.withAlpha(0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp), tint = Color.White)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.withAlpha(0.7f))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    group.folderName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (group.count == 1) "1 item" else "${group.count} itens",
                    color = Color.White.withAlpha(0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun FileListRow(file: FileItem, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val colors = FlesTheme.colors
    val videoLocalPath = if (file.kind == FileKind.Video) {
        file.localPath?.removePrefix("file://")?.let { File(it).takeIf { f -> f.exists() }?.absolutePath }
    } else null
    val videoMeta = if (file.kind == FileKind.Video) rememberVideoMeta(videoLocalPath) else null
    val imageThumb = if (file.kind == FileKind.Image) rememberGridPreviewBitmap(file) else null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(BorderStroke(0.5.dp, colors.border))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val videoThumb = videoMeta?.thumbnail
        when {
            videoThumb != null -> Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(FlesTheme.radius.md))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Image(bitmap = videoThumb, contentDescription = file.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.withAlpha(0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(14.dp), tint = Color.White)
                }
            }
            imageThumb != null -> Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(FlesTheme.radius.md))
                    .background(colors.muted)
            ) {
                Image(bitmap = imageThumb, contentDescription = file.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            else -> FileIconBadge(file.kind)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(file.name, color = colors.foreground, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${formatBytes(file.size)} · ${formatDate(file.createdAt)}", color = colors.mutedForeground, fontSize = 12.sp, maxLines = 1)
        }
        if (file.isFavorite) {
            Icon(Icons.Filled.Star, null, modifier = Modifier.size(14.dp), tint = colors.favorite)
            Spacer(Modifier.width(12.dp))
        }
        Icon(Icons.Filled.MoreVert, null, modifier = Modifier.size(16.dp), tint = colors.foreground)
    }
}

@Composable
fun FileDetailsDialog(
    file: FileItem,
    onClose: () -> Unit,
    onEnsureLocalFile: suspend (FileItem) -> File,
    onOpenFile: suspend (FileItem, File) -> Unit
) {
    val colors = FlesTheme.colors
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val previewable = file.isImagePreviewable() || file.isTextPreviewable()
    var localFile by remember(file.id, file.localPath) { mutableStateOf(file.existingLocalFile()) }
    var previewLoading by remember(file.id) { mutableStateOf(false) }
    var previewError by remember(file.id) { mutableStateOf<String?>(null) }
    var previewText by remember(file.id) { mutableStateOf<String?>(null) }
    var opening by remember(file.id) { mutableStateOf(false) }
    var openError by remember(file.id) { mutableStateOf<String?>(null) }
    var videoPlayerOpen by remember(file.id) { mutableStateOf(false) }

    LaunchedEffect(file.id, file.localPath) {
        if (!previewable) return@LaunchedEffect
        previewLoading = true
        previewError = null
        previewText = null
        val result = runCatching {
            val loaded = localFile ?: onEnsureLocalFile(file)
            loaded to if (file.isTextPreviewable()) loaded.readTextPreview() else null
        }
        result.onSuccess { (loaded, text) ->
            localFile = loaded
            previewText = text
        }.onFailure {
            previewError = it.message ?: "Falha ao carregar preview"
        }
        previewLoading = false
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 560.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(FlesTheme.radius.x2l),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 720.dp)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FileIconBadge(file.kind)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(file.name, color = colors.foreground, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(kindStyle(file.kind).label, color = colors.mutedForeground, fontSize = 12.sp)
                    }
                    IconButtonBox(Icons.Filled.Close, onClick = onClose, size = 34.dp, iconSize = 18.dp)
                }

                FilePreviewPanel(
                    file = file,
                    localFile = localFile,
                    previewText = previewText,
                    loading = previewLoading,
                    error = previewError,
                    onPlayVideo = if (file.kind == FileKind.Video) {
                        {
                            if (localFile != null) {
                                videoPlayerOpen = true
                            } else if (!opening) {
                                opening = true
                                openError = null
                                scope.launch {
                                    runCatching { onEnsureLocalFile(file) }
                                        .onSuccess {
                                            localFile = it
                                            videoPlayerOpen = true
                                        }
                                        .onFailure { openError = it.message ?: "Falha ao carregar vídeo" }
                                    opening = false
                                }
                            }
                        }
                    } else null
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FileDetailRow("Tamanho", formatBytes(file.size))
                    FileDetailRow("Pasta", file.folderName)
                    FileDetailRow("Data", formatDate(file.createdAt))
                    FileDetailRow("Formato", file.mimeType.ifBlank { "application/octet-stream" })
                    FileDetailRow("Estado", file.syncStatus.label())
                    file.lastError?.takeIf { it.isNotBlank() }?.let { FileDetailRow("Erro", it, error = true) }
                    openError?.let { FileDetailRow("Abertura", it, error = true) }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton(
                        text = "Fechar",
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                        variant = AppButtonVariant.Outline
                    )
                    AppButton(
                        text = "Baixar",
                        onClick = {
                            if (opening) return@AppButton
                            opening = true
                            openError = null
                            scope.launch {
                                runCatching {
                                    val loaded = localFile ?: onEnsureLocalFile(file)
                                    localFile = loaded
                                    onOpenFile(file, loaded)
                                }.onFailure {
                                    openError = it.message ?: "Falha ao abrir arquivo"
                                }
                                opening = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        loading = opening,
                        leadingIcon = Icons.Filled.Download
                    )
                }
            }
        }
    }

    if (videoPlayerOpen) {
        val playableFile = localFile
        if (playableFile != null) {
            VideoPlayerOverlay(
                title = file.name,
                uri = android.net.Uri.fromFile(playableFile).toString(),
                onClose = { videoPlayerOpen = false }
            )
        }
    }
}

@Composable
private fun FilePreviewPanel(
    file: FileItem,
    localFile: File?,
    previewText: String?,
    loading: Boolean,
    error: String?,
    onPlayVideo: (() -> Unit)? = null
) {
    val colors = FlesTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 320.dp)
            .clip(RoundedCornerShape(FlesTheme.radius.xl))
            .background(colors.muted.withAlpha(0.6f))
            .border(BorderStroke(0.5.dp, colors.border), RoundedCornerShape(FlesTheme.radius.xl))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            loading -> CircularProgressIndicator(color = colors.primary)
            error != null -> Text(error, color = colors.destructive, fontSize = 13.sp, lineHeight = 18.sp)
            file.kind == FileKind.Video -> {
                val meta = rememberVideoMeta(localFile?.absolutePath)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(FlesTheme.radius.lg))
                        .background(Color.Black)
                        .clickable(enabled = onPlayVideo != null) { onPlayVideo?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    val thumb = meta.thumbnail
                    if (thumb != null) {
                        Image(bitmap = thumb, contentDescription = file.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black.withAlpha(0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(36.dp), tint = Color.White)
                    }
                    val durationLabel = formatVideoDuration(meta.durationMs)
                    if (durationLabel.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            shape = RoundedCornerShape(FlesTheme.radius.sm),
                            color = Color.Black.withAlpha(0.65f)
                        ) {
                            Text(durationLabel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            file.isImagePreviewable() -> {
                val bitmap = remember(localFile?.absolutePath) {
                    localFile?.absolutePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
                }
                if (bitmap != null) {
                    Image(bitmap = bitmap, contentDescription = file.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } else {
                    Text("Preview indisponível", color = colors.mutedForeground, fontSize = 13.sp)
                }
            }
            file.isTextPreviewable() -> {
                Text(
                    previewText ?: "Preview indisponível",
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.foreground,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    maxLines = 12,
                    overflow = TextOverflow.Ellipsis
                )
            }
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FileIconBadge(file.kind, large = true)
                Text("Preview não disponível.", color = colors.mutedForeground, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun FileDetailRow(label: String, value: String, error: Boolean = false) {
    val colors = FlesTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.width(78.dp), color = colors.mutedForeground, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(
            value,
            modifier = Modifier.weight(1f),
            color = if (error) colors.destructive else colors.foreground,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "—"
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024f)
    if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024f * 1024f))
    return String.format("%.1f GB", bytes / (1024f * 1024f * 1024f))
}

fun formatDate(iso: String): String {
    return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4)
}

private fun FileItem.existingLocalFile(): File? {
    if (isSecure) return null
    val path = localPath?.removePrefix("file://") ?: return null
    return File(path).takeIf { it.exists() }
}

@Composable
private fun rememberGridPreviewBitmap(file: FileItem, onEnsureLocalFile: (suspend (FileItem) -> File)? = null): ImageBitmap? {
    val source = file.previewLocalPath ?: file.localPath
    var bitmap by remember(file.id, source, file.isSecure) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(file.id, source) {
        bitmap = null
        if (!file.isImagePreviewable()) return@LaunchedEffect
        bitmap = runCatching {
            val localFile = if (file.isSecure) {
                onEnsureLocalFile?.invoke(file)
            } else {
                source?.removePrefix("file://")?.let { File(it).takeIf { candidate -> candidate.exists() } }
            } ?: return@LaunchedEffect
            withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(localFile.absolutePath)?.asImageBitmap()
            }
        }.getOrNull()
    }
    return bitmap
}

private fun FileItem.isImagePreviewable(): Boolean {
    return kind == FileKind.Image || mimeType.startsWith("image/")
}

private fun FileItem.isTextPreviewable(): Boolean {
    val ext = extension.lowercase()
    return kind == FileKind.Text ||
        kind == FileKind.Code ||
        mimeType.startsWith("text/") ||
        ext in setOf("txt", "md", "json", "xml", "csv", "log", "kt", "java", "js", "ts", "tsx", "jsx", "css", "html")
}

private suspend fun File.readTextPreview(): String = withContext(Dispatchers.IO) {
    inputStream().use { input ->
        val bytes = ByteArray(12_000)
        val count = input.read(bytes).coerceAtLeast(0)
        val text = bytes.decodeToString(0, count).replace("\u0000", "")
        if (length() > count) "$text\n..." else text
    }.ifBlank { "Arquivo de texto vazio." }
}

private fun SyncStatus.label(): String {
    return when (this) {
        SyncStatus.Synced -> "Disponível"
        SyncStatus.PendingCreate -> "Aguardando criação"
        SyncStatus.PendingUpdate -> "Aguardando atualização"
        SyncStatus.PendingDelete -> "Aguardando exclusão"
        SyncStatus.PendingUpload -> "Aguardando envio"
        SyncStatus.Uploading -> "Enviando"
        SyncStatus.Downloading -> "Baixando"
        SyncStatus.Failed -> "Falhou"
    }
}
