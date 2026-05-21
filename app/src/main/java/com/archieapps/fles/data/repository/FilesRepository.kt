package com.archieapps.fles.data.repository

import com.archieapps.fles.ui.model.FileItem
import com.archieapps.fles.ui.model.FileKind

class FilesRepository(
    private val channelsRepository: ChannelsRepository,
    private val messagesRepository: MessagesRepository
) {
    suspend fun getFiles(
        folderId: String? = null,
        favoritesOnly: Boolean = false,
        query: String = "",
        kind: FileKind? = null
    ): List<FileItem> {
        val channels = channelsRepository.getChannels()
        val selected = channels.filter { folderId == null || it.id == folderId }
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
}
