package com.archieapps.fles.di

import android.content.Context
import com.archieapps.fles.data.local.AppLockStorage
import com.archieapps.fles.data.local.KeyStorage
import com.archieapps.fles.data.local.SecureFolderStorage
import com.archieapps.fles.data.local.db.FlesDatabase
import com.archieapps.fles.data.remote.ApiClient
import com.archieapps.fles.data.repository.AuthRepository
import com.archieapps.fles.data.repository.AttachmentIo
import com.archieapps.fles.data.repository.ChannelsRepository
import com.archieapps.fles.data.repository.CryptoRepository
import com.archieapps.fles.data.repository.FilesRepository
import com.archieapps.fles.data.repository.MessagesRepository
import com.archieapps.fles.data.repository.SecureFoldersRepository
import com.archieapps.fles.sync.NetworkMonitor
import com.archieapps.fles.sync.SyncEngine
import com.archieapps.fles.sync.SyncQueueRepository
import com.archieapps.fles.sync.SyncScheduler
import com.archieapps.fles.sync.SyncStatusStore

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val apiClient = ApiClient(context.applicationContext)
    val database = FlesDatabase.create(appContext)
    val attachmentIo = AttachmentIo(context.applicationContext)
    val cryptoRepository = CryptoRepository(KeyStorage(context.applicationContext))
    val appLockStorage = AppLockStorage(context.applicationContext)
    val secureFoldersRepository = SecureFoldersRepository(
        apiService = apiClient.apiService,
        cryptoRepository = cryptoRepository,
        attachmentIo = attachmentIo,
        storage = SecureFolderStorage(context.applicationContext)
    )
    val networkMonitor = NetworkMonitor(appContext)
    val syncStatusStore = SyncStatusStore()
    val syncScheduler = SyncScheduler(appContext)
    val syncQueueRepository = SyncQueueRepository(database.syncQueueDao())

    val authRepository = AuthRepository(
        apiService = apiClient.apiService,
        tokenStorage = apiClient.tokenStorage,
        database = database,
        secureFoldersRepository = secureFoldersRepository
    )
    val channelsRepository = ChannelsRepository(
        apiService = apiClient.apiService,
        cryptoRepository = cryptoRepository,
        secureFoldersRepository = secureFoldersRepository,
        channelDao = database.channelDao(),
        queueRepository = syncQueueRepository,
        scheduler = syncScheduler
    )
    val messagesRepository = MessagesRepository(
        apiService = apiClient.apiService,
        cryptoRepository = cryptoRepository,
        secureFoldersRepository = secureFoldersRepository,
        channelsRepository = channelsRepository,
        attachmentIo = attachmentIo,
        messageDao = database.messageDao(),
        listItemDao = database.listItemDao(),
        attachmentDao = database.attachmentDao(),
        queueRepository = syncQueueRepository,
        scheduler = syncScheduler,
        networkMonitor = networkMonitor
    )
    val filesRepository = FilesRepository(
        channelsRepository = channelsRepository,
        messagesRepository = messagesRepository
    )
    val syncEngine = SyncEngine(
        cryptoRepository = cryptoRepository,
        networkMonitor = networkMonitor,
        statusStore = syncStatusStore,
        queueDao = database.syncQueueDao(),
        syncStateDao = database.syncStateDao(),
        channelsRepository = channelsRepository,
        messagesRepository = messagesRepository
    )

    init {
        syncScheduler.schedulePeriodic()
    }
}
