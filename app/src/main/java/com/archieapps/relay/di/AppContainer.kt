package com.archieapps.relay.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.archieapps.relay.data.local.AppLockStorage
import com.archieapps.relay.data.local.KeyStorage
import com.archieapps.relay.data.local.SecureFolderStorage
import com.archieapps.relay.data.local.db.FlesDatabase
import com.archieapps.relay.data.remote.ApiClient
import com.archieapps.relay.data.repository.AuthRepository
import com.archieapps.relay.data.repository.AttachmentIo
import com.archieapps.relay.data.repository.ChannelsRepository
import com.archieapps.relay.data.repository.CryptoRepository
import com.archieapps.relay.data.repository.DownloadCenter
import com.archieapps.relay.data.repository.FilesRepository
import com.archieapps.relay.data.repository.MessagesRepository
import com.archieapps.relay.data.repository.PairingRepository
import com.archieapps.relay.data.repository.SecureFoldersRepository
import com.archieapps.relay.sync.NetworkMonitor
import com.archieapps.relay.sync.SyncEngine
import com.archieapps.relay.sync.SyncQueueRepository
import com.archieapps.relay.sync.SyncScheduler
import com.archieapps.relay.sync.SyncStatusStore

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
        secureFoldersRepository = secureFoldersRepository,
        cryptoRepository = cryptoRepository
    )
    val pairingRepository = PairingRepository(apiClient.apiService, cryptoRepository)
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
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val downloadCenter = DownloadCenter(
        scope = downloadScope,
        filesRepository = filesRepository
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
