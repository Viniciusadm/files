package com.archieapps.fles.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.archieapps.fles.data.repository.AttachmentIo
import com.archieapps.fles.ui.model.Attachment
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.ui.model.ListItem
import com.archieapps.fles.ui.model.Message
import com.archieapps.fles.ui.model.MessageType
import com.archieapps.fles.ui.model.PendingAttachment
import com.archieapps.fles.ui.model.SyncStatus
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.theme.withAlpha
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ChatHeader(
    channel: Channel,
    itemCount: Int,
    onMenuClick: () -> Unit,
    onBack: (() -> Unit)? = null,
    onDeleteClick: () -> Unit = {}
) {
    val colors = FlesTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .border(BorderStroke(0.5.dp, colors.border))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButtonBox(Icons.Filled.Menu, onClick = onMenuClick)
        if (onBack != null) IconButtonBox(Icons.Filled.Close, onClick = onBack, iconSize = 20.dp)
        Spacer(Modifier.width(8.dp))
        FolderAvatar(channel.name, channel.color, size = 32.dp, icon = folderIcon(channel.icon))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(channel.name, color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$itemCount ${if (itemCount == 1) "item" else "itens"}", color = colors.mutedForeground, fontSize = 12.sp, maxLines = 1)
        }
        Box {
            IconButtonBox(Icons.Filled.MoreVert, onClick = { menuOpen = true })
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Excluir conversa", color = colors.destructive) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = colors.destructive) },
                    onClick = {
                        menuOpen = false
                        onDeleteClick()
                    }
                )
            }
        }
    }
}

