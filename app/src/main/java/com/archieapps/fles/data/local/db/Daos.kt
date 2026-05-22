package com.archieapps.fles.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clear()
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Query("DELETE FROM sessions")
    suspend fun clear()
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun active(): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE localId = :localId LIMIT 1")
    suspend fun byLocalId(localId: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE remoteId = :remoteId LIMIT 1")
    suspend fun byRemoteId(remoteId: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(channel: ChannelEntity)

    @Update
    suspend fun update(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE localId = :localId")
    suspend fun delete(localId: String)

    @Query("DELETE FROM channels")
    suspend fun clear()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE folderLocalId = :folderLocalId AND deletedAt IS NULL ORDER BY createdAt ASC")
    fun observeByFolder(folderLocalId: String): Flow<List<MessageEntity>>

    @Transaction
    @Query("SELECT * FROM messages WHERE folderLocalId = :folderLocalId AND deletedAt IS NULL ORDER BY createdAt ASC")
    fun observeByFolderWithChildren(folderLocalId: String): Flow<List<MessageEntityWithChildren>>

    @Query("SELECT * FROM messages WHERE folderLocalId = :folderLocalId AND deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun byFolder(folderLocalId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<MessageEntity>>

    @Transaction
    @Query("SELECT * FROM messages WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeActiveWithChildren(): Flow<List<MessageEntityWithChildren>>

    @Query("SELECT * FROM messages WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun active(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE localId = :localId LIMIT 1")
    suspend fun byLocalId(localId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE remoteId = :remoteId LIMIT 1")
    suspend fun byRemoteId(remoteId: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Update
    suspend fun update(message: MessageEntity)

    @Query("DELETE FROM messages WHERE localId = :localId")
    suspend fun delete(localId: String)

    @Query("DELETE FROM messages")
    suspend fun clear()
}

@Dao
interface ListItemDao {
    @Query("SELECT * FROM list_items WHERE messageLocalId = :messageLocalId AND deletedAt IS NULL ORDER BY position ASC")
    fun observeByMessage(messageLocalId: String): Flow<List<ListItemEntity>>

    @Query("SELECT * FROM list_items WHERE messageLocalId = :messageLocalId AND deletedAt IS NULL ORDER BY position ASC")
    suspend fun byMessage(messageLocalId: String): List<ListItemEntity>

    @Query("SELECT * FROM list_items WHERE messageLocalId = :messageLocalId ORDER BY position ASC")
    suspend fun allByMessage(messageLocalId: String): List<ListItemEntity>

    @Query("SELECT * FROM list_items WHERE localId = :localId LIMIT 1")
    suspend fun byLocalId(localId: String): ListItemEntity?

    @Query("SELECT * FROM list_items WHERE remoteId = :remoteId LIMIT 1")
    suspend fun byRemoteId(remoteId: String): ListItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ListItemEntity)

    @Update
    suspend fun update(item: ListItemEntity)

    @Query("DELETE FROM list_items WHERE localId = :localId")
    suspend fun delete(localId: String)

    @Query("DELETE FROM list_items WHERE messageLocalId = :messageLocalId")
    suspend fun deleteByMessage(messageLocalId: String)

    @Query("DELETE FROM list_items")
    suspend fun clear()
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE messageLocalId = :messageLocalId AND deletedAt IS NULL")
    fun observeByMessage(messageLocalId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE messageLocalId = :messageLocalId AND deletedAt IS NULL")
    suspend fun byMessage(messageLocalId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE messageLocalId = :messageLocalId")
    suspend fun allByMessage(messageLocalId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE deletedAt IS NULL")
    suspend fun active(): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE localId = :localId LIMIT 1")
    suspend fun byLocalId(localId: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE remoteId = :remoteId LIMIT 1")
    suspend fun byRemoteId(remoteId: String): AttachmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attachment: AttachmentEntity)

    @Update
    suspend fun update(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE localId = :localId")
    suspend fun delete(localId: String)

    @Query("DELETE FROM attachments WHERE messageLocalId = :messageLocalId")
    suspend fun deleteByMessage(messageLocalId: String)

    @Query("DELETE FROM attachments")
    suspend fun clear()
}

@Dao
interface SyncQueueDao {
    @Insert
    suspend fun insert(op: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue WHERE status != 'done' ORDER BY id DESC")
    suspend fun pending(): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status != 'done'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM sync_queue WHERE status = 'queued' AND nextAttemptAt <= :now ORDER BY id ASC")
    suspend fun ready(now: Long): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): SyncQueueEntity?

    @Query("SELECT * FROM sync_queue WHERE entityLocalId = :entityLocalId AND status != 'done' ORDER BY id DESC LIMIT 1")
    suspend fun lastPendingForEntity(entityLocalId: String): SyncQueueEntity?

    @Query("SELECT * FROM sync_queue WHERE entityLocalId = :entityLocalId AND status != 'done'")
    suspend fun pendingForEntity(entityLocalId: String): List<SyncQueueEntity>

    @Update
    suspend fun update(op: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM sync_queue WHERE status = 'done'")
    suspend fun cleanupDone()

    @Query("DELETE FROM sync_queue")
    suspend fun clear()
}

@Dao
interface SyncStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncStateEntity)

    @Query("DELETE FROM sync_state")
    suspend fun clear()
}
