package com.archieapps.fles

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.archieapps.fles.di.AppContainer
import com.archieapps.fles.security.BiometricAuthenticator
import com.archieapps.fles.ui.components.AppShell
import com.archieapps.fles.ui.components.MainSection
import com.archieapps.fles.ui.navigation.AppRoutes
import com.archieapps.fles.ui.screens.AppLockScreen
import com.archieapps.fles.ui.screens.FilesExplorerScreen
import com.archieapps.fles.ui.screens.GalleryScreen
import com.archieapps.fles.ui.screens.KeyScreen
import com.archieapps.fles.ui.screens.LoginScreen
import com.archieapps.fles.ui.screens.MessagesScreen
import com.archieapps.fles.ui.screens.SearchScreen
import com.archieapps.fles.ui.screens.SecureFoldersScreen
import com.archieapps.fles.ui.screens.SettingsScreen
import com.archieapps.fles.ui.screens.ThreadScreen
import com.archieapps.fles.ui.state.UiState
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.components.openFile
import com.archieapps.fles.ui.viewmodel.AppLockViewModel
import com.archieapps.fles.ui.viewmodel.FilesViewModel
import com.archieapps.fles.ui.viewmodel.KeyViewModel
import com.archieapps.fles.ui.viewmodel.LoginViewModel
import com.archieapps.fles.ui.viewmodel.MessagesViewModel
import com.archieapps.fles.ui.viewmodel.SearchViewModel
import com.archieapps.fles.ui.viewmodel.SecureFoldersViewModel
import com.archieapps.fles.ui.viewmodel.SessionState
import com.archieapps.fles.ui.viewmodel.SessionViewModel
import com.archieapps.fles.ui.viewmodel.ThreadViewModel

class MainActivity : FragmentActivity() {

    private lateinit var appLockViewModel: AppLockViewModel
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> appLockViewModel.onAppBackgrounded()
            Lifecycle.Event.ON_START -> appLockViewModel.onAppForegrounded()
            else -> Unit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as FlesApp).container
        appLockViewModel = AppLockViewModel(applicationContext, appContainer.appLockStorage)
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        setContent {
            FlesTheme {
                FlesAppRoot(appContainer, appLockViewModel, this)
            }
        }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        appLockViewModel.clear()
        super.onDestroy()
    }
}

