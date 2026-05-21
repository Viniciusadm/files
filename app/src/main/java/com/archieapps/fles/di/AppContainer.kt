package com.archieapps.fles.di

import android.content.Context
import com.archieapps.fles.data.local.KeyStorage
import com.archieapps.fles.data.remote.ApiClient
import com.archieapps.fles.data.repository.AuthRepository
import com.archieapps.fles.data.repository.ChannelsRepository
import com.archieapps.fles.data.repository.CryptoRepository
import com.archieapps.fles.data.repository.FilesRepository
import com.archieapps.fles.data.repository.MessagesRepository

class AppContainer(context: Context) {
    private val apiClient = ApiClient(context.applicationContext)
    val cryptoRepository = CryptoRepository(KeyStorage(context.applicationContext))

    val authRepository = AuthRepository(
        apiService = apiClient.apiService,
        tokenStorage = apiClient.tokenStorage
    )
    val channelsRepository = ChannelsRepository(
        apiService = apiClient.apiService,
        cryptoRepository = cryptoRepository
    )
    val messagesRepository = MessagesRepository(
        apiService = apiClient.apiService,
        cryptoRepository = cryptoRepository,
        channelsRepository = channelsRepository
    )
    val filesRepository = FilesRepository(
        channelsRepository = channelsRepository,
        messagesRepository = messagesRepository
    )
}
