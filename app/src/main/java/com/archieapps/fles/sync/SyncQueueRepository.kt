package com.archieapps.fles.sync

import com.archieapps.fles.data.local.db.QueueStatus
import com.archieapps.fles.data.local.db.SyncQueueDao
import com.archieapps.fles.data.local.db.SyncQueueEntity
import com.archieapps.fles.data.local.db.nowMs

class SyncQueueRepository(private val dao: SyncQueueDao) {
    val pendingCount = dao.observePendingCount()

    suspend fun enqueue(
        opType: String,
        entity: String,
        entityLocalId: String,
        parentLocalId: String? = null,
        payload: String = "{}",
        dependsOnId: Long? = null
    ): Long {
        val now = nowMs()
        return dao.insert(
            SyncQueueEntity(
                opType = opType,
                entity = entity,
                entityLocalId = entityLocalId,
                parentLocalId = parentLocalId,
                payload = payload,
                dependsOnId = dependsOnId,
                status = QueueStatus.Queued,
                attempts = 0,
                nextAttemptAt = now,
                lastError = null,
                createdAt = now
            )
        )
    }

    suspend fun lastPendingOpId(entityLocalId: String): Long? {
        return dao.lastPendingForEntity(entityLocalId)?.id
    }

    suspend fun retryEntity(entityLocalId: String): Int {
        val rows = dao.pendingForEntity(entityLocalId)
        val now = nowMs()
        rows.forEach { dao.update(it.copy(status = QueueStatus.Queued, attempts = 0, nextAttemptAt = now, lastError = null)) }
        return rows.size
    }
}
