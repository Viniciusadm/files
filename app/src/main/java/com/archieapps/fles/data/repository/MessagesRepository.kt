package com.archieapps.fles.data.repository

import com.archieapps.fles.data.remote.AddListItemRequest
import com.archieapps.fles.data.remote.ApiService
import com.archieapps.fles.data.remote.ChecklistItemRequest
import com.archieapps.fles.data.remote.CreateChecklistRequest
import com.archieapps.fles.data.remote.CreateMessageRequest
import com.archieapps.fles.data.remote.MoveMessageRequest
import com.archieapps.fles.data.remote.ReorderListItemsRequest
import com.archieapps.fles.data.remote.UpdateListItemRequest
import com.archieapps.fles.data.remote.requireBody
import com.archieapps.fles.data.remote.requireSuccess
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.ui.model.FileKind
import com.archieapps.fles.ui.model.Message
import com.archieapps.fles.ui.model.PendingAttachment
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class MessagesRepository(
    private val apiService: ApiService,
    private val cryptoRepository: CryptoRepository,
    private val channelsRepository: ChannelsRepository,
    private val attachmentIo: AttachmentIo
) {
    suspend fun getMessages(folderId: String): List<Message> {
        return apiService.messages(folderId)
            .requireBody()
            .map { it.toUi(cryptoRepository) }
            .sortedBy { it.createdAt }
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
        val trimmed = content.trim()
        require(trimmed.isNotBlank()) { "Mensagem vazia" }
        return apiService.createMessage(
            CreateMessageRequest(folderId, cryptoRepository.encryptString(trimmed))
        ).requireBody().toUi(cryptoRepository)
    }

    suspend fun send(folderId: String, content: String, attachments: List<PendingAttachment>): Message {
        if (attachments.isEmpty()) return sendText(folderId, content)
        val fields = linkedMapOf<String, RequestBody>()
        fields["folderId"] = textBody(folderId)
        val trimmed = content.trim()
        if (trimmed.isNotBlank()) fields["content"] = textBody(cryptoRepository.encryptString(trimmed))
        val files = attachments.mapIndexed { index, attachment ->
            val encrypted = cryptoRepository.encryptBytesForFile(attachmentIo.readBytes(attachment.uri), attachment.fileName)
            fields["fileMetadata[$index][fileName]"] = textBody(encrypted.fileNameEnvelope)
            fields["fileMetadata[$index][mimeType]"] = textBody(attachment.mimeType)
            fields["fileMetadata[$index][fileKind]"] = textBody(attachment.kind.toRemoteKind())
            fields["fileMetadata[$index][extension]"] = textBody(attachment.extension)
            fields["fileMetadata[$index][compressed]"] = textBody(if (attachment.compress) "true" else "false")
            fields["fileMetadata[$index][size]"] = textBody(encrypted.bytes.size.toString())
            MultipartBody.Part.createFormData(
                "files[$index]",
                "att-$index.bin",
                encrypted.bytes.toRequestBody("application/octet-stream".toMediaType())
            )
        }
        return apiService.uploadMessage(fields, files).requireBody().toUi(cryptoRepository)
    }

    suspend fun createChecklist(folderId: String, title: String, items: List<String>): Message {
        val filled = items.map { it.trim() }.filter { it.isNotBlank() }
        require(filled.isNotEmpty()) { "Adicione ao menos um item" }
        return apiService.createChecklist(
            CreateChecklistRequest(
                folderId = folderId,
                content = title.trim().takeIf { it.isNotBlank() }?.let { cryptoRepository.encryptString(it) } ?: "",
                items = filled.map { ChecklistItemRequest(cryptoRepository.encryptString(it)) }
            )
        ).requireBody().toUi(cryptoRepository)
    }

    suspend fun deleteMessage(id: String) {
        apiService.deleteMessage(id).requireSuccess()
    }

    suspend fun toggleFavorite(id: String): Message {
        return apiService.toggleFavorite(id).requireBody().toUi(cryptoRepository)
    }

    suspend fun moveMessage(id: String, targetFolderId: String): Message {
        return apiService.moveMessage(id, MoveMessageRequest(targetFolderId)).requireBody().toUi(cryptoRepository)
    }

    suspend fun addListItem(messageId: String, text: String) {
        val trimmed = text.trim()
        require(trimmed.isNotBlank()) { "Item vazio" }
        apiService.addListItem(messageId, AddListItemRequest(cryptoRepository.encryptString(trimmed))).requireBody()
    }

    suspend fun updateListItem(messageId: String, itemId: String, text: String? = null, isChecked: Boolean? = null) {
        val encryptedText = text?.trim()?.takeIf { it.isNotBlank() }?.let { cryptoRepository.encryptString(it) }
        apiService.updateListItem(messageId, itemId, UpdateListItemRequest(text = encryptedText, isChecked = isChecked)).requireBody()
    }

    suspend fun toggleListItem(messageId: String, itemId: String) {
        apiService.toggleListItem(messageId, itemId).requireBody()
    }

    suspend fun deleteListItem(messageId: String, itemId: String) {
        apiService.deleteListItem(messageId, itemId).requireSuccess()
    }

    suspend fun reorderListItems(messageId: String, orderedIds: List<String>) {
        apiService.reorderListItems(messageId, ReorderListItemsRequest(orderedIds)).requireSuccess()
    }

    suspend fun downloadAttachment(url: String): ByteArray {
        return cryptoRepository.decryptBytesFromFile(apiService.download(normalizeDownloadUrl(url)).bytes())
    }

    fun cachePlainFile(fileName: String, bytes: ByteArray) = attachmentIo.writeCacheFile(fileName, bytes)

    private fun textBody(value: String): RequestBody {
        return value.toRequestBody("text/plain".toMediaType())
    }

    private fun normalizeDownloadUrl(url: String): String {
        return url.removePrefix("/api/").removePrefix("api/")
    }

    private fun FileKind.toRemoteKind(): String {
        return when (this) {
            FileKind.Image -> "image"
            FileKind.Pdf -> "pdf"
            FileKind.Code -> "code"
            FileKind.Document,
            FileKind.Text,
            FileKind.Spreadsheet,
            FileKind.Presentation -> "document"
            else -> "other"
        }
    }
}

data class SearchResult(
    val channel: Channel,
    val message: Message
)
