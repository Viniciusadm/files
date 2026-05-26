package com.archieapps.fles.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.archieapps.fles.data.repository.AttachmentIo
import com.archieapps.fles.ui.model.Attachment
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.ui.model.FileItem
import com.archieapps.fles.ui.model.FileKind
import com.archieapps.fles.ui.model.ListItem
import com.archieapps.fles.ui.model.Message
import com.archieapps.fles.ui.model.MessageType
import com.archieapps.fles.ui.model.PendingAttachment
import com.archieapps.fles.ui.model.SyncStatus
import com.archieapps.fles.ui.model.UploadStatus
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.theme.withAlpha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun Modifier.bottomHairline(color: Color): Modifier = drawBehind {
    val stroke = 0.5.dp.toPx()
    drawLine(color, Offset(0f, size.height - stroke / 2f), Offset(size.width, size.height - stroke / 2f), stroke)
}

private fun Modifier.topHairline(color: Color): Modifier = drawBehind {
    val stroke = 0.5.dp.toPx()
    drawLine(color, Offset(0f, stroke / 2f), Offset(size.width, stroke / 2f), stroke)
}

@Composable
fun ChatHeader(
    channel: Channel,
    itemCount: Int,
    onMenuClick: () -> Unit,
    onBack: (() -> Unit)? = null,
    onCreateSubfolderClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val colors = FlesTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .bottomHairline(colors.border)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBack != null) IconButtonBox(Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = onBack, iconSize = 22.dp)
        FolderAvatar(channel.name, channel.color, size = 32.dp, icon = folderIcon(channel.icon))
        Column(modifier = Modifier.weight(1f)) {
            Text(channel.name, color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$itemCount ${if (itemCount == 1) "item" else "itens"}", color = colors.mutedForeground, fontSize = 12.sp, maxLines = 1)
        }
        IconButtonBox(Icons.Filled.Settings, onClick = onMenuClick)
        Box {
            IconButtonBox(Icons.Filled.MoreVert, onClick = { menuOpen = true })
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Nova subpasta", color = colors.foreground) },
                    leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null, tint = colors.foreground) },
                    onClick = {
                        menuOpen = false
                        onCreateSubfolderClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Excluir conversa", color = colors.destructive) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = colors.destructive) },
                    onClick = {
                        menuOpen = false
                        confirmDelete = true
                    }
                )
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir conversa") },
            text = { Text("Essa ação remove a pasta e todas as mensagens contidas.") },
            confirmButton = { AppButton("Excluir", variant = AppButtonVariant.Destructive, onClick = { confirmDelete = false; onDeleteClick() }) },
            dismissButton = { AppButton("Cancelar", variant = AppButtonVariant.Ghost, onClick = { confirmDelete = false }) }
        )
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
    onRetryUpload: (String) -> Unit,
    onCancelUpload: (String) -> Unit,
    onDownload: suspend (Attachment) -> File
) {
    val colors = FlesTheme.colors
    val groupedMessages = remember(messages) {
        messages.sortedBy { it.createdAt }.groupBy { dayLabel(it.createdAt) }.toList()
    }
    val listItemCount = groupedMessages.sumOf { (_, dayMessages) -> 1 + dayMessages.size }
    val latestMessageId = groupedMessages.lastOrNull()?.second?.lastOrNull()?.id
    val listState = rememberLazyListState()

    LaunchedEffect(folderId, latestMessageId, listItemCount) {
        if (listItemCount > 0) {
            listState.scrollToItem(listItemCount - 1)
        }
    }

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
        state = listState,
        modifier = modifier.background(colors.chatBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedMessages.forEach { (day, dayMessages) ->
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
                    onRetryUpload = onRetryUpload,
                    onCancelUpload = onCancelUpload,
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
    onRetryUpload: (String) -> Unit,
    onCancelUpload: (String) -> Unit,
    onDownload: suspend (Attachment) -> File
) {
    val colors = FlesTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    var moveOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            val shape = RoundedCornerShape(
                topStart = FlesTheme.radius.x2l,
                topEnd = FlesTheme.radius.x2l,
                bottomEnd = FlesTheme.radius.md,
                bottomStart = FlesTheme.radius.x2l
            )
            val maxBubbleWidth = maxWidth * if (message.type == MessageType.Text && message.attachments.isEmpty()) 0.85f else 0.92f
            Box(
                modifier = Modifier
                    .widthIn(
                        min = if (message.type == MessageType.List) 260.dp else 0.dp,
                        max = maxBubbleWidth
                    )
                    .shadow(1.dp, shape)
                    .background(colors.bubbleOwn, shape)
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
                        if (message.attachments.isNotEmpty()) {
                            AttachmentGrid(
                                attachments = message.attachments,
                                onRetryUpload = { onRetryUpload(message.id) },
                                onCancelUpload = { onCancelUpload(message.id) },
                                onDownload = onDownload
                            )
                        }
                        if (message.content.isNotBlank()) {
                            Text(message.content, color = colors.bubbleOwnForeground, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                        if (message.attachments.isEmpty() && message.type != MessageType.Text && message.content.isBlank()) {
                            Text("[anexo indisponível]", color = colors.bubbleOwnForeground.withAlpha(0.6f), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    message.lastError?.takeIf { it.isNotBlank() }?.let { error ->
                        Text(error, color = Color(0xFFFFCDD2), fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                    Row(modifier = Modifier.align(Alignment.End).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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
    var downloading by remember { mutableStateOf(false) }
    if (!open) return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.withAlpha(0.25f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .shadow(3.dp, RoundedCornerShape(FlesTheme.radius.lg))
                    .clip(RoundedCornerShape(FlesTheme.radius.lg))
                    .background(colors.card)
                    .border(BorderStroke(0.5.dp, colors.border), RoundedCornerShape(FlesTheme.radius.lg))
                    .clickable(enabled = false) {},
            ) {
                MessageMenuRow(
                    icon = Icons.Filled.Star,
                    label = if (isFavorite) "Desfavoritar" else "Favoritar",
                    iconTint = if (isFavorite) colors.favorite else colors.foreground,
                    onClick = {
                        onDismiss()
                        onFavorite()
                    }
                )
                if (hasAttachments) {
                    MenuSeparator()
                    MessageMenuRow(
                        icon = Icons.Filled.Download,
                        label = "Baixar arquivos",
                        busy = downloading,
                        onClick = {
                            if (downloading) return@MessageMenuRow
                            downloading = true
                            scope.launch {
                                runCatching { onDownloadAll() }
                                downloading = false
                                onDismiss()
                            }
                        }
                    )
                }
                MenuSeparator()
                MessageMenuRow(
                    icon = Icons.Filled.FolderOpen,
                    label = "Mover para...",
                    onClick = {
                        onDismiss()
                        onMove()
                    }
                )
                MenuSeparator()
                MessageMenuRow(
                    icon = Icons.Filled.Delete,
                    label = "Excluir",
                    iconTint = colors.destructive,
                    textColor = colors.destructive,
                    onClick = {
                        onDismiss()
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun MessageMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    busy: Boolean = false,
    iconTint: Color = FlesTheme.colors.foreground,
    textColor: Color = FlesTheme.colors.foreground,
    onClick: () -> Unit
) {
    val colors = FlesTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (busy) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.foreground)
        else Icon(icon, null, modifier = Modifier.size(16.dp), tint = iconTint)
        Text(label, color = textColor, fontSize = 14.sp)
    }
}

@Composable
private fun MenuSeparator() {
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(FlesTheme.colors.border))
}

@Composable
fun AttachmentGrid(
    attachments: List<Attachment>,
    onRetryUpload: () -> Unit,
    onCancelUpload: () -> Unit,
    onDownload: suspend (Attachment) -> File
) {
    if (attachments.isEmpty()) return
    val media = attachments.filter { it.kind == FileKind.Image || it.kind == FileKind.Video }
    val docs = attachments.filter { it.kind != FileKind.Image && it.kind != FileKind.Video }
    val context = LocalContext.current
    var albumOpen by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when {
            media.size == 1 -> {
                val attachment = media[0]
                if (attachment.kind == FileKind.Image) {
                    ImageThumb(
                        attachment = attachment,
                        height = 200.dp,
                        onRetryUpload = onRetryUpload,
                        onCancelUpload = onCancelUpload,
                        onOpen = { if (attachment.uploadStatus == UploadStatus.Uploaded) viewerIndex = 0 },
                        onEnsureLocal = onDownload
                    )
                } else {
                    VideoThumb(
                        attachment = attachment,
                        height = 200.dp,
                        onRetryUpload = onRetryUpload,
                        onCancelUpload = onCancelUpload,
                        onDownload = onDownload
                    )
                }
            }
            media.size > 1 -> {
                MediaCollage(
                    media = media,
                    onRetryUpload = onRetryUpload,
                    onCancelUpload = onCancelUpload,
                    onEnsureLocal = onDownload,
                    onOpen = { albumOpen = true }
                )
            }
        }
        if (docs.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                docs.forEach { attachment ->
                    DocumentAttachmentCard(attachment, onRetryUpload, onCancelUpload, onDownload)
                }
            }
        }
    }

    if (albumOpen && media.size > 1) {
        MediaAlbumOverlay(
            media = media,
            onClose = { albumOpen = false },
            onPick = { index -> viewerIndex = index },
            onEnsureLocal = onDownload
        )
    }

    val openIndex = viewerIndex
    if (openIndex != null && media.isNotEmpty()) {
        val viewerFiles = remember(media) { media.map { it.toViewerFileItem() } }
        val byId = remember(media) { media.associateBy { it.id } }
        MediaPagerOverlay(
            files = viewerFiles,
            initialIndex = openIndex,
            onEnsureLocalFile = { fileItem ->
                val attachment = byId[fileItem.id]
                    ?: error("Attachment not found for ${fileItem.id}")
                onDownload(attachment)
            },
            onClose = { viewerIndex = null },
            onDownloadCurrent = { fileItem ->
                val attachment = byId[fileItem.id] ?: return@MediaPagerOverlay
                runCatching { onDownload(attachment) }
                    .onSuccess { openFile(context, it, attachment.mimeType) }
            }
        )
    }
}

@Composable
private fun MediaCollage(
    media: List<Attachment>,
    onRetryUpload: () -> Unit,
    onCancelUpload: () -> Unit,
    onEnsureLocal: suspend (Attachment) -> File,
    onOpen: () -> Unit
) {
    val gap = 3.dp
    val height = 200.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(FlesTheme.radius.lg))
    ) {
        when {
            media.size == 2 -> Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                MediaCell(media[0], onRetryUpload, onCancelUpload, onEnsureLocal, onOpen, modifier = Modifier.weight(1f).fillMaxHeight())
                MediaCell(media[1], onRetryUpload, onCancelUpload, onEnsureLocal, onOpen, modifier = Modifier.weight(1f).fillMaxHeight())
            }
            media.size == 3 -> Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                MediaCell(media[0], onRetryUpload, onCancelUpload, onEnsureLocal, onOpen, modifier = Modifier.weight(1f).fillMaxHeight())
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    MediaCell(media[1], onRetryUpload, onCancelUpload, onEnsureLocal, onOpen, modifier = Modifier.weight(1f).fillMaxWidth())
                    MediaCell(media[2], onRetryUpload, onCancelUpload, onEnsureLocal, onOpen, modifier = Modifier.weight(1f).fillMaxWidth())
                }
            }
            else -> Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    MediaCell(media[0], onRetryUpload, onCancelUpload, onEnsureLocal, onOpen, modifier = Modifier.weight(1f).fillMaxHeight())
                    MediaCell(media[1], onRetryUpload, onCancelUpload, onEnsureLocal, onOpen, modifier = Modifier.weight(1f).fillMaxHeight())
                }
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    MediaCell(media[2], onRetryUpload, onCancelUpload, onEnsureLocal, onOpen, modifier = Modifier.weight(1f).fillMaxHeight())
                    MediaCell(
                        media[3],
                        onRetryUpload, onCancelUpload, onEnsureLocal, onOpen,
                        overlayCount = if (media.size > 4) media.size - 4 else null,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaCell(
    attachment: Attachment,
    onRetryUpload: () -> Unit,
    onCancelUpload: () -> Unit,
    onEnsureLocal: suspend (Attachment) -> File,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    overlayCount: Int? = null
) {
    val colors = FlesTheme.colors
    val isVideo = attachment.kind == FileKind.Video
    val ready = attachment.uploadStatus == UploadStatus.Uploaded
    val preview = rememberAttachmentBitmap(attachment, onEnsure = onEnsureLocal)
    val videoLocalPath = if (isVideo) {
        remember(attachment.id, attachment.localPath) {
            attachment.localPath?.removePrefix("file://")?.takeIf { File(it).exists() }
        }
    } else null
    val videoMeta = if (isVideo) rememberVideoMeta(videoLocalPath) else null
    val videoThumb = videoMeta?.thumbnail

    Box(
        modifier = modifier
            .background(if (isVideo) Color.Black else colors.muted)
            .clickable(enabled = ready, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            preview != null -> Image(bitmap = preview, contentDescription = attachment.fileName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            videoThumb != null -> Image(bitmap = videoThumb, contentDescription = attachment.fileName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        if (isVideo && overlayCount == null) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Black.withAlpha(0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        if (overlayCount != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.withAlpha(0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Text("+$overlayCount", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (attachment.uploadStatus != UploadStatus.Uploaded) {
            UploadProgressPanel(
                attachment = attachment,
                onRetry = onRetryUpload,
                onCancel = onCancelUpload,
                modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp)
            )
        }
    }
}

@Composable
private fun MediaAlbumOverlay(
    media: List<Attachment>,
    onClose: () -> Unit,
    onPick: (Int) -> Unit,
    onEnsureLocal: suspend (Attachment) -> File
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.withAlpha(0.95f))
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, null, tint = Color.White)
                }
                Text(
                    "${media.size} ${if (media.size == 1) "item" else "itens"}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(media, key = { _, a -> a.id }) { index, attachment ->
                    MediaCell(
                        attachment = attachment,
                        onRetryUpload = {},
                        onCancelUpload = {},
                        onEnsureLocal = onEnsureLocal,
                        onClick = { onPick(index) },
                        modifier = Modifier.aspectRatio(1f).fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun Attachment.toViewerFileItem(): FileItem = FileItem(
    id = id,
    name = fileName,
    kind = kind,
    size = size,
    folderId = "",
    folderName = "",
    messageId = messageId,
    isFavorite = false,
    createdAt = createdAt,
    url = url,
    mimeType = mimeType,
    compressed = compressed,
    extension = extension,
    previewColor = previewColor,
    localPath = localPath,
    previewLocalPath = previewLocalPath,
    syncStatus = syncStatus,
    lastError = lastError,
    isSecure = false
)

@Composable
private fun VideoThumb(
    attachment: Attachment,
    height: Dp,
    onRetryUpload: () -> Unit,
    onCancelUpload: () -> Unit,
    onDownload: suspend (Attachment) -> File
) {
    val colors = FlesTheme.colors
    val scope = rememberCoroutineScope()
    var localFile by remember(attachment.id, attachment.localPath) {
        mutableStateOf(attachment.localPath?.removePrefix("file://")?.let { File(it).takeIf { f -> f.exists() } })
    }
    var loading by remember(attachment.id) { mutableStateOf(false) }
    var loadFailed by remember(attachment.id) { mutableStateOf(false) }
    var playerOpen by remember(attachment.id) { mutableStateOf(false) }
    val ready = attachment.uploadStatus == UploadStatus.Uploaded
    val meta = rememberVideoMeta(localFile?.absolutePath)
    val durationLabel = formatVideoDuration(meta.durationMs)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(FlesTheme.radius.lg))
            .background(Color.Black)
            .border(BorderStroke(0.5.dp, colors.border), RoundedCornerShape(FlesTheme.radius.lg))
            .clickable(enabled = ready) {
                if (localFile != null) {
                    playerOpen = true
                    return@clickable
                }
                loading = true
                loadFailed = false
                scope.launch {
                    runCatching { onDownload(attachment) }
                        .onSuccess {
                            localFile = it
                            playerOpen = true
                        }
                        .onFailure { loadFailed = true }
                    loading = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val thumbnail = meta.thumbnail
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = attachment.fileName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.Black.withAlpha(0.55f)),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Reproduzir",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        if (durationLabel.isNotEmpty()) {
            AttachmentBadge(
                label = durationLabel,
                background = Color.Black.withAlpha(0.6f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
            )
        }
        if (attachment.uploadStatus != UploadStatus.Uploaded) {
            UploadProgressPanel(
                attachment = attachment,
                onRetry = onRetryUpload,
                onCancel = onCancelUpload,
                modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp)
            )
        }
        if (loadFailed) {
            Text(
                "Falha ao carregar vídeo",
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }
    }

    if (playerOpen) {
        val file = localFile
        if (file != null) {
            VideoPlayerOverlay(
                title = attachment.fileName,
                uri = android.net.Uri.fromFile(file).toString(),
                onClose = { playerOpen = false }
            )
        }
    }
}

@Composable
private fun ImageThumb(
    attachment: Attachment,
    height: Dp,
    onRetryUpload: () -> Unit,
    onCancelUpload: () -> Unit,
    onOpen: () -> Unit,
    onEnsureLocal: suspend (Attachment) -> File
) {
    val colors = FlesTheme.colors
    val preview = rememberAttachmentBitmap(attachment, onEnsure = onEnsureLocal)
    val compressed = attachment.compressed
    val ready = attachment.uploadStatus == UploadStatus.Uploaded
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(FlesTheme.radius.lg))
            .background(colors.muted)
            .border(BorderStroke(0.5.dp, colors.border), RoundedCornerShape(FlesTheme.radius.lg))
            .clickable(enabled = ready, onClick = onOpen),
        contentAlignment = Alignment.Center
    ) {
        if (preview != null) {
            Image(bitmap = preview, contentDescription = attachment.fileName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text("Toque para abrir", color = colors.mutedForeground, fontSize = 11.sp)
        }
        AttachmentBadge(
            label = if (compressed) "Comprimida" else "Original",
            background = if (compressed) colors.primary.withAlpha(0.85f) else Color.Black.withAlpha(0.55f),
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
        )
        if (attachment.uploadStatus != UploadStatus.Uploaded) {
            UploadProgressPanel(
                attachment = attachment,
                onRetry = onRetryUpload,
                onCancel = onCancelUpload,
                modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp)
            )
        }
    }
}

@Composable
private fun DocumentAttachmentCard(attachment: Attachment, onRetryUpload: () -> Unit, onCancelUpload: () -> Unit, onDownload: suspend (Attachment) -> File) {
    val colors = FlesTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FlesTheme.radius.lg))
            .background(colors.muted.withAlpha(0.6f))
            .border(BorderStroke(0.5.dp, colors.border), RoundedCornerShape(FlesTheme.radius.lg))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(FlesTheme.radius.md))
                .background(colors.card)
                .border(BorderStroke(0.5.dp, colors.border), RoundedCornerShape(FlesTheme.radius.md)),
            contentAlignment = Alignment.Center
        ) {
            Icon(documentIcon(attachment.kind), null, modifier = Modifier.size(26.dp), tint = colors.primary)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(attachment.fileName, color = colors.bubbleOwnForeground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatBytes(attachment.size) + if (attachment.compressed) " · Comprimida" else "", color = colors.bubbleOwnForeground.withAlpha(0.65f), fontSize = 11.sp)
            if (attachment.uploadStatus != UploadStatus.Uploaded) {
                UploadProgressPanel(attachment, onRetryUpload, onCancelUpload)
            }
            if (error != null) {
                Text(error.orEmpty(), color = colors.destructive, fontSize = 11.sp, maxLines = 2)
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(FlesTheme.radius.full))
                .background(colors.primary.withAlpha(0.15f))
                .clickable(enabled = !busy && attachment.uploadStatus == UploadStatus.Uploaded) {
                    busy = true
                    error = null
                    scope.launch {
                        runCatching { onDownload(attachment) }
                            .onSuccess { openFile(context, it, attachment.mimeType) }
                            .onFailure { error = it.message ?: "Falha no download" }
                        busy = false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (busy) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.primary)
            else Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp), tint = colors.primary)
        }
    }
}

@Composable
private fun UploadProgressPanel(
    attachment: Attachment,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FlesTheme.colors
    val label = when (attachment.uploadStatus) {
        UploadStatus.PendingUpload -> "pausado"
        UploadStatus.Uploading -> "enviando"
        UploadStatus.Retrying -> "retomando"
        UploadStatus.Uploaded -> "concluído"
        UploadStatus.Failed -> "erro"
        UploadStatus.Cancelled -> "cancelado"
        UploadStatus.Paused -> "pausado"
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FlesTheme.radius.md))
            .background(colors.background.withAlpha(0.86f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LinearProgressIndicator(
            progress = attachment.uploadProgress,
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(FlesTheme.radius.full)),
            color = if (attachment.uploadStatus == UploadStatus.Failed) colors.destructive else colors.primary,
            trackColor = colors.border
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${(attachment.uploadProgress * 100).toInt()}% · ${formatBytes(attachment.uploadedBytes)} / ${formatBytes(attachment.totalBytes)} · $label", modifier = Modifier.weight(1f), color = colors.foreground, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (attachment.uploadStatus == UploadStatus.Failed) {
                Text("Tentar", modifier = Modifier.clickable { onRetry() }.padding(4.dp), color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("Cancelar", modifier = Modifier.clickable { onCancel() }.padding(4.dp), color = colors.destructive, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AttachmentBadge(label: String, background: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(FlesTheme.radius.sm))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun rememberAttachmentBitmap(
    attachment: Attachment,
    onEnsure: (suspend (Attachment) -> File)? = null
): ImageBitmap? {
    val source = attachment.previewLocalPath ?: attachment.previewUrl ?: attachment.localPath ?: attachment.url
    var bitmap by remember(attachment.id, source) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(attachment.id, source) {
        val initial = loadBitmap(source)
        if (initial != null) {
            bitmap = initial
            return@LaunchedEffect
        }
        if (onEnsure == null || attachment.uploadStatus != UploadStatus.Uploaded) return@LaunchedEffect
        val materialized = runCatching { onEnsure(attachment) }.getOrNull() ?: return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(materialized.absolutePath)?.asImageBitmap() }.getOrNull()
        }
    }
    return bitmap
}

private suspend fun loadBitmap(source: String?): ImageBitmap? {
    if (source.isNullOrBlank()) return null
    return withContext(Dispatchers.IO) {
        runCatching {
            if (source.startsWith("http://") || source.startsWith("https://")) {
                URL(source).openStream().use { BitmapFactory.decodeStream(it) }
            } else {
                BitmapFactory.decodeFile(source.removePrefix("file://"))
            }?.asImageBitmap()
        }.getOrNull()
    }
}

private fun documentIcon(kind: FileKind): androidx.compose.ui.graphics.vector.ImageVector {
    return when (kind) {
        FileKind.Pdf, FileKind.Document, FileKind.Text -> Icons.Filled.Description
        FileKind.Code -> Icons.AutoMirrored.Filled.InsertDriveFile
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
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
    Column(modifier = Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(FlesTheme.radius.md))
                    .background(colors.primary.withAlpha(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ListAlt, null, modifier = Modifier.size(16.dp), tint = colors.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(message.content.ifBlank { "Lista" }, color = colors.bubbleOwnForeground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$done de $total concluídos", color = colors.bubbleOwnForeground.withAlpha(0.7f), fontSize = 11.sp)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(FlesTheme.radius.full))
                .background(colors.foreground.withAlpha(0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (total == 0) 0f else done.toFloat() / total.toFloat())
                    .height(4.dp)
                    .clip(RoundedCornerShape(FlesTheme.radius.full))
                    .background(colors.primary)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ListTextInput(value = newItem, onValueChange = { newItem = it }, placeholder = "Adicionar item", modifier = Modifier.weight(1f), onSubmit = {
                val text = newItem.trim()
                if (text.isNotBlank()) {
                    onAdd(text)
                    newItem = ""
                }
            })
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(FlesTheme.radius.full))
                    .background(colors.muted)
                    .clickable(enabled = newItem.trim().isNotEmpty()) {
                        val text = newItem.trim()
                        if (text.isNotBlank()) {
                            onAdd(text)
                            newItem = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp), tint = colors.foreground)
            }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FlesTheme.radius.md))
            .clickable { }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(FlesTheme.radius.sm))
                .background(if (item.checked) colors.primary else Color.Transparent)
                .border(BorderStroke(1.5.dp, if (item.checked) colors.primary else colors.bubbleOwnForeground.withAlpha(0.3f)), RoundedCornerShape(FlesTheme.radius.sm))
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (item.checked) Icon(Icons.Filled.Check, null, modifier = Modifier.size(12.dp), tint = colors.primaryForeground)
        }
        if (editing) {
            ListTextInput(value = text, onValueChange = { text = it }, placeholder = "Item", modifier = Modifier.weight(1f), onSubmit = {
                editing = false
                onUpdate(text)
            })
        } else {
            Text(
                item.text,
                modifier = Modifier.weight(1f),
                color = if (item.checked) colors.bubbleOwnForeground.withAlpha(0.6f) else colors.bubbleOwnForeground,
                fontSize = 14.sp,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (onMoveUp != null) MiniIconButton(Icons.Filled.ArrowUpward, tint = colors.mutedForeground, onClick = onMoveUp)
                if (onMoveDown != null) MiniIconButton(Icons.Filled.ArrowDownward, tint = colors.mutedForeground, onClick = onMoveDown)
                MiniIconButton(Icons.Filled.Edit, tint = colors.mutedForeground, onClick = { editing = true })
                MiniIconButton(Icons.Filled.Close, tint = colors.destructive, onClick = onRemove)
            }
        }
    }
}

@Composable
private fun MiniIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(FlesTheme.radius.sm))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(13.dp), tint = tint)
    }
}

