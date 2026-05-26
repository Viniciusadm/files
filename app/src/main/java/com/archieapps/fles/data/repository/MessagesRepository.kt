package com.archieapps.fles.data.repository

import com.archieapps.fles.data.local.db.AttachmentDao
import com.archieapps.fles.data.local.db.AttachmentEntity
import com.archieapps.fles.data.local.db.ListItemDao
import com.archieapps.fles.data.local.db.ListItemEntity
import com.archieapps.fles.data.local.db.LocalSyncStatus
import com.archieapps.fles.data.local.db.LocalUploadStatus
import com.archieapps.fles.data.local.db.MessageDao
import com.archieapps.fles.data.local.db.MessageEntity
import com.archieapps.fles.data.local.db.MessageEntityWithChildren
import com.archieapps.fles.data.local.db.nowMs
import com.archieapps.fles.data.local.db.parseRemoteDate
import com.archieapps.fles.data.remote.AddListItemRequest
import com.archieapps.fles.data.remote.ApiService
import com.archieapps.fles.data.remote.ChecklistItemRequest
import com.archieapps.fles.data.remote.CreateChecklistRequest
import com.archieapps.fles.data.remote.CreateMessageRequest
import com.archieapps.fles.data.remote.CreateUploadRequest
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
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import kotlin.math.min

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesRepository(
    private val apiService: ApiService,
    private val cryptoRepository: CryptoRepository,
    private val secureFoldersRepository: SecureFoldersRepository,
    private val channelsRepository: ChannelsRepository,
    private val attachmentIo: AttachmentIo,
    private val messageDao: MessageDao,
    private val listItemDao: ListItemDao,
    private val attachmentDao: AttachmentDao,
    private val queueRepository: SyncQueueRepository,
    private val scheduler: SyncScheduler,
    private val networkMonitor: NetworkMonitor
) {
    private companion object {
        const val UPLOAD_CHUNK_SIZE = 2 * 1024 * 1024
    }

    fun observeMessages(folderId: String): Flow<List<Message>> {
        return messageDao.observeByFolderWithChildren(folderId).mapLatest { rows ->
            val secure = channelsRepository.isSecure(folderId)
            if (secure && !secureFoldersRepository.state.value.unlocked) emptyList()
            else rows.mapNotNull { if (secure) it.toSecureUiOrNull() else it.toUiOrNull() }
        }
    }

    fun observeAllMessages(): Flow<List<Message>> {
        return messageDao.observeActiveWithChildren().mapLatest { rows ->
            rows.mapNotNull { row ->
                val secure = channelsRepository.isSecure(row.message.folderLocalId)
                if (secure && !secureFoldersRepository.state.value.unlocked) {
                    null
                } else if (secure) {
                    row.toSecureUiOrNull()
                } else {
                    row.toUiOrNull()
                }
            }
        }
    }

    private fun MessageEntityWithChildren.toUiOrNull(): Message? {
        val activeListItems = listItems.filter { it.deletedAt == null }
        val activeAttachments = attachments.filter { it.deletedAt == null }
        val itemsClean = activeListItems.filter { !cryptoRepository.looksLikeEnvelope(it.text) }
        val attachmentsClean = activeAttachments.filter { !cryptoRepository.looksLikeEnvelope(it.fileName) }
        val contentEnvelope = cryptoRepository.looksLikeEnvelope(message.content)
        if (contentEnvelope) {
            if (attachmentsClean.isEmpty() && itemsClean.isEmpty()) return null
            return message.copy(content = "").toUi(
                itemsClean.sortedBy { it.position }.map { it.toUi() },
                attachmentsClean.map { it.toUi() }
            )
        }
        return message.toUi(
            itemsClean.sortedBy { it.position }.map { it.toUi() },
            attachmentsClean.map { it.toUi() }
        )
    }

    private fun MessageEntityWithChildren.toSecureUiOrNull(): Message? {
        val activeListItems = listItems.filter { it.deletedAt == null }
        val activeAttachments = attachments.filter { it.deletedAt == null }
        val decryptedContent = secureFoldersRepository.tryDecryptStringOrNull(message.content)
        val itemsDecrypted = activeListItems.mapNotNull { item ->
            val text = secureFoldersRepository.tryDecryptStringOrNull(item.text) ?: return@mapNotNull null
            item.copy(text = text).toUi()
        }
        val attachmentsDecrypted = activeAttachments.mapNotNull { att ->
            val name = secureFoldersRepository.tryDecryptStringOrNull(att.fileName) ?: return@mapNotNull null
            att.copy(fileName = name).toUi()
        }
        if (decryptedContent == null && itemsDecrypted.isEmpty() && attachmentsDecrypted.isEmpty()) return null
        return message.copy(content = decryptedContent ?: "").toUi(
            itemsDecrypted.sortedBy { it.position },
            attachmentsDecrypted
        )
    }

    suspend fun getMessages(folderId: String): List<Message> {
        val secure = channelsRepository.isSecure(folderId)
        if (secure && !secureFoldersRepository.state.value.unlocked) return emptyList()
        return messageDao.byFolder(folderId).map { message ->
            if (secure) message.toSecureUi() else message.toUi(
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
        attachments.forEach { AttachmentIo.validateFileSize(it.fileName, it.size) }
        requireFolderAccessible(folderId)
        val secure = channelsRepository.isSecure(folderId)
        val now = nowMs()
        val localId = UUID.randomUUID().toString()
        val type = when {
            attachments.isEmpty() -> "text"
            attachments.any { it.isImage } && attachments.any { !it.isImage } -> "mixed"
            attachments.all { it.isImage } -> "image"
            else -> "document"
        }
        val message = MessageEntity(localId, null, folderId, type, encryptForLocal(trimmed, secure), false, LocalSyncStatus.PendingCreate, null, now, now, null)
        messageDao.upsert(message)
        attachments.forEach { persistAttachment(localId, it, secure) }
        queueRepository.enqueue(
            opType = "create",
            entity = "message",
            entityLocalId = localId,
            parentLocalId = folderId,
            payload = "{\"type\":\"$type\",\"hasAttachments\":${attachments.isNotEmpty()}}",
            dependsOnId = queueRepository.lastPendingOpId(folderId)
        )
        scheduler.scheduleNow()
        return if (secure) message.toSecureUi() else message.toUi(emptyList(), attachmentDao.byMessage(localId).map { it.toUi() })
    }

    suspend fun createChecklist(folderId: String, title: String, items: List<String>): Message {
        val filled = items.map { it.trim() }.filter { it.isNotBlank() }
        require(filled.isNotEmpty()) { "Adicione ao menos um item" }
        requireFolderAccessible(folderId)
        val secure = channelsRepository.isSecure(folderId)
        val now = nowMs()
        val localId = UUID.randomUUID().toString()
        val message = MessageEntity(localId, null, folderId, "list", encryptForLocal(title.trim(), secure), false, LocalSyncStatus.PendingCreate, null, now, now, null)
        messageDao.upsert(message)
        filled.forEachIndexed { index, text ->
            listItemDao.upsert(
                ListItemEntity(
                    localId = UUID.randomUUID().toString(),
                    remoteId = null,
                    messageLocalId = localId,
                    text = encryptForLocal(text, secure),
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
        return if (secure) message.toSecureUi() else message.toUi(listItemDao.byMessage(localId).map { it.toUi() }, emptyList())
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
        requireFolderAccessible(row.folderLocalId)
        val now = nowMs()
        val updated = row.copy(isFavorite = !row.isFavorite, syncStatus = LocalSyncStatus.PendingUpdate, updatedAt = now, lastError = null)
        messageDao.update(updated)
        queueRepository.enqueue(opType = "favorite", entity = "message", entityLocalId = id, parentLocalId = row.folderLocalId, dependsOnId = queueRepository.lastPendingOpId(id))
        scheduler.scheduleNow()
        return if (channelsRepository.isSecure(row.folderLocalId)) updated.toSecureUi() else updated.toUi(listItemDao.byMessage(id).map { it.toUi() }, attachmentDao.byMessage(id).map { it.toUi() })
    }

    suspend fun moveMessage(id: String, targetFolderId: String): Message? {
        val row = messageDao.byLocalId(id) ?: return null
        requireFolderAccessible(row.folderLocalId)
        requireFolderAccessible(targetFolderId)
        require(channelsRepository.isSecure(row.folderLocalId) == channelsRepository.isSecure(targetFolderId)) {
            "Mover entre áreas normal e segura ainda não é suportado."
        }
        val now = nowMs()
        val updated = row.copy(folderLocalId = targetFolderId, syncStatus = LocalSyncStatus.PendingUpdate, updatedAt = now, lastError = null)
        messageDao.update(updated)
        queueRepository.enqueue(opType = "move", entity = "message", entityLocalId = id, parentLocalId = targetFolderId, dependsOnId = queueRepository.lastPendingOpId(id))
        scheduler.scheduleNow()
        return if (channelsRepository.isSecure(targetFolderId)) updated.toSecureUi() else updated.toUi(listItemDao.byMessage(id).map { it.toUi() }, attachmentDao.byMessage(id).map { it.toUi() })
    }

    suspend fun addListItem(messageId: String, text: String) {
        val trimmed = text.trim()
        require(trimmed.isNotBlank()) { "Item vazio" }
        val now = nowMs()
        val itemId = UUID.randomUUID().toString()
        val secure = messageDao.byLocalId(messageId)?.let { channelsRepository.isSecure(it.folderLocalId) } == true
        listItemDao.upsert(ListItemEntity(itemId, null, messageId, encryptForLocal(trimmed, secure), false, now.toDouble(), LocalSyncStatus.PendingCreate, null, now, now, null))
        queueRepository.enqueue("create", "listItem", itemId, messageId, dependsOnId = queueRepository.lastPendingOpId(messageId))
        scheduler.scheduleNow()
    }

    suspend fun updateListItem(messageId: String, itemId: String, text: String? = null, isChecked: Boolean? = null) {
        val row = listItemDao.byLocalId(itemId) ?: return
        val now = nowMs()
        val secure = messageDao.byLocalId(messageId)?.let { channelsRepository.isSecure(it.folderLocalId) } == true
        val nextText = text?.trim()?.takeIf { it.isNotBlank() }?.let { encryptForLocal(it, secure) } ?: row.text
        listItemDao.update(row.copy(text = nextText, isChecked = isChecked ?: row.isChecked, syncStatus = LocalSyncStatus.PendingUpdate, updatedAt = now, lastError = null))
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
        val entity = attachmentDao.byLocalId(attachment.id) ?: throw IllegalStateException("Anexo não encontrado")
        val secure = messageDao.byLocalId(entity.messageLocalId)?.let { channelsRepository.isSecure(it.folderLocalId) } == true
        if (secure) {
            entity.previewLocalPath?.let { File(it) }?.takeIf { it.exists() }?.let { return it }
        }
        attachment.localPath?.let {
            val file = File(it.removePrefix("file://"))
            if (file.exists()) {
                return if (secure) {
                    val storedBytes = file.readBytes()
                    val bytes = runCatching { decryptFileBytes(entity.messageLocalId, storedBytes) }.getOrElse {
                        if (storedBytes.looksLikePlainFile(attachment)) storedBytes else throw it
                    }
                    val previewFile = attachmentIo.writeCacheFile(attachment.fileName, bytes)
                    if (entity.previewLocalPath != previewFile.absolutePath) {
                        attachmentDao.update(entity.copy(previewLocalPath = previewFile.absolutePath, updatedAt = nowMs()))
                    }
                    previewFile
                } else {
                    file
                }
            }
        }
        if (!networkMonitor.isOnline()) throw IllegalStateException("Arquivo indisponível offline. Baixe quando houver conexão.")
        attachmentDao.update(entity.copy(syncStatus = LocalSyncStatus.Downloading, lastError = null))
        return runCatching {
            val encryptedBytes = apiService.download(normalizeDownloadUrl(entity.remoteUrl.orEmpty())).bytes()
            val decryptedBytes = decryptFileBytes(entity.messageLocalId, encryptedBytes)
            val localFile = if (secure) {
                attachmentIo.persistAttachmentBytes(entity.localId, entity.extension.orEmpty(), encryptedBytes)
            } else {
                attachmentIo.persistDownloadedFile(entity.localId, entity.fileName, decryptedBytes)
            }
            val previewFile = if (secure) attachmentIo.writeCacheFile(attachment.fileName, decryptedBytes) else localFile
            attachmentDao.update(entity.copy(
                localPath = localFile.absolutePath,
                previewLocalPath = if (secure) previewFile.absolutePath else entity.previewLocalPath,
                syncStatus = LocalSyncStatus.Synced,
                lastError = null,
                updatedAt = nowMs()
            ))
            previewFile
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

    suspend fun retryUpload(messageLocalId: String) {
        attachmentDao.allByMessage(messageLocalId).forEach {
            attachmentDao.update(it.copy(uploadStatus = LocalUploadStatus.Retrying, syncStatus = LocalSyncStatus.Uploading, lastError = null, uploadState = "retomando", updatedAt = nowMs()))
        }
        queueRepository.retryEntity(messageLocalId)
        scheduler.scheduleNow()
    }

    suspend fun cancelUpload(messageLocalId: String) {
        attachmentDao.allByMessage(messageLocalId).forEach { attachment ->
            attachment.uploadId?.let { uploadId -> runCatching { apiService.cancelUpload(uploadId).requireSuccess() } }
            attachmentDao.update(attachment.copy(uploadStatus = LocalUploadStatus.Cancelled, syncStatus = LocalSyncStatus.Failed, lastError = "Upload cancelado", uploadState = "cancelado", updatedAt = nowMs()))
        }
        hardDeleteMessage(messageLocalId)
    }

    suspend fun refreshMessagesFromRemote(folderLocalId: String) {
        val folderRemoteId = channelsRepository.getRemoteId(folderLocalId) ?: return
        apiService.messages(folderRemoteId).requireBody().forEach { upsertFromRemote(it, folderLocalId) }
    }

    suspend fun pushMessageCreate(localId: String) {
        val row = messageDao.byLocalId(localId) ?: return
        val folderRemoteId = channelsRepository.getRemoteId(row.folderLocalId) ?: throw IllegalStateException("Pasta ainda sem remoteId")
        val secure = channelsRepository.isSecure(row.folderLocalId)
        val remote = if (row.type == "list") {
            apiService.createChecklist(
                CreateChecklistRequest(
                    folderId = folderRemoteId,
                    content = row.content.takeIf { it.isNotBlank() }?.let { remoteString(it, secure) } ?: "",
                    items = listItemDao.allByMessage(localId).filter { it.deletedAt == null }.map { ChecklistItemRequest(remoteString(it.text, secure)) }
                )
            ).requireBody()
        } else {
            val atts = attachmentDao.allByMessage(localId).filter { it.deletedAt == null }
            if (atts.isEmpty()) {
                apiService.createMessage(CreateMessageRequest(folderRemoteId, remoteString(row.content, secure))).requireBody()
            } else {
                atts.forEach { attachmentDao.update(it.copy(uploadStatus = LocalUploadStatus.Uploading, syncStatus = LocalSyncStatus.Uploading, lastError = null)) }
                val fields = linkedMapOf<String, RequestBody>()
                fields["folderId"] = textBody(folderRemoteId)
                if (row.content.isNotBlank()) fields["content"] = textBody(remoteString(row.content, secure))
                val uploadIds = atts.mapIndexed { index, attachment ->
                    val upload = prepareUploadFile(attachment, secure)
                    fields["fileMetadata[$index][fileName]"] = textBody(upload.fileNameEnvelope)
                    fields["fileMetadata[$index][mimeType]"] = textBody(attachment.mimeType)
                    fields["fileMetadata[$index][fileKind]"] = textBody(attachment.fileKind ?: "other")
                    fields["fileMetadata[$index][extension]"] = textBody(attachment.extension.orEmpty())
                    fields["fileMetadata[$index][compressed]"] = textBody(if (attachment.compressed == true) "true" else "false")
                    fields["fileMetadata[$index][size]"] = textBody(upload.file.length().toString())
                    val uploadId = uploadAttachmentChunks(attachment, upload)
                    if (!secure) runCatching { upload.file.delete() }
                    fields["uploadIds[$index]"] = textBody(uploadId)
                    uploadId
                }
                require(uploadIds.size == atts.size) { "Falha ao preparar uploads." }
                apiService.uploadMessage(fields, emptyList()).requireBody()
            }
        }
        applyMessageRemote(localId, remote, row.folderLocalId)
    }

    private suspend fun prepareUploadFile(attachment: AttachmentEntity, secure: Boolean): PreparedUpload {
        val localFile = File(attachment.localPath.orEmpty())
        if (!localFile.exists()) throw IllegalStateException("Arquivo local não encontrado.")
        if (secure) {
            return PreparedUpload(localFile, attachment.fileName)
        }

        val encrypted = cryptoRepository.encryptFileForUpload(localFile, attachment.fileName, attachmentIo.uploadWorkFile(attachment.localId))
        return PreparedUpload(encrypted.file, encrypted.fileNameEnvelope)
    }

    private suspend fun uploadAttachmentChunks(attachment: AttachmentEntity, upload: PreparedUpload): String {
        val existingUploadId = attachment.uploadId
        val session = if (existingUploadId.isNullOrBlank()) {
            apiService.createUpload(
                CreateUploadRequest(
                    fileName = upload.fileNameEnvelope,
                    mimeType = attachment.mimeType,
                    size = upload.file.length(),
                    chunkSize = UPLOAD_CHUNK_SIZE,
                    extension = attachment.extension.orEmpty(),
                    fileKind = attachment.fileKind ?: "other",
                    compressed = attachment.compressed == true
                )
            ).requireBody()
        } else {
            attachmentDao.update(attachment.copy(uploadStatus = LocalUploadStatus.Retrying, uploadState = "retomando", lastError = null))
            apiService.uploadStatus(existingUploadId).requireBody()
        }

        var uploadId = session.uploadId
        var chunkSize = session.chunkSize
        var nextChunkIndex = session.nextChunkIndex
        var uploadedBytes = session.receivedBytes
        var totalChunks = session.totalChunks
        attachmentDao.update(
            attachment.copy(
                uploadId = uploadId,
                chunkSize = chunkSize,
                totalChunks = totalChunks,
                nextChunkIndex = nextChunkIndex,
                uploadedBytes = uploadedBytes,
                uploadStatus = LocalUploadStatus.Uploading,
                uploadState = "enviando",
                lastError = null,
                updatedAt = nowMs()
            )
        )

        while (nextChunkIndex < totalChunks) {
            val offset = nextChunkIndex.toLong() * chunkSize.toLong()
            val length = min(chunkSize.toLong(), upload.file.length() - offset)
            val fields = linkedMapOf<String, RequestBody>()
            fields["chunkIndex"] = textBody(nextChunkIndex.toString())
            fields["totalChunks"] = textBody(totalChunks.toString())
            fields["chunkSize"] = textBody(chunkSize.toString())
            val part = MultipartBody.Part.createFormData(
                "chunk",
                "chunk-$nextChunkIndex.bin",
                FileChunkRequestBody(upload.file, offset, length)
            )
            val response = apiService.uploadChunk(uploadId, fields, part).requireBody()
            chunkSize = response.chunkSize
            totalChunks = response.totalChunks
            nextChunkIndex = response.nextChunkIndex
            uploadedBytes = response.receivedBytes
            attachmentDao.byLocalId(attachment.localId)?.let {
                attachmentDao.update(
                    it.copy(
                        chunkSize = chunkSize,
                        totalChunks = totalChunks,
                        nextChunkIndex = nextChunkIndex,
                        uploadedBytes = uploadedBytes,
                        uploadStatus = LocalUploadStatus.Uploading,
                        uploadState = "enviando",
                        updatedAt = nowMs()
                    )
                )
            }
        }

        val completed = apiService.completeUpload(uploadId).requireBody()
        attachmentDao.byLocalId(attachment.localId)?.let {
            attachmentDao.update(
                it.copy(
                    chunkSize = completed.chunkSize,
                    totalChunks = completed.totalChunks,
                    nextChunkIndex = completed.nextChunkIndex,
                    uploadedBytes = completed.size,
                    uploadStatus = LocalUploadStatus.Uploading,
                    uploadState = "concluído",
                    updatedAt = nowMs()
                )
            )
        }
        return uploadId
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
        val secure = channelsRepository.isSecure(message.folderLocalId)
        when (opType) {
            "create" -> applyListItemRemote(localId, apiService.addListItem(messageRemoteId, AddListItemRequest(remoteString(row.text, secure))).requireBody(), secure)
            "update" -> applyListItemRemote(localId, apiService.updateListItem(messageRemoteId, row.remoteId ?: throw IllegalStateException("Item ainda sem remoteId"), UpdateListItemRequest(text = remoteString(row.text, secure), isChecked = row.isChecked)).requireBody(), secure)
            "toggle" -> applyListItemRemote(localId, apiService.toggleListItem(messageRemoteId, row.remoteId ?: throw IllegalStateException("Item ainda sem remoteId")).requireBody(), secure)
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
            "message" -> messageDao.byLocalId(localId)?.let {
                messageDao.update(it.copy(syncStatus = LocalSyncStatus.Failed, lastError = error))
                attachmentDao.allByMessage(localId).forEach { attachment ->
                    attachmentDao.update(attachment.copy(syncStatus = LocalSyncStatus.Failed, uploadStatus = LocalUploadStatus.Failed, lastError = error))
                }
            }
            "listItem" -> listItemDao.byLocalId(localId)?.let { listItemDao.update(it.copy(syncStatus = LocalSyncStatus.Failed, lastError = error)) }
            "attachment" -> attachmentDao.byLocalId(localId)?.let { attachmentDao.update(it.copy(syncStatus = LocalSyncStatus.Failed, uploadStatus = LocalUploadStatus.Failed, lastError = error)) }
        }
    }

    private suspend fun persistAttachment(messageLocalId: String, attachment: PendingAttachment, secure: Boolean) {
        val now = nowMs()
        val localId = UUID.randomUUID().toString()
        val file = if (secure) {
            val key = secureFoldersRepository.secureKey() ?: throw IllegalStateException("Pastas seguras bloqueadas.")
            val source = attachmentIo.persistAttachment(localId, attachment)
            val encrypted = cryptoRepository.encryptFileForUploadWithKey(
                source,
                key,
                secureFoldersRepository.encryptString(attachment.fileName),
                attachmentIo.temporaryWorkFile(localId)
            )
            encrypted.file.copyTo(source, overwrite = true)
            runCatching { encrypted.file.delete() }
            source
        } else {
            attachmentIo.persistAttachment(localId, attachment)
        }
        attachmentDao.upsert(
            AttachmentEntity(
                localId = localId,
                remoteId = null,
                messageLocalId = messageLocalId,
                fileName = encryptForLocal(attachment.fileName, secure),
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
        val secure = remote.folderIsSecure == true || channelsRepository.isSecure(folderLocalId)
        if (existing == null) {
            messageDao.upsert(MessageEntity(localId, remoteId, folderLocalId, remote.type.toLocalMessageType(), localString(remote.content, secure), remote.isFavorite.asBoolean(), LocalSyncStatus.Synced, null, remoteCreatedAt, remoteUpdatedAt, null))
        } else if (existing.syncStatus == LocalSyncStatus.Synced && (remoteUpdatedAt > existing.updatedAt || (!secure && cryptoRepository.looksLikeEnvelope(existing.content)))) {
            messageDao.update(existing.copy(folderLocalId = folderLocalId, type = remote.type.toLocalMessageType(), content = localString(remote.content, secure), isFavorite = remote.isFavorite.asBoolean(), updatedAt = remoteUpdatedAt, lastError = null))
        }
        remote.listItems.orEmpty().forEach { upsertListItemFromRemote(localId, it, secure) }
        remote.attachments.orEmpty().forEach { upsertAttachmentFromRemote(localId, it, secure) }
        return localId
    }

    private suspend fun upsertListItemFromRemote(messageLocalId: String, remote: RemoteListItem, secure: Boolean) {
        val remoteId = remote.id ?: return
        val existing = listItemDao.byRemoteId(remoteId)
        val updatedAt = parseRemoteDate(remote.updatedAt)
        if (existing == null) {
            listItemDao.upsert(ListItemEntity(UUID.randomUUID().toString(), remoteId, messageLocalId, localString(remote.text, secure), remote.isChecked.asBoolean(), remote.position.asDouble(), LocalSyncStatus.Synced, null, parseRemoteDate(remote.createdAt), updatedAt, null))
        } else if (existing.syncStatus == LocalSyncStatus.Synced && (updatedAt > existing.updatedAt || (!secure && cryptoRepository.looksLikeEnvelope(existing.text)))) {
            listItemDao.update(existing.copy(text = localString(remote.text, secure), isChecked = remote.isChecked.asBoolean(), position = remote.position.asDouble(), updatedAt = updatedAt, lastError = null))
        }
    }

    private suspend fun upsertAttachmentFromRemote(messageLocalId: String, remote: RemoteAttachment, secure: Boolean) {
        val remoteId = remote.id ?: return
        val existing = attachmentDao.byRemoteId(remoteId)
        val now = nowMs()
        if (existing == null) {
            attachmentDao.upsert(
                AttachmentEntity(
                    localId = UUID.randomUUID().toString(),
                    remoteId = remoteId,
                    messageLocalId = messageLocalId,
                    fileName = localString(remote.fileName, secure),
                    mimeType = remote.mimeType ?: "application/octet-stream",
                    size = remote.size.asLong(),
                    fileKind = remote.fileKind,
                    extension = remote.extension,
                    compressed = remote.compressed.asBoolean(),
                    localPath = null,
                    remoteUrl = remote.url,
                    previewLocalPath = null,
                    previewUrl = remote.previewUrl,
                    uploadStatus = LocalUploadStatus.Uploaded,
                    uploadedBytes = remote.size.asLong(),
                    syncStatus = LocalSyncStatus.Synced,
                    lastError = null,
                    createdAt = parseRemoteDate(remote.createdAt),
                    updatedAt = now,
                    deletedAt = null
                )
            )
        } else if (existing.syncStatus == LocalSyncStatus.Synced) {
            attachmentDao.update(existing.copy(fileName = localString(remote.fileName, secure), mimeType = remote.mimeType ?: existing.mimeType, size = remote.size.asLong(), fileKind = remote.fileKind ?: existing.fileKind, extension = remote.extension ?: existing.extension, compressed = remote.compressed.asBoolean(), remoteUrl = remote.url, previewUrl = remote.previewUrl ?: existing.previewUrl, uploadStatus = LocalUploadStatus.Uploaded, uploadedBytes = remote.size.asLong(), updatedAt = now, lastError = null))
        }
    }

    private suspend fun applyMessageRemote(localId: String, remote: RemoteMessage, folderLocalId: String) {
        val row = messageDao.byLocalId(localId) ?: return
        val secure = channelsRepository.isSecure(folderLocalId)
        messageDao.update(row.copy(remoteId = remote.id, content = localString(remote.content, secure), isFavorite = remote.isFavorite.asBoolean(), updatedAt = parseRemoteDate(remote.updatedAt), syncStatus = LocalSyncStatus.Synced, lastError = null))
        remote.listItems.orEmpty().forEachIndexed { index, item ->
            val local = listItemDao.allByMessage(localId).getOrNull(index) ?: return@forEachIndexed
            applyListItemRemote(local.localId, item, secure)
        }
        remote.attachments.orEmpty().forEachIndexed { index, attachment ->
            val local = attachmentDao.allByMessage(localId).getOrNull(index) ?: return@forEachIndexed
            attachmentDao.update(local.copy(remoteId = attachment.id, remoteUrl = attachment.url, previewUrl = attachment.previewUrl, uploadStatus = LocalUploadStatus.Uploaded, syncStatus = LocalSyncStatus.Synced, updatedAt = nowMs(), lastError = null))
        }
    }

    private suspend fun applyListItemRemote(localId: String, remote: RemoteListItem, secure: Boolean = false) {
        val row = listItemDao.byLocalId(localId) ?: return
        listItemDao.update(row.copy(remoteId = remote.id, text = localString(remote.text, secure), isChecked = remote.isChecked.asBoolean(), position = remote.position.asDouble(), updatedAt = parseRemoteDate(remote.updatedAt), syncStatus = LocalSyncStatus.Synced, lastError = null))
    }

    private suspend fun requireFolderAccessible(folderId: String) {
        if (channelsRepository.isSecure(folderId) && !secureFoldersRepository.state.value.unlocked) {
            throw IllegalStateException("Pastas seguras bloqueadas.")
        }
    }

    private suspend fun remoteString(value: String, secure: Boolean): String {
        return if (secure) value else cryptoRepository.encryptString(value)
    }

    private suspend fun localString(value: String?, secure: Boolean): String {
        return if (secure) value.orEmpty() else cryptoRepository.tryDecryptString(value)
    }

    private fun encryptForLocal(value: String, secure: Boolean): String {
        return if (secure && value.isNotBlank()) secureFoldersRepository.encryptString(value) else value
    }

    private suspend fun encryptAttachmentBytes(bytes: ByteArray, fileName: String, secure: Boolean): EncryptedFile {
        return if (secure) {
            val key = secureFoldersRepository.secureKey() ?: throw IllegalStateException("Pastas seguras bloqueadas.")
            cryptoRepository.encryptBytesForFileWithKey(bytes, fileName, key, secureFoldersRepository.encryptString(fileName))
        } else {
            cryptoRepository.encryptBytesForFile(bytes, fileName)
        }
    }

    private suspend fun decryptFileBytes(messageLocalId: String, bytes: ByteArray): ByteArray {
        val message = messageDao.byLocalId(messageLocalId) ?: return cryptoRepository.decryptBytesFromFile(bytes)
        val secure = channelsRepository.isSecure(message.folderLocalId)
        return if (secure) {
            val key = secureFoldersRepository.secureKey() ?: throw IllegalStateException("Pastas seguras bloqueadas.")
            cryptoRepository.decryptBytesFromFileWithKey(bytes, key)
        } else {
            cryptoRepository.decryptBytesFromFile(bytes)
        }
    }

    private suspend fun MessageEntity.toSecureUi(): Message {
        return copy(content = secureFoldersRepository.tryDecryptString(content)).toUi(
            listItemDao.byMessage(localId).map { it.copy(text = secureFoldersRepository.tryDecryptString(it.text)).toUi() },
            attachmentDao.byMessage(localId).map { it.copy(fileName = secureFoldersRepository.tryDecryptString(it.fileName)).toUi() }
        )
    }

    private fun MessageEntityWithChildren.toSecureUi(): Message {
        return message.copy(content = secureFoldersRepository.tryDecryptString(message.content)).toUi(
            listItems.filter { it.deletedAt == null }.sortedBy { it.position }.map { it.copy(text = secureFoldersRepository.tryDecryptString(it.text)).toUi() },
            attachments.filter { it.deletedAt == null }.map { it.copy(fileName = secureFoldersRepository.tryDecryptString(it.fileName)).toUi() }
        )
    }

    private suspend fun hardDeleteMessage(localId: String) {
        listItemDao.deleteByMessage(localId)
        attachmentDao.deleteByMessage(localId)
        messageDao.delete(localId)
    }

    private fun textBody(value: String): RequestBody = value.toRequestBody("text/plain".toMediaType())

    private fun normalizeDownloadUrl(url: String): String = url.removePrefix("/api/").removePrefix("api/")
}

private fun ByteArray.looksLikePlainFile(attachment: Attachment): Boolean {
    val ext = attachment.extension.lowercase()
    return when {
        startsWithBytes(0x89, 0x50, 0x4E, 0x47) -> true
        startsWithBytes(0xFF, 0xD8, 0xFF) -> true
        startsWithAscii("GIF87a") || startsWithAscii("GIF89a") -> true
        size >= 12 && startsWithAscii("RIFF") && copyOfRange(8, 12).decodeToString() == "WEBP" -> true
        attachment.kind == FileKind.Text || attachment.mimeType.startsWith("text/") || ext in setOf("txt", "md", "json", "xml", "csv", "log") -> true
        else -> false
    }
}

private fun ByteArray.startsWithBytes(vararg bytes: Int): Boolean {
    return size >= bytes.size && bytes.indices.all { this[it].toInt() and 0xFF == bytes[it] }
}

private fun ByteArray.startsWithAscii(value: String): Boolean {
    return size >= value.length && copyOfRange(0, value.length).decodeToString() == value
}

private fun MessageEntityWithChildren.toUi(): Message {
    return message.toUi(
        listItems.filter { it.deletedAt == null }.sortedBy { it.position }.map { it.toUi() },
        attachments.filter { it.deletedAt == null }.map { it.toUi() }
    )
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

private data class PreparedUpload(
    val file: File,
    val fileNameEnvelope: String
)

private class FileChunkRequestBody(
    private val file: File,
    private val offset: Long,
    private val byteCount: Long
) : RequestBody() {
    override fun contentType() = "application/octet-stream".toMediaType()

    override fun contentLength(): Long = byteCount

    override fun writeTo(sink: BufferedSink) {
        FileInputStream(file).use { input ->
            var skipped = 0L
            while (skipped < offset) {
                val current = input.skip(offset - skipped)
                if (current <= 0L) throw IllegalStateException("Não foi possível posicionar o chunk.")
                skipped += current
            }
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var remaining = byteCount
            while (remaining > 0) {
                val read = input.read(buffer, 0, min(buffer.size.toLong(), remaining).toInt())
                if (read < 0) break
                sink.write(buffer, 0, read)
                remaining -= read.toLong()
            }
        }
    }
}
