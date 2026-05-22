package com.archieapps.fles.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val localId: String,
    val remoteId: String?,
    val name: String?,
    val email: String,
    val updatedAt: Long
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val userLocalId: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "channels",
    indices = [
        Index("remoteId", unique = true),
        Index("parentLocalId"),
        Index("deletedAt", "updatedAt")
    ]
)
data class ChannelEntity(
    @PrimaryKey val localId: String,
    val remoteId: String?,
    val name: String,
    val icon: String?,
    val color: String?,
    val parentLocalId: String?,
    val lastMessagePreview: String?,
    val syncStatus: String,
    val lastError: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)

@Entity(
    tableName = "messages",
    indices = [
        Index("remoteId", unique = true),
        Index("folderLocalId", "deletedAt", "createdAt")
    ]
)
data class MessageEntity(
    @PrimaryKey val localId: String,
    val remoteId: String?,
    val folderLocalId: String,
    val type: String,
    val content: String,
    val isFavorite: Boolean,
    val syncStatus: String,
    val lastError: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)

@Entity(
    tableName = "list_items",
    indices = [
        Index("remoteId", unique = true),
        Index("messageLocalId", "position")
    ]
)
data class ListItemEntity(
    @PrimaryKey val localId: String,
    val remoteId: String?,
    val messageLocalId: String,
    val text: String,
    val isChecked: Boolean,
    val position: Double,
    val syncStatus: String,
    val lastError: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)

@Entity(
    tableName = "attachments",
    indices = [
        Index("remoteId", unique = true),
        Index("messageLocalId"),
        Index("uploadStatus")
    ]
)
data class AttachmentEntity(
    @PrimaryKey val localId: String,
    val remoteId: String?,
    val messageLocalId: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val fileKind: String?,
    val extension: String?,
    val compressed: Boolean?,
    val localPath: String?,
    val remoteUrl: String?,
    val previewLocalPath: String?,
    val previewUrl: String?,
    val uploadStatus: String,
    val syncStatus: String,
    val lastError: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)

@Entity(
    tableName = "sync_queue",
    indices = [
        Index("status", "nextAttemptAt"),
        Index("entityLocalId"),
        Index("dependsOnId")
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val opType: String,
    val entity: String,
    val entityLocalId: String,
    val parentLocalId: String?,
    val payload: String,
    val dependsOnId: Long?,
    val status: String,
    val attempts: Int,
    val nextAttemptAt: Long,
    val lastError: String?,
    val createdAt: Long
)

@Entity(tableName = "sync_state", primaryKeys = ["entity"])
data class SyncStateEntity(
    val entity: String,
    val lastPulledAt: Long,
    val cursor: String?
)

data class MessageWithChildren(
    @ColumnInfo(name = "messageLocalId") val messageLocalId: String,
    @ColumnInfo(name = "messageRemoteId") val messageRemoteId: String?,
    val folderLocalId: String,
    val type: String,
    val content: String,
    val isFavorite: Boolean,
    @ColumnInfo(name = "messageSyncStatus") val messageSyncStatus: String,
    @ColumnInfo(name = "messageLastError") val messageLastError: String?,
    @ColumnInfo(name = "messageCreatedAt") val messageCreatedAt: Long,
    @ColumnInfo(name = "messageUpdatedAt") val messageUpdatedAt: Long,
    @ColumnInfo(name = "listLocalId") val listLocalId: String?,
    @ColumnInfo(name = "listRemoteId") val listRemoteId: String?,
    val text: String?,
    val isChecked: Boolean?,
    val position: Double?,
    @ColumnInfo(name = "listSyncStatus") val listSyncStatus: String?,
    @ColumnInfo(name = "listLastError") val listLastError: String?,
    @ColumnInfo(name = "listCreatedAt") val listCreatedAt: Long?,
    @ColumnInfo(name = "listUpdatedAt") val listUpdatedAt: Long?,
    @ColumnInfo(name = "attachmentLocalId") val attachmentLocalId: String?,
    @ColumnInfo(name = "attachmentRemoteId") val attachmentRemoteId: String?,
    val fileName: String?,
    val mimeType: String?,
    val size: Long?,
    val fileKind: String?,
    val extension: String?,
    val compressed: Boolean?,
    val localPath: String?,
    val remoteUrl: String?,
    val previewLocalPath: String?,
    val previewUrl: String?,
    val uploadStatus: String?,
    @ColumnInfo(name = "attachmentSyncStatus") val attachmentSyncStatus: String?,
    @ColumnInfo(name = "attachmentLastError") val attachmentLastError: String?,
    @ColumnInfo(name = "attachmentCreatedAt") val attachmentCreatedAt: Long?,
    @ColumnInfo(name = "attachmentUpdatedAt") val attachmentUpdatedAt: Long?
)

data class MessageEntityWithChildren(
    @Embedded val message: MessageEntity,
    @Relation(
        parentColumn = "localId",
        entityColumn = "messageLocalId"
    )
    val listItems: List<ListItemEntity>,
    @Relation(
        parentColumn = "localId",
        entityColumn = "messageLocalId"
    )
    val attachments: List<AttachmentEntity>
)
