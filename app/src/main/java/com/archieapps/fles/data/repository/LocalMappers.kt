package com.archieapps.fles.data.repository

import androidx.compose.ui.graphics.Color
import com.archieapps.fles.data.local.db.AttachmentEntity
import com.archieapps.fles.data.local.db.ChannelEntity
import com.archieapps.fles.data.local.db.ListItemEntity
import com.archieapps.fles.data.local.db.LocalSyncStatus
import com.archieapps.fles.data.local.db.LocalUploadStatus
import com.archieapps.fles.data.local.db.MessageEntity
import com.archieapps.fles.data.local.db.formatIso
import com.archieapps.fles.ui.model.Attachment
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.ui.model.FileKind
import com.archieapps.fles.ui.model.ListItem
import com.archieapps.fles.ui.model.Message
import com.archieapps.fles.ui.model.MessageType
import com.archieapps.fles.ui.model.SyncStatus
import com.archieapps.fles.ui.model.UploadStatus

fun ChannelEntity.toUi(): Channel {
    return Channel(
        id = localId,
        remoteId = remoteId,
        name = name,
        lastMessage = lastMessagePreview.orEmpty(),
        updatedAt = formatIso(updatedAt),
        icon = icon ?: "Folder",
        color = parseColorValue(color),
        isSecure = isSecure,
        parentId = parentLocalId,
        syncStatus = syncStatus.toUiSyncStatus(),
        lastError = lastError
    )
}

fun MessageEntity.toUi(items: List<ListItem>, attachments: List<Attachment>): Message {
    return Message(
        id = localId,
        remoteId = remoteId,
        folderId = folderLocalId,
        type = type.toUiMessageType(attachments),
        content = content,
        createdAt = formatIso(createdAt),
        updatedAt = formatIso(updatedAt),
        isFavorite = isFavorite,
        syncStatus = syncStatus.toUiSyncStatus(),
        lastError = lastError,
        attachments = attachments,
        listItems = items
    )
}

fun ListItemEntity.toUi(): ListItem {
    return ListItem(
        id = localId,
        remoteId = remoteId,
        messageId = messageLocalId,
        text = text,
        checked = isChecked,
        position = position.toInt(),
        syncStatus = syncStatus.toUiSyncStatus(),
        lastError = lastError
    )
}

fun AttachmentEntity.toUi(): Attachment {
    val kind = fileKind.toFileKind(mimeType, fileName, extension)
    return Attachment(
        id = localId,
        remoteId = remoteId,
        messageId = messageLocalId,
        fileName = fileName,
        mimeType = mimeType,
        size = size,
        kind = kind,
        createdAt = formatIso(createdAt),
        url = localPath ?: remoteUrl.orEmpty(),
        previewUrl = previewLocalPath ?: previewUrl,
        compressed = compressed ?: false,
        extension = extension.orEmpty(),
        previewColor = if (kind == FileKind.Image) Color(0xFFDBEAFE) else null,
        localPath = localPath,
        previewLocalPath = previewLocalPath,
        uploadStatus = uploadStatus.toUiUploadStatus(),
        syncStatus = syncStatus.toUiSyncStatus(),
        lastError = lastError
    )
}

fun String.toUiSyncStatus(): SyncStatus {
    return when (this) {
        LocalSyncStatus.Synced -> SyncStatus.Synced
        LocalSyncStatus.PendingCreate -> SyncStatus.PendingCreate
        LocalSyncStatus.PendingUpdate -> SyncStatus.PendingUpdate
        LocalSyncStatus.PendingDelete -> SyncStatus.PendingDelete
        LocalSyncStatus.PendingUpload -> SyncStatus.PendingUpload
        LocalSyncStatus.Uploading -> SyncStatus.Uploading
        LocalSyncStatus.Downloading -> SyncStatus.Downloading
        else -> SyncStatus.Failed
    }
}

fun String.toUiUploadStatus(): UploadStatus {
    return when (this) {
        LocalUploadStatus.PendingUpload -> UploadStatus.PendingUpload
        LocalUploadStatus.Uploading -> UploadStatus.Uploading
        LocalUploadStatus.Uploaded -> UploadStatus.Uploaded
        else -> UploadStatus.Failed
    }
}

fun FileKind.toRemoteKind(): String {
    return when (this) {
        FileKind.Image -> "image"
        FileKind.Pdf -> "pdf"
        FileKind.Code -> "code"
        FileKind.Document,
        FileKind.Text,
        FileKind.Spreadsheet,
        FileKind.Presentation -> "document"
        else -> "other"
    }
}

fun String?.toLocalMessageType(): String = when (this) {
    "list", "image", "document", "mixed", "text" -> this
    else -> "text"
}

private fun String.toUiMessageType(attachments: List<Attachment>): MessageType {
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
    when (this?.lowercase()) {
        "image" -> return FileKind.Image
        "document" -> return FileKind.Document
        "pdf" -> return FileKind.Pdf
        "code" -> return FileKind.Code
        "other" -> return FileKind.Other
    }
    val mime = mimeType.orEmpty().lowercase()
    val ext = (extension ?: fileName?.substringAfterLast('.', "")).orEmpty().lowercase()
    return when {
        mime.startsWith("image/") -> FileKind.Image
        mime.startsWith("video/") -> FileKind.Video
        mime.startsWith("audio/") -> FileKind.Audio
        mime == "application/pdf" || ext == "pdf" -> FileKind.Pdf
        ext in setOf("zip", "rar", "7z", "tar", "gz") -> FileKind.Archive
        ext in setOf("xls", "xlsx", "csv", "ods") -> FileKind.Spreadsheet
        ext in setOf("ppt", "pptx", "odp") -> FileKind.Presentation
        ext in setOf("js", "ts", "kt", "java", "py", "php", "html", "css", "json", "md", "xml", "sql") -> FileKind.Code
        mime.startsWith("text/") || ext in setOf("txt", "rtf") -> FileKind.Text
        mime.contains("document") || ext in setOf("doc", "docx", "odt") -> FileKind.Document
        else -> FileKind.Other
    }
}

private fun parseColorValue(value: String?): Color {
    val hex = value?.removePrefix("#")?.takeIf { it.length == 6 || it.length == 8 } ?: "3390EC"
    return runCatching {
        val argb = if (hex.length == 6) "FF$hex" else hex
        Color(argb.toLong(16))
    }.getOrDefault(Color(0xFF3390EC))
}
