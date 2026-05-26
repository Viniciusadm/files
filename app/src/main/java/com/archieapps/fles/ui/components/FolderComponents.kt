package com.archieapps.fles.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.ui.theme.FlesTheme

data class ChannelTreeItem(
    val channel: Channel,
    val depth: Int
)

data class ChannelTreeNode(
    val channel: Channel,
    val children: List<ChannelTreeNode>
)

fun buildChannelTree(channels: List<Channel>): List<ChannelTreeNode> {
    val childrenByParent = channels.groupBy { it.parentId }
    val seen = mutableSetOf<String>()

    fun build(parentId: String?): List<ChannelTreeNode> {
        return childrenByParent[parentId].orEmpty().mapNotNull { channel ->
            if (!seen.add(channel.id)) null
            else ChannelTreeNode(channel, build(channel.id))
        }
    }

    val roots = build(null).toMutableList()
    channels.forEach { channel ->
        if (seen.add(channel.id)) {
            roots += ChannelTreeNode(channel, build(channel.id))
        }
    }
    return roots
}

fun flattenChannelTree(channels: List<Channel>): List<ChannelTreeItem> {
    val result = mutableListOf<ChannelTreeItem>()
    fun walk(nodes: List<ChannelTreeNode>, depth: Int) {
        nodes.forEach { node ->
            result += ChannelTreeItem(node.channel, depth)
            walk(node.children, depth + 1)
        }
    }
    walk(buildChannelTree(channels), 0)
    return result
}

fun folderIcon(name: String): ImageVector {
    return when (name) {
        "Inbox" -> Icons.Filled.Inbox
        "Briefcase" -> Icons.Filled.BusinessCenter
        "GraduationCap" -> Icons.Filled.School
        "Image" -> Icons.Filled.Image
        "Star" -> Icons.Filled.Star
        else -> Icons.Filled.Folder
    }
}

@Composable
fun FolderAvatar(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    icon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(size * 0.5f), tint = Color.White)
        } else {
            Text(
                name.take(2).uppercase(),
                color = Color.White,
                fontSize = if (size <= 32.dp) 12.sp else 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun FolderGridCard(
    channel: Channel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val colors = FlesTheme.colors
    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(FlesTheme.radius.x2l))
            .clip(RoundedCornerShape(FlesTheme.radius.x2l))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(FlesTheme.radius.x2l))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .background(channel.color.copy(alpha = 0.12f)),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            FolderAvatar(channel.name, channel.color, size = 64.dp, icon = folderIcon(channel.icon))
        }
        Text(
            channel.name,
            modifier = androidx.compose.ui.Modifier.padding(10.dp),
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
fun FolderListItem(
    channel: Channel,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    depth: Int = 0,
    hasChildren: Boolean = false,
    expanded: Boolean = false,
    childCount: Int = 0,
    onToggleExpand: (() -> Unit)? = null,
    onCreateSubfolder: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val colors = FlesTheme.colors
    val chevronRotation by animateFloatAsState(targetValue = if (expanded) 90f else 0f, label = "chevron")
    var menuOpen by remember { mutableStateOf(false) }
    val hasContextMenu = onCreateSubfolder != null || onRename != null || onDelete != null
    Box(modifier = modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(FlesTheme.radius.lg))
            .background(if (active) colors.sidebarAccent else Color.Transparent)
            .let {
                if (hasContextMenu) it.combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
                else it.clickable(onClick = onClick)
            }
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = (depth * 12).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (depth > 0) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(colors.border)
            )
            Spacer(Modifier.width(8.dp))
        }
        if (hasChildren && onToggleExpand != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleExpand),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = if (expanded) "Colapsar" else "Expandir",
                    tint = colors.mutedForeground,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(32.dp))
        }
        FolderAvatar(channel.name, channel.color, size = 40.dp, icon = folderIcon(channel.icon))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    channel.name,
                    color = colors.sidebarForeground,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (hasChildren && childCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "($childCount)",
                        color = colors.mutedForeground,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            Text(
                channel.lastMessage,
                color = colors.mutedForeground,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
    }
        if (hasContextMenu) {
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (onCreateSubfolder != null) {
                    DropdownMenuItem(
                        text = { Text("Nova subpasta", color = colors.foreground) },
                        leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null, tint = colors.foreground) },
                        onClick = {
                            menuOpen = false
                            onCreateSubfolder()
                        }
                    )
                }
                if (onRename != null) {
                    DropdownMenuItem(
                        text = { Text("Renomear", color = colors.foreground) },
                        leadingIcon = { Icon(Icons.Filled.Edit, null, tint = colors.foreground) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        }
                    )
                }
                if (onDelete != null) {
                    DropdownMenuItem(
                        text = { Text("Excluir", color = colors.destructive) },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = colors.destructive) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RenameChannelDialog(
    currentName: String,
    onClose: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Renomear pasta") },
        text = {
            AppInput(name, { name = it }, "Nome da pasta")
        },
        confirmButton = {
            AppButton(
                "Salvar",
                enabled = name.trim().isNotBlank() && name.trim() != currentName,
                onClick = { onConfirm(name.trim()) }
            )
        },
        dismissButton = { AppButton("Cancelar", variant = AppButtonVariant.Ghost, onClick = onClose) }
    )
}

@Composable
fun ConfirmDeleteChannelDialog(
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Excluir conversa") },
        text = { Text("Essa ação remove a pasta e todas as mensagens contidas.") },
        confirmButton = { AppButton("Excluir", variant = AppButtonVariant.Destructive, onClick = onConfirm) },
        dismissButton = { AppButton("Cancelar", variant = AppButtonVariant.Ghost, onClick = onClose) }
    )
}
