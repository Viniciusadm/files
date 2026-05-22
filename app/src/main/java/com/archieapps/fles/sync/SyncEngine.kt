package com.archieapps.fles.sync

import com.archieapps.fles.data.local.db.QueueStatus
import com.archieapps.fles.data.local.db.SyncQueueDao
import com.archieapps.fles.data.local.db.SyncStateDao
import com.archieapps.fles.data.local.db.SyncStateEntity
import com.archieapps.fles.data.local.db.nowMs
import com.archieapps.fles.data.repository.ChannelsRepository
import com.archieapps.fles.data.repository.CryptoRepository
import com.archieapps.fles.data.repository.MessagesRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.min
import kotlin.random.Random

class SyncEngine(
    private val cryptoRepository: CryptoRepository,
    private val networkMonitor: NetworkMonitor,
    private val statusStore: SyncStatusStore,
    private val queueDao: SyncQueueDao,
    private val syncStateDao: SyncStateDao,
    private val channelsRepository: ChannelsRepository,
    private val messagesRepository: MessagesRepository
) {
    @Volatile private var running = false

    init {
        queueDao.observePendingCount().onEach { statusStore.setPending(it) }.launchIn(kotlinx.coroutines.GlobalScope)
        networkMonitor.online.onEach {
            statusStore.setOnline(it)
            if (it) runCatching { runOnce() }
        }.launchIn(kotlinx.coroutines.GlobalScope)
    }

    suspend fun runOnce() {
        if (running) return
        running = true
        try {
            if (!cryptoRepository.hasKey()) {
                statusStore.setGlobal(GlobalSyncState.WaitingKey)
                return
            }
            if (!networkMonitor.isOnline()) {
                statusStore.setGlobal(GlobalSyncState.Offline)
                return
            }
            statusStore.setGlobal(GlobalSyncState.Syncing)
            drainQueue()
            channelsRepository.refreshFromRemote()
            syncStateDao.upsert(SyncStateEntity("channels", nowMs(), null))
            (channelsRepository.getChannels() + channelsRepository.getSecureChannels()).forEach { channel ->
                runCatching {
                    messagesRepository.refreshMessagesFromRemote(channel.id)
                    syncStateDao.upsert(SyncStateEntity("messages:${channel.id}", nowMs(), null))
                }
            }
            queueDao.cleanupDone()
            statusStore.markSynced()
        } catch (error: Throwable) {
            statusStore.setGlobal(GlobalSyncState.Failed)
            throw error
        } finally {
            running = false
        }
    }

    private suspend fun drainQueue() {
        var safetyCounter = 0
        while (safetyCounter++ < 500) {
            val op = nextReadyOp() ?: break
            queueDao.update(op.copy(status = QueueStatus.InFlight))
            runCatching {
                when ("${op.entity}.${op.opType}") {
                    "channel.create" -> channelsRepository.pushCreate(op.entityLocalId)
                    "channel.delete" -> channelsRepository.pushDelete(op.entityLocalId)
                    "message.create" -> messagesRepository.pushMessageCreate(op.entityLocalId)
                    "message.delete" -> messagesRepository.pushMessageDelete(op.entityLocalId)
                    "message.favorite" -> messagesRepository.pushMessageFavorite(op.entityLocalId)
                    "message.move" -> messagesRepository.pushMessageMove(op.entityLocalId)
                    "message.reorder" -> messagesRepository.pushReorder(op.entityLocalId)
                    "listItem.create" -> messagesRepository.pushListItem(op.entityLocalId, "create")
                    "listItem.update" -> messagesRepository.pushListItem(op.entityLocalId, "update")
                    "listItem.toggle" -> messagesRepository.pushListItem(op.entityLocalId, "toggle")
                    "listItem.delete" -> messagesRepository.pushListItem(op.entityLocalId, "delete")
                    else -> throw IllegalStateException("Operação não suportada: ${op.entity}.${op.opType}")
                }
            }.onSuccess {
                queueDao.update(op.copy(status = QueueStatus.Done, lastError = null))
            }.onFailure { error ->
                val attempts = op.attempts + 1
                val message = error.message ?: error.toString()
                val next = nowMs() + backoff(attempts)
                queueDao.update(op.copy(status = QueueStatus.Queued, attempts = attempts, nextAttemptAt = next, lastError = message))
                when (op.entity) {
                    "channel" -> channelsRepository.markFailed(op.entityLocalId, message)
                    else -> messagesRepository.markFailed(op.entity, op.entityLocalId, message)
                }
                return
            }
        }
    }

    private suspend fun nextReadyOp() = queueDao.ready(nowMs()).firstOrNull { op ->
        val parentId = op.dependsOnId
        parentId == null || queueDao.byId(parentId)?.status == QueueStatus.Done
    }

    private fun backoff(attempts: Int): Long {
        val base = min((1 shl min(attempts, 10)) * 1000L, 5 * 60 * 1000L)
        return (base * (0.5 + Random.nextDouble() * 0.5)).toLong()
    }
}
