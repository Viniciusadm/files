package com.archieapps.fles.data.repository

import com.archieapps.fles.data.remote.ApiService
import com.archieapps.fles.data.remote.requireBody
import com.archieapps.fles.ui.model.Channel

class ChannelsRepository(
    private val apiService: ApiService,
    private val cryptoRepository: CryptoRepository
) {
    suspend fun getChannels(): List<Channel> {
        return apiService.channels()
            .requireBody()
            .map { it.toUi(cryptoRepository) }
            .sortedByDescending { it.updatedAt }
    }

    suspend fun getChannel(id: String): Channel? {
        return getChannels().firstOrNull { it.id == id }
    }
}
