package com.archieapps.fles.data.repository

import com.archieapps.fles.data.local.db.AttachmentDao
import com.archieapps.fles.data.local.db.AttachmentEntity
import com.archieapps.fles.data.local.db.ListItemDao
import com.archieapps.fles.data.local.db.ListItemEntity
import com.archieapps.fles.data.local.db.LocalSyncStatus
import com.archieapps.fles.data.local.db.LocalUploadStatus
import com.archieapps.fles.data.local.db.MessageDao
import com.archieapps.fles.data.local.db.MessageEntity
import com.archieapps.fles.data.local.db.nowMs
import com.archieapps.fles.data.local.db.parseRemoteDate
import com.archieapps.fles.data.remote.AddListItemRequest
import com.archieapps.fles.data.remote.ApiService
import com.archieapps.fles.data.remote.ChecklistItemRequest
import com.archieapps.fles.data.remote.CreateChecklistRequest
import com.archieapps.fles.data.remote.CreateMessageRequest
import com.archieapps.fles.data.remote.MoveMessageRequest
import com.archieapps.fles.data.remote.RemoteAttachment
import com.archieapps.fles.data.remote.RemoteListItem
import com.archieapps.fles.data.remote.RemoteMessage
import com.archieapps.fles.data.remote.ReorderListItemsRequest
import com.archieapps.fles.data.remote.UpdateListItemRequest
import com.archieapps.fles.data.remote.requireBody
import com.archieapps.fles.data.remote.requireSuccess
import com.archieapps.fles.sync.NetworkMonitor
import com.archieapps.fles.sync.SyncQueueRepository
import com.archieapps.fles.sync.SyncScheduler
import com.archieapps.fles.ui.model.Attachment
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.ui.model.FileKind
import com.archieapps.fles.ui.model.Message
import com.archieapps.fles.ui.model.PendingAttachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesRepository(
    private val apiService: ApiService,
    private val cryptoRepository: CryptoRepository,
    private val channelsRepository: ChannelsRepository,
    private val attachmentIo: AttachmentIo,
    private val messageDao: MessageDao,
    private val listItemDao: ListItemDao,
    private val attachmentDao: AttachmentDao,
    private val queueRepository: SyncQueueRepository,
    private val scheduler: SyncScheduler,
    private val networkMonitor: NetworkMonitor
) {
    fun observeMessages(folderId: String): Flow<List<Message>> {
        return messageDao.observeByFolder(folderId).mapLatest { rows ->
            rows.map { message ->
                message.toUi(
                    listItemDao.byMessage(message.localId).map { it.toUi() },
                    attachmentDao.byMessage(message.localId).map { it.toUi() }
                )
            }
        }
    }

    fun observeAllMessages(): Flow<List<Message>> {
        return messageDao.observeActive().mapLatest { rows ->
            rows.map { message ->
                message.toUi(
                    listItemDao.byMessage(message.localId).map { it.toUi() },
                    attachmentDao.byMessage(message.localId).map { it.toUi() }
                )
            }
        }
    }

    suspend fun getMessages(folderId: String): List<Message> {
        return messageDao.byFolder(folderId).map { message ->
            message.toUi(
                listItemDao.byMessage(message.localId).map { it.toUi() },
                attachmentDao.byMessage(message.localId).map { it.toUi() }
            )
        }
    }

    suspend fun search(query: String): List<SearchResult> {
        val term = query.trim()
        if (term.length < 2) return emptyList()
        val channels = channelsRepository.getChannels()
        return channels.flatMap { channel ->
            getMessages(channel.id).mapNotNull { message ->
                if (message.content.contains(term, ignoreCase = true) || channel.name.contains(term, ignoreCase = true)) {
                    SearchResult(channel, message)
                } else {
                    null
                }
            }
        }.sortedByDescending { it.message.createdAt }
    }

    suspend fun sendText(folderId: String, content: String): Message {
        return send(folderId, content, emptyList())
    }

    suspend fun send(folderId: String, content: String, attachments: List<PendingAttachment>): Message {
        val trimmed = content.trim()
        require(trimmed.isNotBlank() || attachments.isNotEmpty()) { "Mensagem vazia" }
        val now = nowMs()
        val localId = UUID.randomUUID().toString()
        val type = when {
            attachments.isEmpty() -> "text"
            attachments.any { it.isImage } && attachments.any { !it.isImage } -> "mixed"
            attachments.all { it.isImage } -> "image"
            else -> "document"
        }
        val message = MessageEntity(localId, null, folderId, type, trimmed, false, LocalSyncStatus.PendingCreate, null, now, now, null)
        messageDao.upsert(message)
        attachments.forEach { persistAttachment(localId, it) }
        queueRepository.enqueue(
            opType = "create",
            entity = "message",
            entityLocalId = localId,
            parentLocalId = folderId,
            payload = "{\"type\":\"$type\",\"hasAttachments\":${attachments.isNotEmpty()}}",
            dependsOnId = queueRepository.lastPendingOpId(folderId)
        )
        scheduler.scheduleNow()
        return message.toUi(emptyList(), attachmentDao.byMessage(localId).map { it.toUi() })
    }

    suspend fun createChecklist(folderId: String, title: String, items: List<String>): Message {
        val filled = items.map { it.trim() }.filter { it.isNotBlank() }
        require(filled.isNotEmpty()) { "Adicione ao menos um item" }
        val now = nowMs()
        val localId = UUID.randomUUID().toString()
        val message = MessageEntity(localId, null, folderId, "list", title.trim(), false, LocalSyncStatus.PendingCreate, null, now, now, null)
        messageDao.upsert(message)
        filled.forEachIndexed { index, text ->
            listItemDao.upsert(
                ListItemEntity(
                    localId = UUID.randomUUID().toString(),
                    remoteId = null,
                    messageLocalId = localId,
                    text = text,
                    isChecked = false,
                    position = (index + 1).toDouble(),
                    syncStatus = LocalSyncStatus.Synced,
                    lastError = null,
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null
                )
            )
        }
        queueRepository.enqueue(
            opType = "create",
            entity = "message",
            entityLocalId = localId,
            parentLocalId = folderId,
            payload = "{\"type\":\"list\"}",
            dependsOnId = queueRepository.lastPendingOpId(folderId)
        )
        scheduler.scheduleNow()
        return message.toUi(listItemDao.byMessage(localId).map { it.toUi() }, emptyList())
    }

    suspend fun deleteMessage(id: String) {
        val row = messageDao.byLocalId(id) ?: return
        val now = nowMs()
        messageDao.update(row.copy(syncStatus = LocalSyncStatus.PendingDelete, updatedAt = now, deletedAt = now))
        queueRepository.enqueue(opType = "delete", entity = "message", entityLocalId = id, parentLocalId = row.folderLocalId)
        scheduler.scheduleNow()
    }

    suspend fun toggleFavorite(id: String): Message? {
        val row = messageDao.byLocalId(id) ?: return null
        val now = nowMs()
        val updated = row.copy(isFavorite = !row.isFavorite, syncStatus = LocalSyncStatus.PendingUpdate, updatedAt = now, lastError = null)
        messageDao.update(updated)
        queueRepository.enqueue(opType = "favorite", entity = "message", entityLocalId = id, parentLocalId = row.folderLocalId, dependsOnId = queueRepository.lastPendingOpId(id))
        scheduler.scheduleNow()
        return updated.toUi(listItemDao.byMessage(id).map { it.toUi() }, attachmentDao.byMessage(id).map { it.toUi() })
    }

    suspend fun moveMessage(id: String, targetFolderId: String): Message? {
        val row = messageDao.byLocalId(id) ?: return null
        val now = nowMs()
        val updated = row.copy(folderLocalId = targetFolderId, syncStatus = LocalSyncStatus.PendingUpdate, updatedAt = now, lastError = null)
        messageDao.update(updated)
        queueRepository.enqueue(opType = "move", entity = "message", entityLocalId = id, parentLocalId = targetFolderId, dependsOnId = queueRepository.lastPendingOpId(id))
        scheduler.scheduleNow()
        return updated.toUi(listItemDao.byMessage(id).map { it.toUi() }, attachmentDao.byMessage(id).map { it.toUi() })
    }

    suspend fun addListItem(messageId: String, text: String) {
        val trimmed = text.trim()
        require(trimmed.isNotBlank()) { "Item vazio" }
        val now = nowMs()
        val itemId = UUID.randomUUID().toString()
        listItemDao.upsert(ListItemEntity(itemId, null, messageId, trimmed, false, now.toDouble(), LocalSyncStatus.PendingCreate, null, now, now, null))
        queueRepository.enqueue("create", "listItem", itemId, messageId, dependsOnId = queueRepository.lastPendingOpId(messageId))
        scheduler.scheduleNow()
    }

    suspend fun updateListItem(messageId: String, itemId: String, text: String? = null, isChecked: Boolean? = null) {
        val row = listItemDao.byLocalId(itemId) ?: return
        val now = nowMs()
        listItemDao.update(row.copy(text = text?.trim()?.takeIf { it.isNotBlank() } ?: row.text, isChecked = isChecked ?: row.isChecked, syncStatus = LocalSyncStatus.PendingUpdate, updatedAt = now, lastError = null))
        queueRepository.enqueue("update", "listItem", itemId, messageId, dependsOnId = queueRepository.lastPendingOpId(itemId))
        scheduler.scheduleNow()
    }

    suspend fun toggleListItem(messageId: String, itemId: String) {
        val row = listItemDao.byLocalId(itemId) ?: return
        updateListItem(messageId, itemId, isChecked = !row.isChecked)
    }

    suspend fun deleteListItem(messageId: String, itemId: String) {
        val row = listItemDao.byLocalId(itemId) ?: return
        val now = nowMs()
        listItemDao.update(row.copy(syncStatus = LocalSyncStatus.PendingDelete, updatedAt = now, deletedAt = now))
        queueRepository.enqueue("delete", "listItem", itemId, messageId, dependsOnId = queueRepository.lastPendingOpId(itemId))
        scheduler.scheduleNow()
    }

    suspend fun reorderListItems(messageId: String, orderedIds: List<String>) {
        val now = nowMs()
        orderedIds.forEachIndexed { index, id ->
            listItemDao.byLocalId(id)?.let { listItemDao.update(it.copy(position = ((index + 1) * 1000).toDouble(), syncStatus = LocalSyncStatus.PendingUpdate, updatedAt = now)) }
        }
        queueRepository.enqueue("reorder", "message", messageId, dependsOnId = queueRepository.lastPendingOpId(messageId))
        scheduler.scheduleNow()
    }

    suspend fun downloadAttachment(attachment: Attachment): File {
        attachment.localPath?.let {
            val file = File(it.removePrefix("file://"))
            if (file.exists()) return file
        }
        if (!networkMonitor.isOnline()) throw IllegalStateException("Arquivo indisponível offline. Baixe quando houver conexão.")
        val entity = attachmentDao.byLocalId(attachment.id) ?: throw IllegalStateException("Anexo não encontrado")
        attachmentDao.update(entity.copy(syncStatus = LocalSyncStatus.Downloading, lastError = null))
        return runCatching {
            val bytes = cryptoRepository.decryptBytesFromFile(apiService.download(normalizeDownloadUrl(entity.remoteUrl.orEmpty())).bytes())
            val file = attachmentIo.persistDownloadedFile(entity.localId, entity.fileName, bytes)
            attachmentDao.update(entity.copy(localPath = file.absolutePath, syncStatus = LocalSyncStatus.Synced, lastError = null, updatedAt = nowMs()))
            file
        }.getOrElse {
            attachmentDao.update(entity.copy(syncStatus = LocalSyncStatus.Failed, lastError = it.message))
            throw it
        }
    }

    suspend fun downloadAttachment(url: String): ByteArray {
        if (!networkMonitor.isOnline()) throw IllegalStateException("Arquivo indisponível offline. Baixe quando houver conexão.")
        return cryptoRepository.decryptBytesFromFile(apiService.download(normalizeDownloadUrl(url)).bytes())
    }

    fun cachePlainFile(fileName: String, bytes: ByteArray): File = attachmentIo.writeCacheFile(fileName, bytes)

    suspend fun refreshMessagesFromRemote(folderLocalId: String) {
        val folderRemoteId = channelsRepository.getRemoteId(folderLocalId) ?: return
        apiService.messages(folderRemoteId).requireBody().forEach { upsertFromRemote(it, folderLocalId) }
    }

    suspend fun pushMessageCreate(localId: String) {
        val row = messageDao.byLocalId(localId) ?: return
        val folderRemoteId = channelsRepository.getRemoteId(row.folderLocalId) ?: throw IllegalStateException("Pasta ainda sem remoteId")
        val remote = if (row.type == "list") {
            apiService.createChecklist(
                CreateChecklistRequest(
                    folderId = folderRemoteId,
                    content = row.content.takeIf { it.isNotBlank() }?.let { cryptoRepository.encryptString(it) } ?: "",
                    items = listItemDao.allByMessage(localId).filter { it.deletedAt == null }.map { ChecklistItemRequest(cryptoRepository.encryptString(it.text)) }
                )
            ).requireBody()
        } else {
            val atts = attachmentDao.allByMessage(localId).filter { it.deletedAt == null }
            if (atts.isEmpty()) {
                apiService.createMessage(CreateMessageRequest(folderRemoteId, cryptoRepository.encryptString(row.content))).requireBody()
            } else {
                atts.forEach { attachmentDao.update(it.copy(uploadStatus = LocalUploadStatus.Uploading, syncStatus = LocalSyncStatus.Uploading, lastError = null)) }
                val fields = linkedMapOf<String, RequestBody>()
                fields["folderId"] = textBody(folderRemoteId)
                if (row.content.isNotBlank()) fields["content"] = textBody(cryptoRepository.encryptString(row.content))
                val files = atts.mapIndexed { index, attachment ->
                    val encrypted = cryptoRepository.encryptBytesForFile(attachmentIo.readBytes(attachment.localPath.orEmpty()), attachment.fileName)
                    fields["fileMetadata[$index][fileName]"] = textBody(encrypted.fileNameEnvelope)
                    fields["fileMetadata[$index][mimeType]"] = textBody(attachment.mimeType)
                    fields["fileMetadata[$index][fileKind]"] = textBody(attachment.fileKind ?: "other")
                    fields["fileMetadata[$index][extension]"] = textBody(attachment.extension.orEmpty())
                    fields["fileMetadata[$index][compressed]"] = textBody(if (attachment.compressed == true) "true" else "false")
                    fields["fileMetadata[$index][size]"] = textBody(encrypted.bytes.size.toString())
                    MultipartBody.Part.createFormData("files[$index]", "att-$index.bin", encrypted.bytes.toRequestBody("application/octet-stream".toMediaType()))
                }
                apiService.uploadMessage(fields, files).requireBody()
            }
        }
        applyMessageRemote(localId, remote, row.folderLocalId)
    }

    suspend fun pushMessageDelete(localId: String) {
        val row = messageDao.byLocalId(localId) ?: return
        row.remoteId?.let { apiService.deleteMessage(it).requireSuccess() }
        hardDeleteMessage(localId)
    }

    suspend fun pushMessageFavorite(localId: String) {
        val row = messageDao.byLocalId(localId) ?: return
        val remoteId = row.remoteId ?: throw IllegalStateException("Mensagem ainda sem remoteId")
        applyMessageRemote(localId, apiService.toggleFavorite(remoteId).requireBody(), row.folderLocalId)
    }

    suspend fun pushMessageMove(localId: String) {
        val row = messageDao.byLocalId(localId) ?: return
        val remoteId = row.remoteId ?: throw IllegalStateException("Mensagem ainda sem remoteId")
        val folderRemoteId = channelsRepository.getRemoteId(row.folderLocalId) ?: throw IllegalStateException("Pasta destino ainda sem remoteId")
        applyMessageRemote(localId, apiService.moveMessage(remoteId, MoveMessageRequest(folderRemoteId)).requireBody(), row.folderLocalId)
    }

    suspend fun pushListItem(localId: String, opType: String) {
        val row = listItemDao.byLocalId(localId) ?: return
        val message = messageDao.byLocalId(row.messageLocalId) ?: return
        val messageRemoteId = message.remoteId ?: throw IllegalStateException("Mensagem ainda sem remoteId")
        when (opType) {
            "create" -> applyListItemRemote(localId, apiService.addListItem(messageRemoteId, AddListItemRequest(cryptoRepository.encryptString(row.text))).requireBody())
            "update" -> applyListItemRemote(localId, apiService.updateListItem(messageRemoteId, row.remoteId ?: throw IllegalStateException("Item ainda sem remoteId"), UpdateListItemRequest(text = cryptoRepository.encryptString(row.text), isChecked = row.isChecked)).requireBody())
            "toggle" -> applyListItemRemote(localId, apiService.toggleListItem(messageRemoteId, row.remoteId ?: throw IllegalStateException("Item ainda sem remoteId")).requireBody())
            "delete" -> {
                val remoteId = row.remoteId
                if (remoteId != null) apiService.deleteListItem(messageRemoteId, remoteId).requireSuccess()
                listItemDao.delete(localId)
            }
        }
    }

    suspend fun pushReorder(messageLocalId: String) {
        val messageRemoteId = messageDao.byLocalId(messageLocalId)?.remoteId ?: throw IllegalStateException("Mensagem ainda sem remoteId")
        val remoteIds = listItemDao.byMessage(messageLocalId).sortedBy { it.position }.map { it.remoteId ?: throw IllegalStateException("Algum item ainda sem remoteId") }
        apiService.reorderListItems(messageRemoteId, ReorderListItemsRequest(remoteIds)).requireSuccess()
        val now = nowMs()
        listItemDao.byMessage(messageLocalId).forEach { listItemDao.update(it.copy(syncStatus = LocalSyncStatus.Synced, lastError = null, updatedAt = now)) }
    }

    suspend fun markFailed(entity: String, localId: String, error: String) {
        when (entity) {
            "message" -> messageDao.byLocalId(localId)?.let { messageDao.update(it.copy(syncStatus = LocalSyncStatus.Failed, lastError = error)) }
            "listItem" -> listItemDao.byLocalId(localId)?.let { listItemDao.update(it.copy(syncStatus = LocalSyncStatus.Failed, lastError = error)) }
            "attachment" -> attachmentDao.byLocalId(localId)?.let { attachmentDao.update(it.copy(syncStatus = LocalSyncStatus.Failed, uploadStatus = LocalUploadStatus.Failed, lastError = error)) }
        }
    }

    private suspend fun persistAttachment(messageLocalId: String, attachment: PendingAttachment) {
        val now = nowMs()
        val localId = UUID.randomUUID().toString()
        val file = attachmentIo.persistAttachment(localId, attachment)
        attachmentDao.upsert(
            AttachmentEntity(
                localId = localId,
                remoteId = null,
                messageLocalId = messageLocalId,
                fileName = attachment.fileName,
                mimeType = attachment.mimeType,
                size = file.length().takeIf { it > 0L } ?: attachment.size,
                fileKind = attachment.kind.toRemoteKind(),
                extension = attachment.extension,
                compressed = attachment.compress && attachment.isImage,
                localPath = file.absolutePath,
                remoteUrl = null,
                previewLocalPath = null,
                previewUrl = null,
                uploadStatus = LocalUploadStatus.PendingUpload,
                syncStatus = LocalSyncStatus.PendingCreate,
                lastError = null,
                createdAt = now,
                updatedAt = now,
                deletedAt = null
            )
        )
    }

    private suspend fun upsertFromRemote(remote: RemoteMessage, folderLocalId: String): String {
        val remoteId = remote.id ?: return ""
        val remoteUpdatedAt = parseRemoteDate(remote.updatedAt)
        val remoteCreatedAt = parseRemoteDate(remote.createdAt)
        val existing = messageDao.byRemoteId(remoteId)
        val localId = existing?.localId ?: UUID.randomUUID().toString()
        if (existing == null) {
            messageDao.upsert(MessageEntity(localId, remoteId, folderLocalId, remote.type.toLocalMessageType(), cryptoRepository.tryDecryptString(remote.content), remote.isFavorite.asBoolean(), LocalSyncStatus.Synced, null, remoteCreatedAt, remoteUpdatedAt, null))
        } else if (existing.syncStatus == LocalSyncStatus.Synced && remoteUpdatedAt > existing.updatedAt) {
            messageDao.update(existing.copy(folderLocalId = folderLocalId, type = remote.type.toLocalMessageType(), content = cryptoRepository.tryDecryptString(remote.content), isFavorite = remote.isFavorite.asBoolean(), updatedAt = remoteUpdatedAt, lastError = null))
        }
        remote.listItems.orEmpty().forEach { upsertListItemFromRemote(localId, it) }
        remote.attachments.orEmpty().forEach { upsertAttachmentFromRemote(localId, it) }
        return localId
    }

    private suspend fun upsertListItemFromRemote(messageLocalId: String, remote: RemoteListItem) {
        val remoteId = remote.id ?: return
        val existing = listItemDao.byRemoteId(remoteId)
        val updatedAt = parseRemoteDate(remote.updatedAt)
        if (existing == null) {
            listItemDao.upsert(ListItemEntity(UUID.randomUUID().toString(), remoteId, messageLocalId, cryptoRepository.tryDecryptString(remote.text), remote.isChecked.asBoolean(), remote.position.asDouble(), LocalSyncStatus.Synced, null, parseRemoteDate(remote.createdAt), updatedAt, null))
        } else if (existing.syncStatus == LocalSyncStatus.Synced && updatedAt > existing.updatedAt) {
            listItemDao.update(existing.copy(text = cryptoRepository.tryDecryptString(remote.text), isChecked = remote.isChecked.asBoolean(), position = remote.position.asDouble(), updatedAt = updatedAt, lastError = null))
        }
    }

    private suspend fun upsertAttachmentFromRemote(messageLocalId: String, remote: RemoteAttachment) {
        val remoteId = remote.id ?: return
        val existing = attachmentDao.byRemoteId(remoteId)
        val now = nowMs()
        if (existing == null) {
            attachmentDao.upsert(AttachmentEntity(UUID.randomUUID().toString(), remoteId, messageLocalId, cryptoRepository.tryDecryptString(remote.fileName), remote.mimeType ?: "application/octet-stream", remote.size.asLong(), remote.fileKind, remote.extension, remote.compressed.asBoolean(), null, remote.url, null, remote.previewUrl, LocalUploadStatus.Uploaded, LocalSyncStatus.Synced, null, parseRemoteDate(remote.createdAt), now, null))
        } else if (existing.syncStatus == LocalSyncStatus.Synced) {
            attachmentDao.update(existing.copy(fileName = cryptoRepository.tryDecryptString(remote.fileName), mimeType = remote.mimeType ?: existing.mimeType, size = remote.size.asLong(), fileKind = remote.fileKind ?: existing.fileKind, extension = remote.extension ?: existing.extension, compressed = remote.compressed.asBoolean(), remoteUrl = remote.url, previewUrl = remote.previewUrl ?: existing.previewUrl, uploadStatus = LocalUploadStatus.Uploaded, updatedAt = now, lastError = null))
        }
    }

    private suspend fun applyMessageRemote(localId: String, remote: RemoteMessage, folderLocalId: String) {
        val row = messageDao.byLocalId(localId) ?: return
        messageDao.update(row.copy(remoteId = remote.id, content = cryptoRepository.tryDecryptString(remote.content), isFavorite = remote.isFavorite.asBoolean(), updatedAt = parseRemoteDate(remote.updatedAt), syncStatus = LocalSyncStatus.Synced, lastError = null))
        remote.listItems.orEmpty().forEachIndexed { index, item ->
            val local = listItemDao.allByMessage(localId).getOrNull(index) ?: return@forEachIndexed
            applyListItemRemote(local.localId, item)
        }
        remote.attachments.orEmpty().forEachIndexed { index, attachment ->
            val local = attachmentDao.allByMessage(localId).getOrNull(index) ?: return@forEachIndexed
            attachmentDao.update(local.copy(remoteId = attachment.id, remoteUrl = attachment.url, previewUrl = attachment.previewUrl, uploadStatus = LocalUploadStatus.Uploaded, syncStatus = LocalSyncStatus.Synced, updatedAt = nowMs(), lastError = null))
        }
    }

    private suspend fun applyListItemRemote(localId: String, remote: RemoteListItem) {
        val row = listItemDao.byLocalId(localId) ?: return
        listItemDao.update(row.copy(remoteId = remote.id, text = cryptoRepository.tryDecryptString(remote.text), isChecked = remote.isChecked.asBoolean(), position = remote.position.asDouble(), updatedAt = parseRemoteDate(remote.updatedAt), syncStatus = LocalSyncStatus.Synced, lastError = null))
    }

    private suspend fun hardDeleteMessage(localId: String) {
        listItemDao.deleteByMessage(localId)
        attachmentDao.deleteByMessage(localId)
        messageDao.delete(localId)
    }

    private fun textBody(value: String): RequestBody = value.toRequestBody("text/plain".toMediaType())

    private fun normalizeDownloadUrl(url: String): String = url.removePrefix("/api/").removePrefix("api/")
}

private fun Any?.asBoolean(): Boolean = when (this) {
    is Boolean -> this
    is Number -> toInt() == 1
    is String -> equals("true", ignoreCase = true) || this == "1"
    else -> false
}

private fun Any?.asLong(): Long = when (this) {
    is Number -> toLong()
    is String -> toLongOrNull() ?: 0L
    else -> 0L
}

private fun Any?.asDouble(): Double = when (this) {
    is Number -> toDouble()
    is String -> toDoubleOrNull() ?: 0.0
    else -> 0.0
}

data class SearchResult(
    val channel: Channel,
    val message: Message
)