@Composable
private fun ListTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSubmit: (() -> Unit)? = null
) {
    val colors = FlesTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(FlesTheme.radius.md))
            .background(colors.background)
            .border(BorderStroke(0.5.dp, colors.border), RoundedCornerShape(FlesTheme.radius.md))
            .padding(horizontal = 12.dp),
        textStyle = TextStyle(color = colors.foreground, fontSize = 14.sp, lineHeight = 20.sp),
        cursorBrush = SolidColor(colors.primary),
        singleLine = true,
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onDone = { onSubmit?.invoke() },
            onNext = { onSubmit?.invoke() }
        ),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, color = colors.mutedForeground, fontSize = 14.sp)
                inner()
            }
        }
    )
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
    var attachmentError by remember { mutableStateOf<String?>(null) }
    val pending = remember { mutableStateListOf<PendingAttachment>() }
    val docs = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        runCatching { uris.map { attachmentIo.pendingFromUri(it) } }
            .onSuccess {
                pending.addAll(it)
                attachmentError = null
            }
            .onFailure { attachmentError = it.message ?: "Não foi possível anexar o arquivo." }
    }
    val images = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        runCatching { uris.map { attachmentIo.pendingFromUri(it) } }
            .onSuccess {
                pending.addAll(it)
                attachmentError = null
            }
            .onFailure { attachmentError = it.message ?: "Não foi possível anexar o arquivo." }
    }
    if (!keyConfigured) {
        Column(modifier = modifier.fillMaxWidth().background(colors.background).topHairline(colors.border).padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().background(colors.favorite.withAlpha(0.15f), RoundedCornerShape(FlesTheme.radius.lg)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Configure sua chave E2EE para enviar mensagens.", modifier = Modifier.weight(1f), color = colors.foreground, fontSize = 13.sp)
                AppButton("Configurar", onClick = onConfigureKey)
            }
        }
        return
    }
    Column(
        modifier = modifier.fillMaxWidth().background(colors.background).topHairline(colors.border).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (pending.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                pending.forEachIndexed { index, item ->
                    PendingAttachmentRow(item = item, onRemove = { pending.removeAt(index) })
                }
            }
        }
        Surface(modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(FlesTheme.radius.x2l)), shape = RoundedCornerShape(FlesTheme.radius.x2l), color = colors.card, border = BorderStroke(0.5.dp, colors.border)) {
            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButtonBox(Icons.Filled.AttachFile, onClick = { pickerOpen = true }, tint = colors.mutedForeground, size = 36.dp)
                IconButtonBox(Icons.AutoMirrored.Filled.ListAlt, onClick = { listOpen = true }, tint = colors.mutedForeground, size = 36.dp)
                MessageComposerInput(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f))
                val canSend = (text.trim().isNotBlank() || pending.isNotEmpty()) && !sending
                Box(modifier = Modifier.size(36.dp).background(colors.primary.withAlpha(if (canSend) 1f else 0.5f), RoundedCornerShape(FlesTheme.radius.full)).clickable(enabled = canSend) {
                    onSend(text, pending.toList())
                    text = ""
                    pending.clear()
                }, contentAlignment = Alignment.Center) {
                    if (sending) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.primaryForeground)
                    else Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp), tint = colors.primaryForeground)
                }
            }
        }
        attachmentError?.let {
            Text(it, modifier = Modifier.padding(horizontal = 8.dp), color = colors.destructive, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Text("Toque no clipe para anexar", modifier = Modifier.padding(horizontal = 8.dp), color = colors.mutedForeground, fontSize = 11.sp, textAlign = TextAlign.Start)
    }
    if (pickerOpen) {
        AlertDialog(
            onDismissRequest = { pickerOpen = false },
            title = { Text("Anexar") },
            text = {
                Column {
                    DropdownLikeRow("Galeria", Icons.Filled.PhotoLibrary) {
                        pickerOpen = false
                        runCatching {
                            images.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        }.onFailure {
                            android.util.Log.e("Composer", "Falha ao abrir galeria", it)
                            attachmentError = "Não foi possível abrir a galeria: ${it.message}"
                        }
                    }
                    DropdownLikeRow("Arquivos", Icons.Filled.AttachFile) {
                        pickerOpen = false
                        runCatching { docs.launch(arrayOf("*/*")) }
                            .onFailure {
                                android.util.Log.e("Composer", "Falha ao abrir arquivos", it)
                                attachmentError = "Não foi possível abrir arquivos: ${it.message}"
                            }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { AppButton("Cancelar", variant = AppButtonVariant.Ghost, onClick = { pickerOpen = false }) }
        )
    }
    if (listOpen) CreateListDialog(onClose = { listOpen = false }, onCreate = onCreateList)
}

@Composable
private fun MessageComposerInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = FlesTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .heightIn(min = 36.dp, max = 160.dp)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        textStyle = TextStyle(color = colors.foreground, fontSize = 14.sp, lineHeight = 20.sp),
        cursorBrush = SolidColor(colors.primary),
        singleLine = false,
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text("Escreva uma mensagem...", color = colors.mutedForeground, fontSize = 14.sp, lineHeight = 20.sp)
                }
                inner()
            }
        }
    )
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
    var error by remember { mutableStateOf<String?>(null) }
    val colors = FlesTheme.colors
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.withAlpha(0.4f))
                .padding(16.dp)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 448.dp)
                    .shadow(3.dp, RoundedCornerShape(FlesTheme.radius.xl))
                    .clip(RoundedCornerShape(FlesTheme.radius.xl))
                    .background(colors.background)
                    .border(BorderStroke(0.5.dp, colors.border), RoundedCornerShape(FlesTheme.radius.xl))
                    .clickable(enabled = false) {}
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ListAlt, null, modifier = Modifier.size(20.dp), tint = colors.primary)
                    Text("Nova lista", color = colors.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Crie um checklist com vários itens. Você poderá marcar e reordenar depois.",
                    color = colors.mutedForeground,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    Text("Título (opcional)", color = colors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    ListTextInput(value = title, onValueChange = { title = it }, placeholder = "Ex.: Lista de compras", modifier = Modifier.fillMaxWidth())
                }
                Text("Itens", color = colors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEachIndexed { index, value ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${index + 1}.", color = colors.mutedForeground, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.width(20.dp))
                            ListTextInput(
                                value = value,
                                onValueChange = { items[index] = it },
                                placeholder = "Item ${index + 1}",
                                modifier = Modifier.weight(1f),
                                onSubmit = {
                                    if (index == items.lastIndex) items.add("")
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(FlesTheme.radius.md))
                                    .clickable(enabled = items.size > 1) {
                                        if (items.size > 1) items.removeAt(index)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp), tint = colors.mutedForeground.withAlpha(if (items.size > 1) 1f else 0.3f))
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(FlesTheme.radius.md))
                        .clickable { items.add("") }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp), tint = colors.mutedForeground)
                    Text("Adicionar item", color = colors.mutedForeground, fontSize = 13.sp)
                }
                if (error != null) {
                    Text(error.orEmpty(), color = colors.destructive, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(FlesTheme.radius.md))
                            .clickable(onClick = onClose)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancelar", color = colors.foreground, fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(FlesTheme.radius.md))
                            .background(colors.primary)
                            .clickable {
                                val filled = items.map { it.trim() }.filter { it.isNotBlank() }
                                if (filled.isEmpty()) {
                                    error = "Adicione ao menos um item"
                                    return@clickable
                                }
                                error = null
                                onCreate(title.trim(), filled)
                                onClose()
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Criar lista", color = colors.primaryForeground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun MoveMessageDialog(channels: List<Channel>, onClose: () -> Unit, onMove: (String) -> Unit) {
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
        SyncStatus.Failed -> Icons.Filled.ErrorOutline
        else -> Icons.Filled.Schedule
    }
    Icon(icon, null, modifier = Modifier.size(11.dp), tint = if (status == SyncStatus.Failed) Color(0xFFE57373) else tint)
}

fun dayLabel(iso: String): String {
    val date = parseIsoCalendar(iso) ?: return ""
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val yesterday = today.clone() as Calendar
    yesterday.add(Calendar.DATE, -1)
    val day = date.clone() as Calendar
    day.set(Calendar.HOUR_OF_DAY, 0)
    day.set(Calendar.MINUTE, 0)
    day.set(Calendar.SECOND, 0)
    day.set(Calendar.MILLISECOND, 0)
    return when (day.timeInMillis) {
        today.timeInMillis -> "Hoje"
        yesterday.timeInMillis -> "Ontem"
        else -> SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault()).format(date.time)
    }
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

private fun parseIsoCalendar(iso: String): Calendar? {
    val normalized = iso.replace(Regex("\\.\\d+"), "")
    val patterns = listOf("yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd")
    for (pattern in patterns) {
        val parsed = runCatching {
            SimpleDateFormat(pattern, Locale.getDefault()).parse(normalized)
        }.getOrNull()
        if (parsed != null) {
            return Calendar.getInstance().apply { time = parsed }
        }
    }
    return null
}

fun openFile(context: Context, file: File, mimeType: String): Result<Unit> {
    return runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, file.name))
    }
}
