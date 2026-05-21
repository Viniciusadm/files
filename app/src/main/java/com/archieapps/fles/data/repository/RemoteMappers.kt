package com.archieapps.fles.data.repository

import androidx.compose.ui.graphics.Color
import com.archieapps.fles.data.remote.RemoteAttachment
import com.archieapps.fles.data.remote.RemoteChannel
import com.archieapps.fles.data.remote.RemoteListItem
import com.archieapps.fles.data.remote.RemoteMessage
import com.archieapps.fles.ui.model.Attachment
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.ui.model.FileItem
import com.archieapps.fles.ui.model.FileKind
import com.archieapps.fles.ui.model.ListItem
import com.archieapps.fles.ui.model.Message
import com.archieapps.fles.ui.model.MessageType
import com.archieapps.fles.ui.model.SyncStatus

suspend fun RemoteChannel.toUi(cryptoRepository: CryptoRepository): Channel {
    return Channel(
        id = id.orEmpty(),
        name = cryptoRepository.tryDecryptString(name),
        lastMessage = cryptoRepository.tryDecryptString(lastMessage),
        updatedAt = updatedAt.orEmpty(),
        icon = icon ?: "Folder",
        color = parseColor(color),
        syncStatus = SyncStatus.Synced
    )
}

suspend fun RemoteMessage.toUi(cryptoRepository: CryptoRepository): Message {
    val uiAttachments = attachments.orEmpty().map { it.toUi(cryptoRepository) }
    return Message(
        id = id.orEmpty(),
        folderId = folderId.orEmpty(),
        type = type.toMessageType(uiAttachments),
        content = cryptoRepository.tryDecryptString(content),
        createdAt = createdAt.orEmpty(),
        isFavorite = isFavorite.asBoolean(),
        syncStatus = SyncStatus.Synced,
        attachments = uiAttachments,
        listItems = listItems.orEmpty().map { it.toUi(cryptoRepository) }
    )
}

suspend fun RemoteListItem.toUi(cryptoRepository: CryptoRepository): ListItem {
    return ListItem(
        id = id.orEmpty(),
        text = cryptoRepository.tryDecryptString(text),
        checked = isChecked.asBoolean(),
        position = position.asInt()
    )
}

suspend fun RemoteAttachment.toUi(cryptoRepository: CryptoRepository): Attachment {
    return Attachment(
        id = id.orEmpty(),
        fileName = cryptoRepository.tryDecryptString(fileName),
        mimeType = mimeType ?: "application/octet-stream",
        size = size.asLong(),
        kind = fileKind.toFileKind(mimeType, fileName, extension),
        createdAt = createdAt.orEmpty(),
        url = url.orEmpty(),
        previewUrl = previewUrl,
        compressed = compressed.asBoolean(),
        extension = extension.orEmpty(),
        previewColor = if (fileKind == "image") Color(0xFFDBEAFE) else null
    )
}

fun Message.toFileItem(channel: Channel): List<FileItem> {
    return attachments.map {
        FileItem(
            id = it.id,
            name = it.fileName,
            kind = it.kind,
            size = it.size,
            folderId = folderId,
            folderName = channel.name,
            messageId = id,
            isFavorite = isFavorite,
            createdAt = it.createdAt,
            url = it.url,
            mimeType = it.mimeType,
            compressed = it.compressed,
            extension = it.extension,
            previewColor = it.previewColor
        )
    }
}

private fun parseColor(value: String?): Color {
    val hex = value?.removePrefix("#")?.takeIf { it.length == 6 || it.length == 8 } ?: "3390EC"
    return runCatching {
        val argb = if (hex.length == 6) "FF$hex" else hex
        Color(argb.toLong(16))
    }.getOrDefault(Color(0xFF3390EC))
}

private fun String?.toMessageType(attachments: List<Attachment>): MessageType {
    return when (this) {
        "list" -> MessageType.List
        "image" -> MessageType.Image
        "document" -> MessageType.Document
        "mixed" -> MessageType.Mixed
        "text" -> MessageType.Text
        else -> when {
            attachments.isEmpty() -> MessageType.Text
            attachments.all { it.kind == FileKind.Image } -> MessageType.Image
            attachments.size > 1 -> MessageType.Mixed
            else -> MessageType.Document
        }
    }
}

private fun String?.toFileKind(mimeType: String?, fileName: String?, extension: String?): FileKind {
    val kind = this?.lowercase()
    if (kind != null) {
        when (kind) {
            "image" -> return FileKind.Image
            "document" -> return FileKind.Document
            "pdf" -> return FileKind.Pdf
            "code" -> return FileKind.Code
            "other" -> return FileKind.Other
        }
    }
    val mime = mimeType.orEmpty().lowercase()
    val ext = (extension ?: fileName?.substringAfterLast('.', "")).orEmpty().lowercase()
    return when {
        mime.startsWith("image/") -> FileKind.Image
        mime.startsWith("video/") -> FileKind.Video
        mime.startsWith("audio/") -> FileKind.Audio
        mime == "application/pdf" || ext == "pdf" -> FileKind.Pdf
        ext in setOf("zip", "rar", "7z", "tar", "gz") -> FileKind.Archive
        ext in setOf("xls", "xlsx", "csv") -> FileKind.Spreadsheet
        ext in setOf("ppt", "pptx") -> FileKind.Presentation
        ext in setOf("js", "ts", "kt", "java", "py", "php", "html", "css", "json") -> FileKind.Code
        ext in setOf("txt", "md") -> FileKind.Text
        mime == "text/uri-list" || ext == "url" -> FileKind.Link
        mime.contains("document") || ext in setOf("doc", "docx", "odt") -> FileKind.Document
        else -> FileKind.Other
    }
}

private fun Any?.asBoolean(): Boolean {
    return when (this) {
        is Boolean -> this
        is Number -> toInt() == 1
        is String -> equals("true", ignoreCase = true) || this == "1"
        else -> false
    }
}

private fun Any?.asLong(): Long {
    return when (this) {
        is Number -> toLong()
        is String -> toLongOrNull() ?: 0L
        else -> 0L
    }
}

private fun Any?.asInt(): Int {
    return when (this) {
        is Number -> toInt()
        is String -> toIntOrNull() ?: 0
        else -> 0
    }
}