@Composable
fun FlesAppRoot(
    appContainer: AppContainer,
    appLockViewModel: AppLockViewModel,
    activity: FragmentActivity
) {
    val sessionViewModel = remember { SessionViewModel(appContainer.authRepository) }
    val sessionState by sessionViewModel.state.collectAsState()
    val lockState by appLockViewModel.state.collectAsState()

    DisposableEffect(sessionState) {
        if (sessionState != SessionState.Authenticated) {
            appLockViewModel.resetUnlocked()
        }
        onDispose { }
    }

    when (sessionState) {
        SessionState.Loading -> LoadingScreen()
        SessionState.Unauthenticated -> AuthNavHost(
            appContainer = appContainer,
            activity = activity,
            onAuthenticated = {
                appLockViewModel.markUnlocked()
                sessionViewModel.markAuthenticated()
            }
        )
        SessionState.Authenticated -> {
            if (lockState.enabled && !lockState.unlocked) {
                AppLockScreen(
                    availability = lockState.availability,
                    onUnlockClick = {
                        BiometricAuthenticator.prompt(
                            activity = activity,
                            title = "Desbloquear aplicativo",
                            subtitle = "Use sua biometria ou credencial do dispositivo",
                            onSuccess = { appLockViewModel.markUnlocked() }
                        )
                    }
                )
            } else {
                AuthenticatedNavHost(
                    appContainer = appContainer,
                    appLockViewModel = appLockViewModel,
                    activity = activity,
                    onLogout = {
                        appLockViewModel.resetUnlocked()
                        sessionViewModel.logout()
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlesTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = FlesTheme.colors.primary)
    }
}

@Composable
private fun AuthNavHost(
    appContainer: AppContainer,
    activity: FragmentActivity,
    onAuthenticated: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppRoutes.Login,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(AppRoutes.Login) {
            val loginViewModel = remember { LoginViewModel(appContainer.authRepository) }
            val state by loginViewModel.state.collectAsState()
            var keyConfigured by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { keyConfigured = appContainer.cryptoRepository.hasKey() }
            LoginScreen(
                state = state,
                keyConfigured = keyConfigured,
                onEmailChange = loginViewModel::onEmailChange,
                onPasswordChange = loginViewModel::onPasswordChange,
                onLoginClick = { loginViewModel.login(onAuthenticated) },
                onKeyClick = { navController.navigate(AppRoutes.Key) }
            )
        }
        composable(AppRoutes.Key) {
            val keyViewModel = remember { KeyViewModel(appContainer.cryptoRepository) }
            val state by keyViewModel.state.collectAsState()
            KeyScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onValueChange = keyViewModel::onValueChange,
                onSave = { keyViewModel.save { activity.recreate() } },
                onGenerate = keyViewModel::generate
            )
        }
    }
}

@Composable
private fun AuthenticatedNavHost(
    appContainer: AppContainer,
    appLockViewModel: AppLockViewModel,
    activity: FragmentActivity,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    LaunchedEffect(Unit) {
        appContainer.syncScheduler.scheduleNow()
    }
    val messagesViewModel = remember { MessagesViewModel(appContainer.channelsRepository) }
    val channelsState by messagesViewModel.state.collectAsState()
    val channels = (channelsState as? UiState.Success)?.data.orEmpty()
    val secureState by appContainer.secureFoldersRepository.state.collectAsState()
    var secureChannels by remember { mutableStateOf(emptyList<com.archieapps.fles.ui.model.Channel>()) }
    LaunchedEffect(Unit) {
        appContainer.channelsRepository.observeSecureChannels().collect { secureChannels = it }
    }
    val homeChannelsState = when (val state = channelsState) {
        UiState.Loading -> UiState.Loading
        UiState.Empty -> if (secureChannels.isEmpty()) UiState.Empty else UiState.Success(secureChannels)
        is UiState.Error -> state
        is UiState.Success -> {
            val homeChannels = state.data + secureChannels
            if (homeChannels.isEmpty()) UiState.Empty else UiState.Success(homeChannels)
        }
    }
    val allChannels = channels + secureChannels
    val createChannel: (String, String, String, Boolean) -> Unit = { name, icon, color, isSecure -> messagesViewModel.createChannel(name, icon, color, isSecure) }
    val createSubfolder: (String, String, String, Boolean, String) -> Unit = { name, icon, color, isSecure, parentLocalId ->
        messagesViewModel.createChannel(name, icon, color, isSecure, parentLocalId)
    }

    val openSettings: () -> Unit = { navController.navigate(AppRoutes.Settings) }
    val navigateMessages: () -> Unit = {
        navController.navigate(AppRoutes.Messages) {
            popUpTo(AppRoutes.Messages) { inclusive = true }
            launchSingleTop = true
        }
    }
    val navigateGallery: () -> Unit = {
        navController.navigate(AppRoutes.Gallery) {
            popUpTo(AppRoutes.Messages)
            launchSingleTop = true
        }
    }
    val navigateFiles: () -> Unit = {
        navController.navigate(AppRoutes.Files) {
            popUpTo(AppRoutes.Messages)
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.Messages,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(AppRoutes.Messages) {
            Shell(MainSection.Messages, navigateMessages, navigateGallery, navigateFiles) {
                MessagesScreen(
                    state = homeChannelsState,
                    onOpenSettings = openSettings,
                    onThreadClick = { navController.navigate(AppRoutes.thread(it)) },
                    onNavigateSearch = { navController.navigate(AppRoutes.search(it)) }
                )
            }
        }
        composable(
            route = AppRoutes.Thread,
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId").orEmpty()
            val threadViewModel = remember(folderId) {
                ThreadViewModel(folderId, appContainer.channelsRepository, appContainer.messagesRepository)
            }
            val state by threadViewModel.state.collectAsState()
            val action by threadViewModel.action.collectAsState()
            var keyConfigured by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { keyConfigured = appContainer.cryptoRepository.hasKey() }
            Shell(MainSection.Messages, navigateMessages, navigateGallery, navigateFiles, showBottomNav = false) {
                ThreadScreen(
                    state = state,
                    action = action,
                    channels = allChannels,
                    attachmentIo = appContainer.attachmentIo,
                    keyConfigured = keyConfigured,
                    onOpenSettings = openSettings,
                    onBack = { navController.popBackStack() },
                    onDeleteThread = { threadViewModel.deleteThread { navController.popBackStack() } },
                    onCreateSubfolder = { name, icon, color, isSecure -> createSubfolder(name, icon, color, isSecure, folderId) },
                    onSend = { text, attachments -> threadViewModel.send(text, attachments) {} },
                    onCreateList = { title, items -> threadViewModel.createChecklist(title, items) {} },
                    onConfigureKey = { navController.navigate(AppRoutes.Key) },
                    onToggleFavorite = threadViewModel::toggleFavorite,
                    onDeleteMessage = threadViewModel::deleteMessage,
                    onMoveMessage = threadViewModel::moveMessage,
                    onToggleListItem = threadViewModel::toggleListItem,
                    onAddListItem = threadViewModel::addListItem,
                    onUpdateListItem = threadViewModel::updateListItem,
                    onDeleteListItem = threadViewModel::deleteListItem,
                    onReorderListItems = threadViewModel::reorderListItems,
                    onRetryUpload = threadViewModel::retryUpload,
                    onCancelUpload = threadViewModel::cancelUpload,
                    onDownload = threadViewModel::downloadToCache
                )
            }
        }
        composable(AppRoutes.Gallery) {
            val filesViewModel = remember(secureState.unlocked) {
                FilesViewModel(null, false, appContainer.channelsRepository, appContainer.filesRepository, appContainer.messagesRepository)
            }
            val state by filesViewModel.state.collectAsState()
            val context = LocalContext.current
            Shell(MainSection.Gallery, navigateMessages, navigateGallery, navigateFiles) {
                GalleryScreen(
                    state = state,
                    onOpenSettings = openSettings,
                    onQueryChange = filesViewModel::onQueryChange,
                    onModeChange = filesViewModel::onModeChange,
                    onFavoritesToggle = filesViewModel::onFavoritesToggle,
                    onKindChange = filesViewModel::onKindChange,
                    onGroupModeChange = filesViewModel::onGalleryGroupModeChange,
                    onSelectGalleryFolder = filesViewModel::onSelectGalleryFolder,
                    onEnsureLocalFile = filesViewModel::ensureLocalFile,
                    onOpenFile = { file, localFile -> openFile(context, localFile, file.mimeType).getOrThrow() }
                )
            }
        }
        composable(AppRoutes.Files) {
            val filesViewModel = remember(secureState.unlocked) {
                FilesViewModel(null, false, appContainer.channelsRepository, appContainer.filesRepository, appContainer.messagesRepository)
            }
            val state by filesViewModel.state.collectAsState()
            val context = LocalContext.current
            Shell(MainSection.Files, navigateMessages, navigateGallery, navigateFiles) {
                FilesExplorerScreen(
                    state = state,
                    allChannels = allChannels,
                    onOpenSettings = openSettings,
                    onBack = null,
                    onOpenFolder = { navController.navigate(AppRoutes.folderFiles(it)) },
                    onQueryChange = filesViewModel::onQueryChange,
                    onEnsureLocalFile = filesViewModel::ensureLocalFile,
                    onOpenFile = { file, localFile -> openFile(context, localFile, file.mimeType).getOrThrow() },
                    onToggleSelection = filesViewModel::toggleSelection,
                    onClearSelection = filesViewModel::clearSelection,
                    onBulkDelete = filesViewModel::bulkDelete,
                    onBulkMove = filesViewModel::bulkMove,
                    onDeleteFile = filesViewModel::deleteFile,
                    onMoveFile = filesViewModel::moveFile,
                    onToggleFavorite = filesViewModel::toggleFavorite,
                    onRenameFile = filesViewModel::renameFile
                )
            }
        }
        composable(
            route = AppRoutes.FolderFiles,
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId").orEmpty()
            val filesViewModel = remember(folderId, secureState.unlocked) {
                FilesViewModel(folderId, false, appContainer.channelsRepository, appContainer.filesRepository, appContainer.messagesRepository)
            }
            val state by filesViewModel.state.collectAsState()
            val context = LocalContext.current
            Shell(MainSection.Files, navigateMessages, navigateGallery, navigateFiles, showBottomNav = false) {
                FilesExplorerScreen(
                    state = state,
                    allChannels = allChannels,
                    onOpenSettings = openSettings,
                    onBack = { navController.popBackStack() },
                    onOpenFolder = { navController.navigate(AppRoutes.folderFiles(it)) },
                    onQueryChange = filesViewModel::onQueryChange,
                    onEnsureLocalFile = filesViewModel::ensureLocalFile,
                    onOpenFile = { file, localFile -> openFile(context, localFile, file.mimeType).getOrThrow() },
                    onToggleSelection = filesViewModel::toggleSelection,
                    onClearSelection = filesViewModel::clearSelection,
                    onBulkDelete = filesViewModel::bulkDelete,
                    onBulkMove = filesViewModel::bulkMove,
                    onDeleteFile = filesViewModel::deleteFile,
                    onMoveFile = filesViewModel::moveFile,
                    onToggleFavorite = filesViewModel::toggleFavorite,
                    onRenameFile = filesViewModel::renameFile
                )
            }
        }
        composable(AppRoutes.Settings) {
            val lockState by appLockViewModel.state.collectAsState()
            Shell(MainSection.Settings, navigateMessages, navigateGallery, navigateFiles, showBottomNav = false) {
                SettingsScreen(
                    secureUnlocked = secureState.unlocked,
                    appLockEnabled = lockState.enabled,
                    appLockAvailability = lockState.availability,
                    onBack = { navController.popBackStack() },
                    onCreateChannel = createChannel,
                    onNavigateKey = { navController.navigate(AppRoutes.Key) },
                    onNavigateSecureFolders = { navController.navigate(AppRoutes.SecureFolders) },
                    onEnableAppLock = {
                        BiometricAuthenticator.prompt(
                            activity = activity,
                            title = "Ativar bloqueio do aplicativo",
                            subtitle = "Confirme sua biometria ou credencial do dispositivo",
                            onSuccess = { appLockViewModel.setEnabled(true) }
                        )
                    },
                    onDisableAppLock = { appLockViewModel.setEnabled(false) },
                    onRefreshAppLockAvailability = { appLockViewModel.refreshAvailability() },
                    onLogout = onLogout
                )
            }
        }
        composable(
            route = AppRoutes.Search,
            arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val initialQuery = backStackEntry.arguments?.getString("query").orEmpty()
            val searchViewModel = remember(initialQuery) {
                SearchViewModel(initialQuery, appContainer.messagesRepository)
            }
            val state by searchViewModel.state.collectAsState()
            Shell(MainSection.Search, navigateMessages, navigateGallery, navigateFiles, showBottomNav = false) {
                SearchScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onQueryChange = searchViewModel::onQueryChange,
                    onThreadClick = { navController.navigate(AppRoutes.thread(it)) }
                )
            }
        }
        composable(AppRoutes.Key) {
            val keyViewModel = remember { KeyViewModel(appContainer.cryptoRepository) }
            val state by keyViewModel.state.collectAsState()
            Shell(MainSection.Key, navigateMessages, navigateGallery, navigateFiles, showBottomNav = false) {
                KeyScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onValueChange = keyViewModel::onValueChange,
                    onSave = { keyViewModel.save { activity.recreate() } },
                    onGenerate = keyViewModel::generate
                )
            }
        }
        composable(AppRoutes.SecureFolders) {
            val secureViewModel = remember { SecureFoldersViewModel(appContainer.secureFoldersRepository) }
            val state by secureViewModel.state.collectAsState()
            Shell(MainSection.SecureFolders, navigateMessages, navigateGallery, navigateFiles, showBottomNav = false) {
                SecureFoldersScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onPinChange = secureViewModel::onPinChange,
                    onSetup = { secureViewModel.setup { navController.popBackStack() } },
                    onUnlock = { secureViewModel.unlock { navController.popBackStack() } },
                    onReset = { secureViewModel.reset { navController.popBackStack() } },
                    onLock = secureViewModel::lock
                )
            }
        }
    }
}

@Composable
private fun Shell(
    section: MainSection,
    onNavigateMessages: () -> Unit,
    onNavigateGallery: () -> Unit,
    onNavigateFiles: () -> Unit,
    showBottomNav: Boolean = true,
    content: @Composable () -> Unit
) {
    AppShell(
        section = section,
        onNavigateMessages = onNavigateMessages,
        onNavigateGallery = onNavigateGallery,
        onNavigateFiles = onNavigateFiles,
        showBottomNav = showBottomNav,
        content = content
    )
}
