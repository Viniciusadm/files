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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.model.FileKind
import com.archieapps.fles.ui.model.FileItem
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.theme.withAlpha

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
    onKindChange: (FileKind?) -> Unit
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AppInput(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Buscar arquivos por nome, tipo ou pasta...",
                leadingIcon = Icons.Filled.Search,
                modifier = Modifier.weight(1f)
            )
            AppButton("Ordenar", onClick = {}, variant = AppButtonVariant.Outline, leadingIcon = Icons.Filled.Sort)
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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
            listOf(null, FileKind.Image, FileKind.Document, FileKind.Pdf, FileKind.Archive, FileKind.Link).forEach { kind ->
                val label = kind?.let { kindStyle(it).label + if (it == FileKind.Archive) "s" else "" } ?: "Todos"
                Badge(label, active = activeKind == kind, modifier = Modifier.clickable { onKindChange(kind) })
            }
        }
    }
}

@Composable
fun FileGridCard(file: FileItem, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val colors = FlesTheme.colors
    val style = kindStyle(file.kind)
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
            if (file.previewColor == null) {
                FileIconBadge(file.kind, large = true, modifier = Modifier.align(Alignment.Center))
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

@Composable
fun FileListRow(file: FileItem, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val colors = FlesTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(BorderStroke(0.5.dp, colors.border))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileIconBadge(file.kind)
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
