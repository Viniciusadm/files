package com.archieapps.fles.ui.model

import androidx.compose.ui.graphics.Color

enum class MessageType {
    Text,
    List,
    Image,
    Document,
    Mixed
}

enum class SyncStatus {
    Synced,
    PendingCreate,
    PendingUpload,
    Error
}

enum class FileKind {
    Image,
    Video,
    Audio,
    Pdf,
    Archive,
    Spreadsheet,
    Presentation,
    Document,
    Code,
    Text,
    Link,
    Other
}

data class User(
    val id: Long,
    val name: String?,
    val email: String
)

data class Channel(
    val id: String,
    val name: String,
    val lastMessage: String,
    val updatedAt: String,
    val icon: String,
    val color: Color,
    val syncStatus: SyncStatus = SyncStatus.Synced
)

data class ListItem(
    val id: String,
    val text: String,
    val checked: Boolean,
    val position: Int = 0
)

data class Attachment(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val kind: FileKind,
    val createdAt: String,
    val url: String,
    val previewUrl: String? = null,
    val compressed: Boolean = false,
    val extension: String = "",
    val previewColor: Color? = null
)

data class Message(
    val id: String,
    val folderId: String,
    val type: MessageType,
    val content: String,
    val createdAt: String,
    val isFavorite: Boolean,
    val syncStatus: SyncStatus,
    val attachments: List<Attachment> = emptyList(),
    val listItems: List<ListItem> = emptyList()
)

data class FileItem(
    val id: String,
    val name: String,
    val kind: FileKind,
    val size: Long,
    val folderId: String,
    val folderName: String,
    val messageId: String,
    val isFavorite: Boolean,
    val createdAt: String,
    val url: String,
    val mimeType: String,
    val compressed: Boolean,
    val extension: String,
    val previewColor: Color? = null
)

data class PendingAttachment(
    val uri: android.net.Uri,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val kind: FileKind,
    val extension: String,
    val isImage: Boolean,
    val compress: Boolean = isImage
)
