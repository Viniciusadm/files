package com.archieapps.fles.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
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

fun flattenChannelTree(channels: List<Channel>): List<ChannelTreeItem> {
    val childrenByParent = channels.groupBy { it.parentId }
    val seen = mutableSetOf<String>()
    val result = mutableListOf<ChannelTreeItem>()

    fun append(parentId: String?, depth: Int) {
        childrenByParent[parentId].orEmpty().forEach { channel ->
            if (!seen.add(channel.id)) return@forEach
            result += ChannelTreeItem(channel, depth)
            append(channel.id, depth + 1)
        }
    }

    append(null, 0)
    channels.forEach { channel ->
        if (seen.add(channel.id)) {
            result += ChannelTreeItem(channel, 0)
            append(channel.id, 1)
        }
    }

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
fun FolderListItem(
    channel: Channel,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    depth: Int = 0
) {
    val colors = FlesTheme.colors
    Row(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(FlesTheme.radius.lg))
            .background(if (active) colors.sidebarAccent else Color.Transparent)
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = (depth * 18).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(8.dp))
        FolderAvatar(channel.name, channel.color, size = 40.dp, icon = folderIcon(channel.icon))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                channel.name,
                color = colors.sidebarForeground,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
}
