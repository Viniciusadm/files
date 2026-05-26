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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChannelsRepository(
    private val apiService: ApiService,
    private val cryptoRepository: CryptoRepository,
    private val secureFoldersRepository: SecureFoldersRepository,
    private val channelDao: ChannelDao,
    private val queueRepository: SyncQueueRepository,
    private val scheduler: SyncScheduler
) {
    fun observeChannels(): Flow<List<Channel>> {
        return channelDao.observeActive(false).map { rows ->
            rows.mapNotNull { if (cryptoRepository.looksLikeEnvelope(it.name)) null else it.toUi() }
        }
    }

    fun observeSecureChannels(): Flow<List<Channel>> {
        return combine(channelDao.observeSecureActive(), secureFoldersRepository.state) { rows, secureState ->
            if (!secureState.unlocked) emptyList() else rows.mapNotNull { it.toSecureUiOrNull() }
        }
    }

    suspend fun getChannels(): List<Channel> {
        return channelDao.active(false).mapNotNull { if (cryptoRepository.looksLikeEnvelope(it.name)) null else it.toUi() }
    }

    suspend fun getSecureChannels(): List<Channel> {
        if (!secureFoldersRepository.state.value.unlocked) return emptyList()
        return channelDao.secureActive().mapNotNull { it.toSecureUiOrNull() }
    }

    suspend fun getChannel(id: String): Channel? {
        val row = channelDao.byLocalId(id) ?: return null
        if (row.isSecure && !secureFoldersRepository.state.value.unlocked) return null
        return if (row.isSecure) row.toSecureUiOrNull() else if (cryptoRepository.looksLikeEnvelope(row.name)) null else row.toUi()
    }

    suspend fun getRemoteId(localId: String): String? {
        return channelDao.byLocalId(localId)?.remoteId
    }

    suspend fun isSecure(localId: String): Boolean {
        return channelDao.byLocalId(localId)?.isSecure == true
    }

    suspend fun createChannel(name: String, icon: String, color: String, isSecure: Boolean = false, parentLocalId: String? = null): Channel {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Informe um nome" }
        val parent = parentLocalId?.let { channelDao.byLocalId(it) }
        require(parentLocalId == null || parent != null) { "Pasta pai inválida." }
        val inheritedSecure = parent?.isSecure ?: isSecure
        if (inheritedSecure && !secureFoldersRepository.state.value.unlocked) throw IllegalStateException("Pastas seguras bloqueadas.")
        val now = nowMs()
        val localId = UUID.randomUUID().toString()
        val storedName = if (inheritedSecure) secureFoldersRepository.encryptString(trimmed) else trimmed
        val entity = ChannelEntity(
            localId = localId,
            remoteId = null,
            name = storedName,
            icon = icon,
            color = color,
            isSecure = inheritedSecure,
            parentLocalId = parentLocalId,
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
            payload = "{\"name\":\"${trimmed.jsonEscape()}\",\"icon\":\"${icon.jsonEscape()}\",\"color\":\"${color.jsonEscape()}\",\"isSecure\":$inheritedSecure,\"parentLocalId\":${parentLocalId?.let { "\"${it.jsonEscape()}\"" } ?: "null"}}",
            dependsOnId = parentLocalId?.let { queueRepository.lastPendingOpId(it) }
        )
        scheduler.scheduleNow()
        return if (inheritedSecure) entity.toSecureUi() else entity.toUi()
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
        remote.forEach { updateParentFromRemote(it) }
        channelDao.active(true).forEach { local ->
            if (local.remoteId != null && local.remoteId !in remoteIds && local.syncStatus == LocalSyncStatus.Synced) {
                val now = nowMs()
                channelDao.update(local.copy(deletedAt = now, updatedAt = now))
            }
        }
    }

    suspend fun pushCreate(localId: String) {
        val local = channelDao.byLocalId(localId) ?: return
        val parentRemoteId = local.parentLocalId?.let { parentLocalId ->
            channelDao.byLocalId(parentLocalId)?.remoteId ?: throw IllegalStateException("Pasta pai ainda sem remoteId")
        }
        val remote = apiService.createFolder(
            CreateFolderRequest(
                name = if (local.isSecure) local.name else cryptoRepository.encryptString(local.name),
                parentId = parentRemoteId,
                icon = local.icon ?: "Folder",
                color = local.color ?: "#3390ec",
                isSecure = local.isSecure
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
            val staleEncryption = !existing.isSecure && cryptoRepository.looksLikeEnvelope(existing.name)
            if (existing.syncStatus == LocalSyncStatus.Synced && (remoteUpdatedAt > existing.updatedAt || staleEncryption)) {
                channelDao.update(
                    existing.copy(
                        name = if (remote.isSecure == true) remote.name.orEmpty() else cryptoRepository.tryDecryptString(remote.name),
                        icon = remote.icon ?: existing.icon,
                        color = remote.color ?: existing.color,
                        isSecure = remote.isSecure == true,
                        parentLocalId = remote.parentId?.let { channelDao.byRemoteId(it)?.localId },
                        lastMessagePreview = if (remote.isSecure == true) remote.lastMessage else cryptoRepository.tryDecryptString(remote.lastMessage),
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
                name = if (remote.isSecure == true) remote.name.orEmpty() else cryptoRepository.tryDecryptString(remote.name),
                icon = remote.icon,
                color = remote.color,
                isSecure = remote.isSecure == true,
                parentLocalId = remote.parentId?.let { channelDao.byRemoteId(it)?.localId },
                lastMessagePreview = if (remote.isSecure == true) remote.lastMessage else cryptoRepository.tryDecryptString(remote.lastMessage),
                syncStatus = LocalSyncStatus.Synced,
                lastError = null,
                createdAt = now,
                updatedAt = remoteUpdatedAt,
                deletedAt = null
            )
        )
        return localId
    }

    private suspend fun updateParentFromRemote(remote: RemoteChannel) {
        val remoteId = remote.id ?: return
        val local = channelDao.byRemoteId(remoteId) ?: return
        val parentLocalId = remote.parentId?.let { channelDao.byRemoteId(it)?.localId }
        if (local.parentLocalId != parentLocalId) {
            channelDao.update(local.copy(parentLocalId = parentLocalId))
        }
    }

    private fun ChannelEntity.toSecureUi(): Channel {
        return copy(
            name = secureFoldersRepository.tryDecryptString(name),
            lastMessagePreview = secureFoldersRepository.tryDecryptString(lastMessagePreview)
        ).toUi()
    }

    private fun ChannelEntity.toSecureUiOrNull(): Channel? {
        val decryptedName = secureFoldersRepository.tryDecryptStringOrNull(name) ?: return null
        val decryptedPreview = secureFoldersRepository.tryDecryptStringOrNull(lastMessagePreview) ?: ""
        return copy(name = decryptedName, lastMessagePreview = decryptedPreview).toUi()
    }
}

fun String.jsonEscape(): String = replace("\\", "\\\\").replace("\"", "\\\"")
