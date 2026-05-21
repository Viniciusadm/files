package com.archieapps.fles.data.repository

import com.archieapps.fles.data.remote.ApiService
import com.archieapps.fles.data.remote.requireBody
import com.archieapps.fles.ui.model.Channel
import com.archieapps.fles.ui.model.Message

class MessagesRepository(
    private val apiService: ApiService,
    private val cryptoRepository: CryptoRepository,
    private val channelsRepository: ChannelsRepository
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
}

data class SearchResult(
    val channel: Channel,
    val message: Message
)
