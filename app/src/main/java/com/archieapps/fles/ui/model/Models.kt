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
    PendingUpdate,
    PendingDelete,
    PendingUpload,
    Uploading,
    Downloading,
    Failed
}

enum class UploadStatus {
    PendingUpload,
    Uploading,
    Uploaded,
    Failed
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
    val remoteId: String? = null,
    val name: String,
    val lastMessage: String,
    val updatedAt: String,
    val icon: String,
    val color: Color,
    val isSecure: Boolean = false,
    val parentId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val lastError: String? = null
)

data class ListItem(
    val id: String,
    val remoteId: String? = null,
    val messageId: String = "",
    val text: String,
    val checked: Boolean,
    val position: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val lastError: String? = null
)

data class Attachment(
    val id: String,
    val remoteId: String? = null,
    val messageId: String = "",
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val kind: FileKind,
    val createdAt: String,
    val url: String,
    val previewUrl: String? = null,
    val compressed: Boolean = false,
    val extension: String = "",
    val previewColor: Color? = null,
    val localPath: String? = null,
    val previewLocalPath: String? = null,
    val uploadStatus: UploadStatus = UploadStatus.Uploaded,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val lastError: String? = null
)

data class Message(
    val id: String,
    val remoteId: String? = null,
    val folderId: String,
    val type: MessageType,
    val content: String,
    val createdAt: String,
    val updatedAt: String = createdAt,
    val isFavorite: Boolean,
    val syncStatus: SyncStatus,
    val lastError: String? = null,
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
    val previewColor: Color? = null,
    val localPath: String? = null,
    val previewLocalPath: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val lastError: String? = null,
    val isSecure: Boolean = false
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