@Composable
fun MessageList(
    messages: List<Message>,
    folderId: String,
    channels: List<Channel>,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 16.dp,
    onToggleFavorite: (String) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, String) -> Unit,
    onToggleListItem: (String, String) -> Unit,
    onAddListItem: (String, String) -> Unit,
    onUpdateListItem: (String, String, String) -> Unit,
    onDeleteListItem: (String, String) -> Unit,
    onReorderListItems: (String, List<String>) -> Unit,
    onDownload: suspend (Attachment) -> File
) {
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
        messages.sortedBy { it.createdAt }.groupBy { dayLabel(it.createdAt) }.forEach { (day, dayMessages) ->
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
                    Surface(shape = RoundedCornerShape(FlesTheme.radius.full), color = colors.background, shadowElevation = 1.dp) {
                        Text(day, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = colors.mutedForeground, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            items(dayMessages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    folderId = folderId,
                    channels = channels,
                    onToggleFavorite = onToggleFavorite,
                    onDelete = onDelete,
                    onMove = onMove,
                    onToggleListItem = onToggleListItem,
                    onAddListItem = onAddListItem,
                    onUpdateListItem = onUpdateListItem,
                    onDeleteListItem = onDeleteListItem,
                    onReorderListItems = onReorderListItems,
                    onDownload = onDownload
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    folderId: String,
    channels: List<Channel>,
    onToggleFavorite: (String) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, String) -> Unit,
    onToggleListItem: (String, String) -> Unit,
    onAddListItem: (String, String) -> Unit,
    onUpdateListItem: (String, String, String) -> Unit,
    onDeleteListItem: (String, String) -> Unit,
    onReorderListItems: (String, List<String>) -> Unit,
    onDownload: suspend (Attachment) -> File
) {
    val colors = FlesTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    var moveOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (message.type == MessageType.Text && message.attachments.isEmpty()) 0.78f else 0.9f)
                .shadow(1.dp, RoundedCornerShape(20.dp, 20.dp, 10.dp, 20.dp))
                .background(colors.bubbleOwn, RoundedCornerShape(20.dp, 20.dp, 10.dp, 20.dp))
                .combinedClickable(onClick = {}, onLongClick = { menuOpen = true })
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (message.type == MessageType.List) {
                    ListMessageCard(
                        message = message,
                        onToggle = { onToggleListItem(message.id, it) },
                        onAdd = { onAddListItem(message.id, it) },
                        onUpdate = { itemId, text -> onUpdateListItem(message.id, itemId, text) },
                        onRemove = { onDeleteListItem(message.id, it) },
                        onReorder = { onReorderListItems(message.id, it) }
                    )
                } else {
                    if (message.attachments.isNotEmpty()) AttachmentGrid(message.attachments, onDownload)
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
                    Spacer(Modifier.width(4.dp))
                    Box {
                        Icon(Icons.Filled.MoreVert, null, modifier = Modifier.size(16.dp).clickable { menuOpen = true }, tint = colors.bubbleOwnForeground.withAlpha(0.75f))
                        MessageMenu(
                            open = menuOpen,
                            isFavorite = message.isFavorite,
                            hasAttachments = message.attachments.isNotEmpty(),
                            onDismiss = { menuOpen = false },
                            onFavorite = { onToggleFavorite(message.id) },
                            onDelete = { confirmDelete = true },
                            onMove = { moveOpen = true },
                            onDownloadAll = {
                                message.attachments.forEach { onDownload(it) }
                            }
                        )
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir mensagem") },
            text = { Text("Essa ação remove a mensagem e os anexos relacionados.") },
            confirmButton = { AppButton("Excluir", variant = AppButtonVariant.Destructive, onClick = { confirmDelete = false; onDelete(message.id) }) },
            dismissButton = { AppButton("Cancelar", variant = AppButtonVariant.Ghost, onClick = { confirmDelete = false }) }
        )
    }
    if (moveOpen) {
        MoveMessageDialog(
            channels = channels.filter { it.id != folderId },
            onClose = { moveOpen = false },
            onMove = {
                moveOpen = false
                onMove(message.id, it)
            }
        )
    }
}

@Composable
private fun MessageMenu(
    open: Boolean,
    isFavorite: Boolean,
    hasAttachments: Boolean,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onDownloadAll: suspend () -> Unit
) {
    val colors = FlesTheme.colors
    val scope = rememberCoroutineScope()
    DropdownMenu(expanded = open, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(if (isFavorite) "Desfavoritar" else "Favoritar") },
            leadingIcon = { Icon(Icons.Filled.Star, null, tint = if (isFavorite) colors.favorite else colors.foreground) },
            onClick = { onDismiss(); onFavorite() }
        )
        if (hasAttachments) {
            DropdownMenuItem(
                text = { Text("Baixar arquivos") },
                leadingIcon = { Icon(Icons.Filled.Download, null) },
                onClick = { onDismiss(); scope.launch { onDownloadAll() } }
            )
        }
        DropdownMenuItem(
            text = { Text("Mover para...") },
            leadingIcon = { Icon(Icons.Filled.Folder, null) },
            onClick = { onDismiss(); onMove() }
        )
        DropdownMenuItem(
            text = { Text("Excluir", color = colors.destructive) },
            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = colors.destructive) },
            onClick = { onDismiss(); onDelete() }
        )
    }
}

@Composable
fun AttachmentGrid(attachments: List<Attachment>, onDownload: suspend (Attachment) -> File) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        attachments.forEach { attachment ->
            if (attachment.kind == com.archieapps.fles.ui.model.FileKind.Image) {
                ImageAttachmentCard(attachment, onDownload)
            } else {
                DocumentAttachmentCard(attachment, onDownload)
            }
        }
    }
}

@Composable
private fun ImageAttachmentCard(attachment: Attachment, onDownload: suspend (Attachment) -> File) {
    val colors = FlesTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewerFile by remember { mutableStateOf<File?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Surface(
        modifier = Modifier.width(220.dp).height(150.dp).clickable {
            loading = true
            error = null
            scope.launch {
                runCatching { onDownload(attachment) }
                    .onSuccess { viewerFile = it }
                    .onFailure { error = it.message ?: "Falha ao carregar" }
                loading = false
            }
        },
        shape = RoundedCornerShape(FlesTheme.radius.lg),
        color = colors.background.withAlpha(0.55f),
        border = BorderStroke(0.5.dp, colors.border)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.primary)
            else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.AttachFile, null, modifier = Modifier.size(32.dp), tint = colors.primary)
                    Text(error ?: "Toque para abrir", color = if (error == null) colors.mutedForeground else colors.destructive, fontSize = 12.sp)
                    Text(if (attachment.compressed) "Comprimida" else "Original", color = colors.mutedForeground, fontSize = 10.sp)
                }
            }
        }
    }
    viewerFile?.let { file ->
        ImageViewer(file = file, fileName = attachment.fileName, mimeType = attachment.mimeType, onClose = { viewerFile = null }, onOpen = { openFile(context, file, attachment.mimeType) })
    }
}

