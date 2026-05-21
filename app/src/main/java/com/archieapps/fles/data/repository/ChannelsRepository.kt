package com.archieapps.fles.data.repository

import com.archieapps.fles.data.local.db.ChannelDao
import com.archieapps.fles.data.local.db.ChannelEntity
import com.archieapps.fles.data.local.db.LocalSyncStatus
import com.archieapps.fles.data.local.db.nowMs
import com.archieapps.fles.data.local.db.parseRemoteDate
import com.archieapps.fles.data.remote.ApiService
import com.archieapps.fles.data.remote.CreateFolderRequest
import com.archieapps.fles.data.remote.RemoteChannel
import com.archieapps.fles.data.remote.requireBody
import com.archieapps.fles.data.remote.requireSuccess
import com.archieapps.fles.sync.SyncQueueRepository
import com.archieapps.fles.sync.SyncScheduler
import com.archieapps.fles.ui.model.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChannelsRepository(
    private val apiService: ApiService,
    private val cryptoRepository: CryptoRepository,
    private val channelDao: ChannelDao,
    private val queueRepository: SyncQueueRepository,
    private val scheduler: SyncScheduler
) {
    fun observeChannels(): Flow<List<Channel>> {
        return channelDao.observeActive().map { rows -> rows.map { it.toUi() } }
    }

    suspend fun getChannels(): List<Channel> {
        return channelDao.active().map { it.toUi() }
    }

    suspend fun getChannel(id: String): Channel? {
        return channelDao.byLocalId(id)?.toUi()
    }

    suspend fun getRemoteId(localId: String): String? {
        return channelDao.byLocalId(localId)?.remoteId
    }

    suspend fun createChannel(name: String, icon: String, color: String): Channel {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Informe um nome" }
        val now = nowMs()
        val localId = UUID.randomUUID().toString()
        val entity = ChannelEntity(
            localId = localId,
            remoteId = null,
            name = trimmed,
            icon = icon,
            color = color,
            parentLocalId = null,
            lastMessagePreview = null,
            syncStatus = LocalSyncStatus.PendingCreate,
            lastError = null,
            createdAt = now,
            updatedAt = now,
            deletedAt = null
        )
        channelDao.upsert(entity)
        queueRepository.enqueue(
            opType = "create",
            entity = "channel",
            entityLocalId = localId,
            payload = "{\"name\":\"${trimmed.jsonEscape()}\",\"icon\":\"${icon.jsonEscape()}\",\"color\":\"${color.jsonEscape()}\"}"
        )
        scheduler.scheduleNow()
        return entity.toUi()
    }

    suspend fun deleteChannel(id: String) {
        val row = channelDao.byLocalId(id) ?: return
        val now = nowMs()
        channelDao.update(row.copy(syncStatus = LocalSyncStatus.PendingDelete, updatedAt = now, deletedAt = now, lastError = null))
        queueRepository.enqueue(opType = "delete", entity = "channel", entityLocalId = id)
        scheduler.scheduleNow()
    }

    suspend fun refreshFromRemote() {
        val remote = apiService.channels().requireBody()
        val remoteIds = remote.mapNotNull { it.id }.toSet()
        remote.forEach { upsertFromRemote(it) }
        channelDao.active().forEach { local ->
            if (local.remoteId != null && local.remoteId !in remoteIds && local.syncStatus == LocalSyncStatus.Synced) {
                val now = nowMs()
                channelDao.update(local.copy(deletedAt = now, updatedAt = now))
            }
        }
    }

    suspend fun pushCreate(localId: String) {
        val local = channelDao.byLocalId(localId) ?: return
        val remote = apiService.createFolder(
            CreateFolderRequest(
                name = cryptoRepository.encryptString(local.name),
                parentId = null,
                icon = local.icon ?: "Folder",
                color = local.color ?: "#3390ec"
            )
        ).requireBody()
        val remoteId = remote.id ?: return
        channelDao.update(
            local.copy(
                remoteId = remoteId,
                updatedAt = parseRemoteDate(remote.updatedAt),
                syncStatus = LocalSyncStatus.Synced,
                lastError = null
            )
        )
    }

    suspend fun pushDelete(localId: String) {
        val local = channelDao.byLocalId(localId) ?: return
        val remoteId = local.remoteId
        if (remoteId != null) apiService.deleteFolder(remoteId).requireSuccess()
        channelDao.delete(localId)
    }

    suspend fun markFailed(localId: String, error: String) {
        val local = channelDao.byLocalId(localId) ?: return
        channelDao.update(local.copy(syncStatus = LocalSyncStatus.Failed, lastError = error))
    }

    private suspend fun upsertFromRemote(remote: RemoteChannel): String {
        val remoteId = remote.id ?: return ""
        val remoteUpdatedAt = parseRemoteDate(remote.updatedAt)
        val existing = channelDao.byRemoteId(remoteId)
        if (existing != null) {
            if (existing.syncStatus == LocalSyncStatus.Synced && remoteUpdatedAt > existing.updatedAt) {
                channelDao.update(
                    existing.copy(
                        name = cryptoRepository.tryDecryptString(remote.name),
                        icon = remote.icon ?: existing.icon,
                        color = remote.color ?: existing.color,
                        lastMessagePreview = cryptoRepository.tryDecryptString(remote.lastMessage),
                        updatedAt = remoteUpdatedAt,
                        lastError = null
                    )
                )
            }
            return existing.localId
        }
        val now = nowMs()
        val localId = UUID.randomUUID().toString()
        channelDao.upsert(
            ChannelEntity(
                localId = localId,
                remoteId = remoteId,
                name = cryptoRepository.tryDecryptString(remote.name),
                icon = remote.icon,
                color = remote.color,
                parentLocalId = null,
                lastMessagePreview = cryptoRepository.tryDecryptString(remote.lastMessage),
                syncStatus = LocalSyncStatus.Synced,
                lastError = null,
                createdAt = now,
                updatedAt = remoteUpdatedAt,
                deletedAt = null
            )
        )
        return localId
    }
}

fun String.jsonEscape(): String = replace("\\", "\\\\").replace("\"", "\\\"")
