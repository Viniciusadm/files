package com.archieapps.fles.data.repository

import com.archieapps.fles.ui.model.FileItem
import com.archieapps.fles.ui.model.FileKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FilesRepository(
    private val channelsRepository: ChannelsRepository,
    private val messagesRepository: MessagesRepository
) {
    fun observeFiles(
        folderId: String? = null,
        favoritesOnly: Boolean = false,
        query: String = "",
        kind: FileKind? = null
    ): Flow<Pair<com.archieapps.fles.ui.model.Channel?, List<FileItem>>> {
        return combine(
            channelsRepository.observeChannels(),
            channelsRepository.observeSecureChannels(),
            messagesRepository.observeAllMessages()
        ) { channels, secureChannels, messages ->
            val visibleChannels = channels + secureChannels
            val selectedFolder = folderId?.let { id -> visibleChannels.firstOrNull { it.id == id } }
            val selectedChannels = visibleChannels.filter { folderId == null || it.id == folderId }
            val channelById = selectedChannels.associateBy { it.id }
            val files = messages
                .filter { channelById.containsKey(it.folderId) }
                .flatMap { message -> message.toFileItem(channelById.getValue(message.folderId)) }
                .filter { folderId == null || it.folderId == folderId }
                .filter { !favoritesOnly || it.isFavorite }
                .filter { kind == null || it.kind == kind }
                .filter {
                    query.isBlank() ||
                        it.name.contains(query, ignoreCase = true) ||
                        it.folderName.contains(query, ignoreCase = true)
                }
                .sortedByDescending { it.createdAt }
            selectedFolder to files
        }
    }

    suspend fun getFiles(
        folderId: String? = null,
        favoritesOnly: Boolean = false,
        query: String = "",
        kind: FileKind? = null
    ): List<FileItem> {
        val channels = channelsRepository.getChannels()
        val visibleChannels = channels + channelsRepository.getSecureChannels()
        val selected = visibleChannels.filter { folderId == null || it.id == folderId }
        val files = selected.flatMap { channel ->
            messagesRepository.getMessages(channel.id).flatMap { it.toFileItem(channel) }
        }
        return files
            .filter { folderId == null || it.folderId == folderId }
            .filter { !favoritesOnly || it.isFavorite }
            .filter { kind == null || it.kind == kind }
            .filter {
                query.isBlank() ||
                    it.name.contains(query, ignoreCase = true) ||
                    it.folderName.contains(query, ignoreCase = true)
            }
            .sortedByDescending { it.createdAt }
    }

    suspend fun downloadToCache(file: FileItem, onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }): File {
        val message = messagesRepository.getMessages(file.folderId).firstOrNull { it.id == file.messageId }
        val attachment = message?.attachments?.firstOrNull { it.id == file.id }
            ?: throw IllegalStateException("Arquivo não encontrado")
        return messagesRepository.downloadAttachment(attachment, onProgress)
    }
}