@Composable
private fun DocumentAttachmentCard(attachment: Attachment, onDownload: suspend (Attachment) -> File) {
    val colors = FlesTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Surface(shape = RoundedCornerShape(FlesTheme.radius.lg), color = colors.background.withAlpha(0.55f), border = BorderStroke(0.5.dp, colors.border)) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FileIconBadge(attachment.kind)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.width(156.dp)) {
                Text(attachment.fileName, color = colors.foreground, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatBytes(attachment.size), color = colors.mutedForeground, fontSize = 11.sp)
                if (error != null) Text(error.orEmpty(), color = colors.destructive, fontSize = 11.sp, maxLines = 2)
            }
            IconButtonBox(
                icon = if (busy) Icons.Filled.Schedule else Icons.Filled.Download,
                onClick = {
                    if (busy) return@IconButtonBox
                    busy = true
                    error = null
                    scope.launch {
                        runCatching { onDownload(attachment) }
                            .onSuccess { openFile(context, it, attachment.mimeType) }
                            .onFailure { error = it.message ?: "Falha no download" }
                        busy = false
                    }
                },
                tint = colors.primary
            )
        }
    }
}

@Composable
private fun ImageViewer(file: File, fileName: String, mimeType: String, onClose: () -> Unit, onOpen: () -> Unit) {
    val bitmap = remember(file.absolutePath) { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Box(modifier = Modifier.fillMaxWidth().height(420.dp).background(Color.Black), contentAlignment = Alignment.Center) {
                if (bitmap != null) Image(bitmap = bitmap, contentDescription = fileName, modifier = Modifier.fillMaxSize())
                else Text("Falha ao carregar.", color = Color.White)
            }
        },
        confirmButton = { AppButton("Baixar", leadingIcon = Icons.Filled.Download, onClick = onOpen) },
        dismissButton = { AppButton("Fechar", variant = AppButtonVariant.Ghost, onClick = onClose) }
    )
}

@Composable
fun ListMessageCard(
    message: Message,
    onToggle: (String) -> Unit,
    onAdd: (String) -> Unit,
    onUpdate: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onReorder: (List<String>) -> Unit
) {
    val colors = FlesTheme.colors
    val items = message.listItems.sortedBy { it.position }
    val done = items.count { it.checked }
    val total = items.size
    var newItem by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message.content.ifBlank { "Lista" }, color = colors.bubbleOwnForeground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(colors.foreground.withAlpha(0.08f), RoundedCornerShape(FlesTheme.radius.full))) {
            Box(modifier = Modifier.fillMaxWidth(if (total == 0) 0f else done.toFloat() / total.toFloat()).height(4.dp).background(colors.primary, RoundedCornerShape(FlesTheme.radius.full)))
        }
        Text("$done de $total concluídos", color = colors.bubbleOwnForeground.withAlpha(0.7f), fontSize = 11.sp)
        items.forEachIndexed { index, item ->
            ListItemRow(
                item = item,
                onToggle = { onToggle(item.id) },
                onUpdate = { onUpdate(item.id, it) },
                onRemove = { onRemove(item.id) },
                onMoveUp = if (index > 0) ({ reorder(items, index, -1, onReorder) }) else null,
                onMoveDown = if (index < items.lastIndex) ({ reorder(items, index, 1, onReorder) }) else null
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AppInput(newItem, { newItem = it }, "Adicionar item", modifier = Modifier.weight(1f))
            IconButtonBox(Icons.Filled.Add, onClick = {
                val text = newItem.trim()
                if (text.isNotBlank()) {
                    onAdd(text)
                    newItem = ""
                }
            }, tint = colors.foreground, background = colors.muted)
        }
    }
}

