package com.archieapps.fles.data.repository

import com.archieapps.fles.data.remote.ApiService
import com.archieapps.fles.data.remote.requireBody
import com.archieapps.fles.data.remote.requireSuccess
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

    suspend fun createChannel(name: String, icon: String, color: String): Channel {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Informe um nome" }
        return apiService.createFolder(
            com.archieapps.fles.data.remote.CreateFolderRequest(
                name = cryptoRepository.encryptString(trimmed),
                parentId = null,
                icon = icon,
                color = color
            )
        ).requireBody().toUi(cryptoRepository)
    }

    suspend fun deleteChannel(id: String) {
        apiService.deleteFolder(id).requireSuccess()
    }
}
