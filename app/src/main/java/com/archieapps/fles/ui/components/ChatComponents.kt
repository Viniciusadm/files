package com.archieapps.fles.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.fles.ui.model.MessageType
import com.archieapps.fles.ui.model.MockChannel
import com.archieapps.fles.ui.model.MockMessage
import com.archieapps.fles.ui.model.SyncStatus
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.theme.withAlpha

@Composable
fun ChatHeader(
    channel: MockChannel,
    itemCount: Int,
    onMenuClick: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val colors = FlesTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .border(BorderStroke(0.5.dp, colors.border))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButtonBox(Icons.Filled.Menu, onClick = onMenuClick)
        if (onBack != null) {
            IconButtonBox(Icons.Filled.ChevronLeft, onClick = onBack, iconSize = 22.dp)
        }
        Spacer(Modifier.width(8.dp))
        FolderAvatar(channel.name, channel.color, size = 32.dp, icon = folderIcon(channel.icon))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(channel.name, color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$itemCount ${if (itemCount == 1) "item" else "itens"}", color = colors.mutedForeground, fontSize = 12.sp, maxLines = 1)
        }
        IconButtonBox(Icons.Filled.MoreVert, onClick = {})
    }
}

@Composable
fun MessageList(messages: List<MockMessage>, modifier: Modifier = Modifier, bottomPadding: Dp = 16.dp) {
    val colors = FlesTheme.colors
    if (messages.isEmpty()) {
        EmptyState(
            title = "Nenhuma mensagem ainda",
            description = "Envie textos para começar a conversa nesta pasta.",
            icon = Icons.Filled.Inbox,
            modifier = modifier.fillMaxSize().background(colors.chatBg)
        )
        return
    }
    LazyColumn(
        modifier = modifier.background(colors.chatBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val grouped = messages.sortedBy { it.createdAt }.groupBy { dayLabel(it.createdAt) }
        grouped.forEach { (day, dayMessages) ->
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
                    Surface(
                        modifier = Modifier.shadow(1.dp, RoundedCornerShape(FlesTheme.radius.full)),
                        shape = RoundedCornerShape(FlesTheme.radius.full),
                        color = colors.background
                    ) {
                        Text(day, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = colors.mutedForeground, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            items(dayMessages, key = { it.id }) {
                MessageBubble(it)
            }
        }
    }
}

@Composable
fun MessageBubble(message: MockMessage) {
    val colors = FlesTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .shadow(1.dp, RoundedCornerShape(20.dp, 20.dp, 10.dp, 20.dp)),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 10.dp),
            color = colors.bubbleOwn
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (message.type == MessageType.List) {
                    ListMessageCard(message)
                } else {
                    if (message.attachments.isNotEmpty()) {
                        AttachmentGrid(message)
                    }
                    if (message.content.isNotBlank()) {
                        Text(message.content, color = colors.bubbleOwnForeground, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (message.isFavorite) {
                        Icon(Icons.Filled.Star, null, modifier = Modifier.size(11.dp), tint = colors.favorite)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(formatTime(message.createdAt), color = colors.bubbleOwnForeground.withAlpha(0.7f), fontSize = 11.sp)
                    Spacer(Modifier.width(6.dp))
                    SyncIcon(message.syncStatus, tint = colors.bubbleOwnForeground.withAlpha(0.7f))
                }
            }
        }
    }
}

@Composable
fun AttachmentGrid(message: MockMessage) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        message.attachments.forEach { attachment ->
            Surface(
                shape = RoundedCornerShape(FlesTheme.radius.lg),
                color = FlesTheme.colors.background.withAlpha(0.55f),
                border = BorderStroke(0.5.dp, FlesTheme.colors.border)
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FileIconBadge(attachment.kind)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.width(156.dp)) {
                        Text(attachment.fileName, color = FlesTheme.colors.foreground, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatBytes(attachment.size), color = FlesTheme.colors.mutedForeground, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ListMessageCard(message: MockMessage) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message.content, color = FlesTheme.colors.bubbleOwnForeground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        message.listItems.forEach {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (it.checked) Icons.Filled.CheckCircle else Icons.Filled.Check, null, modifier = Modifier.size(16.dp), tint = if (it.checked) Color(0xFF16A34A) else FlesTheme.colors.mutedForeground)
                Spacer(Modifier.width(8.dp))
                Text(it.text, color = FlesTheme.colors.bubbleOwnForeground, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun Composer(modifier: Modifier = Modifier) {
    val colors = FlesTheme.colors
    var text by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .border(BorderStroke(0.5.dp, colors.border))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(FlesTheme.radius.x2l)),
            shape = RoundedCornerShape(FlesTheme.radius.x2l),
            color = colors.card,
            border = BorderStroke(0.5.dp, colors.border)
        ) {
            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.Bottom) {
                IconButtonBox(Icons.Filled.AttachFile, onClick = {}, tint = colors.mutedForeground, size = 36.dp)
                IconButtonBox(Icons.Filled.ListAlt, onClick = {}, tint = colors.mutedForeground, size = 36.dp)
                AppInput(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "Escreva uma mensagem...",
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.primary, RoundedCornerShape(FlesTheme.radius.full))
                        .clickable { text = "" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Send, null, modifier = Modifier.size(18.dp), tint = colors.primaryForeground)
                }
            }
        }
        Text("Toque no clipe para anexar", modifier = Modifier.padding(horizontal = 8.dp), color = colors.mutedForeground, fontSize = 11.sp, textAlign = TextAlign.Start)
    }
}

@Composable
fun SyncIcon(status: SyncStatus, tint: Color) {
    val icon = when (status) {
        SyncStatus.Synced -> Icons.Filled.Check
        SyncStatus.Error -> Icons.Filled.ErrorOutline
        else -> Icons.Filled.Schedule
    }
    Icon(icon, null, modifier = Modifier.size(11.dp), tint = if (status == SyncStatus.Error) Color(0xFFE57373) else tint)
}

fun dayLabel(iso: String): String {
    return when (iso.substring(0, 10)) {
        "2026-05-20" -> "Hoje"
        "2026-05-19" -> "Ontem"
        else -> iso.substring(8, 10) + " de maio de 2026"
    }
}

fun formatTime(iso: String): String = iso.substring(11, 16)