@Composable
private fun ListItemRow(
    item: ListItem,
    onToggle: () -> Unit,
    onUpdate: (String) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    val colors = FlesTheme.colors
    var editing by remember { mutableStateOf(false) }
    var text by remember(item.id) { mutableStateOf(item.text) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButtonBox(if (item.checked) Icons.Filled.CheckCircle else Icons.Filled.Check, onClick = onToggle, tint = if (item.checked) Color(0xFF16A34A) else colors.mutedForeground, size = 28.dp, iconSize = 16.dp)
        if (editing) {
            AppInput(text, { text = it }, "Item", modifier = Modifier.weight(1f))
            IconButtonBox(Icons.Filled.Check, onClick = { editing = false; onUpdate(text) }, size = 28.dp, iconSize = 16.dp)
        } else {
            Text(
                item.text,
                modifier = Modifier.weight(1f).clickable { editing = true },
                color = if (item.checked) colors.bubbleOwnForeground.withAlpha(0.6f) else colors.bubbleOwnForeground,
                fontSize = 13.sp,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null
            )
            if (onMoveUp != null) IconButtonBox(Icons.Filled.ArrowUpward, onClick = onMoveUp, size = 26.dp, iconSize = 14.dp, tint = colors.mutedForeground)
            if (onMoveDown != null) IconButtonBox(Icons.Filled.ArrowDownward, onClick = onMoveDown, size = 26.dp, iconSize = 14.dp, tint = colors.mutedForeground)
            IconButtonBox(Icons.Filled.Edit, onClick = { editing = true }, size = 26.dp, iconSize = 14.dp, tint = colors.mutedForeground)
            IconButtonBox(Icons.Filled.Close, onClick = onRemove, size = 26.dp, iconSize = 14.dp, tint = colors.destructive)
        }
    }
}

@Composable
fun Composer(
    folderId: String,
    keyConfigured: Boolean,
    attachmentIo: AttachmentIo,
    sending: Boolean,
    onSend: (String, List<PendingAttachment>) -> Unit,
    onCreateList: (String, List<String>) -> Unit,
    onConfigureKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FlesTheme.colors
    var text by remember { mutableStateOf("") }
    var pickerOpen by remember { mutableStateOf(false) }
    var listOpen by remember { mutableStateOf(false) }
    val pending = remember { mutableStateListOf<PendingAttachment>() }
    val docs = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        pending.addAll(uris.map { attachmentIo.pendingFromUri(it) })
    }
    val images = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        pending.addAll(uris.map { attachmentIo.pendingFromUri(it, "image/*") })
    }
    if (!keyConfigured) {
        Column(modifier = modifier.fillMaxWidth().background(colors.background).border(BorderStroke(0.5.dp, colors.border)).padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().background(colors.favorite.withAlpha(0.15f), RoundedCornerShape(FlesTheme.radius.lg)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Configure sua chave E2EE para enviar mensagens.", modifier = Modifier.weight(1f), color = colors.foreground, fontSize = 13.sp)
                AppButton("Configurar", onClick = onConfigureKey)
            }
        }
        return
    }
    Column(
        modifier = modifier.fillMaxWidth().background(colors.background).border(BorderStroke(0.5.dp, colors.border)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (pending.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(160.dp).verticalScroll(rememberScrollState())) {
                pending.forEachIndexed { index, item ->
                    PendingAttachmentRow(item = item, onRemove = { pending.removeAt(index) })
                }
            }
        }
        Surface(modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(FlesTheme.radius.x2l)), shape = RoundedCornerShape(FlesTheme.radius.x2l), color = colors.card, border = BorderStroke(0.5.dp, colors.border)) {
            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.Bottom) {
                IconButtonBox(Icons.Filled.AttachFile, onClick = { pickerOpen = true }, tint = colors.mutedForeground, size = 36.dp)
                IconButtonBox(Icons.Filled.ListAlt, onClick = { listOpen = true }, tint = colors.mutedForeground, size = 36.dp)
                AppInput(value = text, onValueChange = { text = it }, placeholder = "Escreva uma mensagem...", modifier = Modifier.weight(1f), singleLine = false)
                val canSend = (text.trim().isNotBlank() || pending.isNotEmpty()) && !sending
                Box(modifier = Modifier.size(36.dp).background(colors.primary.withAlpha(if (canSend) 1f else 0.5f), RoundedCornerShape(FlesTheme.radius.full)).clickable(enabled = canSend) {
                    onSend(text, pending.toList())
                    text = ""
                    pending.clear()
                }, contentAlignment = Alignment.Center) {
                    if (sending) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.primaryForeground)
                    else Icon(Icons.Filled.Send, null, modifier = Modifier.size(18.dp), tint = colors.primaryForeground)
                }
            }
        }
        Text("Toque no clipe para anexar", modifier = Modifier.padding(horizontal = 8.dp), color = colors.mutedForeground, fontSize = 11.sp, textAlign = TextAlign.Start)
    }
    if (pickerOpen) {
        AlertDialog(
            onDismissRequest = { pickerOpen = false },
            title = { Text("Anexar") },
            text = {
                Column {
                    DropdownLikeRow("Galeria", Icons.Filled.AttachFile) { pickerOpen = false; images.launch("image/*") }
                    DropdownLikeRow("Arquivos", Icons.Filled.AttachFile) { pickerOpen = false; docs.launch(arrayOf("*/*")) }
                }
            },
            confirmButton = {},
            dismissButton = { AppButton("Cancelar", variant = AppButtonVariant.Ghost, onClick = { pickerOpen = false }) }
        )
    }
    if (listOpen) CreateListDialog(onClose = { listOpen = false }, onCreate = onCreateList)
}

@Composable
private fun PendingAttachmentRow(item: PendingAttachment, onRemove: () -> Unit) {
    val colors = FlesTheme.colors
    Surface(shape = RoundedCornerShape(FlesTheme.radius.lg), color = colors.muted, border = BorderStroke(0.5.dp, colors.border)) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FileIconBadge(item.kind)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.fileName, color = colors.foreground, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatBytes(item.size) + if (item.isImage && item.compress) " · Comprimida" else "", color = colors.mutedForeground, fontSize = 11.sp)
            }
            IconButtonBox(Icons.Filled.Close, onClick = onRemove, tint = colors.destructive)
        }
    }
}

@Composable
private fun CreateListDialog(onClose: () -> Unit, onCreate: (String, List<String>) -> Unit) {
    var title by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Nova lista") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                AppInput(title, { title = it }, "Título (opcional)")
                items.forEachIndexed { index, value ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppInput(value, { items[index] = it }, "Item ${index + 1}", modifier = Modifier.weight(1f))
                        IconButtonBox(Icons.Filled.Close, onClick = { if (items.size > 1) items.removeAt(index) }, tint = FlesTheme.colors.mutedForeground)
                    }
                }
                AppButton("Adicionar item", leadingIcon = Icons.Filled.Add, variant = AppButtonVariant.Outline, onClick = { items.add("") })
            }
        },
        confirmButton = { AppButton("Criar lista", onClick = { onCreate(title, items.toList()); onClose() }) },
        dismissButton = { AppButton("Cancelar", variant = AppButtonVariant.Ghost, onClick = onClose) }
    )
}

@Composable
private fun MoveMessageDialog(channels: List<Channel>, onClose: () -> Unit, onMove: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Mover para...") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (channels.isEmpty()) Text("Nenhuma outra pasta disponível.")
                channels.forEach { channel -> FolderListItem(channel = channel, active = false, onClick = { onMove(channel.id) }) }
            }
        },
        confirmButton = {},
        dismissButton = { AppButton("Cancelar", variant = AppButtonVariant.Ghost, onClick = onClose) }
    )
}

@Composable
private fun DropdownLikeRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = FlesTheme.colors.primary)
        Spacer(Modifier.width(12.dp))
        Text(label, color = FlesTheme.colors.foreground)
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
    if (iso.length < 10) return ""
    return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4)
}

fun formatTime(iso: String): String {
    return runCatching {
        val t = iso.substringAfter('T').take(5)
        if (t.length == 5) t else ""
    }.getOrDefault("")
}

private fun reorder(items: List<ListItem>, index: Int, dir: Int, onReorder: (List<String>) -> Unit) {
    val ids = items.map { it.id }.toMutableList()
    val target = index + dir
    if (target !in ids.indices) return
    val tmp = ids[index]
    ids[index] = ids[target]
    ids[target] = tmp
    onReorder(ids)
}

fun openFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, file.name)) }
}
