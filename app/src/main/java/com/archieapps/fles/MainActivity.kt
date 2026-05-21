package com.archieapps.fles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.archieapps.fles.di.AppContainer
import com.archieapps.fles.ui.components.AppShell
import com.archieapps.fles.ui.components.MainSection
import com.archieapps.fles.ui.navigation.AppRoutes
import com.archieapps.fles.ui.screens.FilesScreen
import com.archieapps.fles.ui.screens.KeyScreen
import com.archieapps.fles.ui.screens.LoginScreen
import com.archieapps.fles.ui.screens.MessagesScreen
import com.archieapps.fles.ui.screens.SearchScreen
import com.archieapps.fles.ui.screens.ShareScreen
import com.archieapps.fles.ui.screens.ThreadScreen
import com.archieapps.fles.ui.state.UiState
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.components.openFile
import com.archieapps.fles.ui.viewmodel.FilesViewModel
import com.archieapps.fles.ui.viewmodel.KeyViewModel
import com.archieapps.fles.ui.viewmodel.LoginViewModel
import com.archieapps.fles.ui.viewmodel.MessagesViewModel
import com.archieapps.fles.ui.viewmodel.SearchViewModel
import com.archieapps.fles.ui.viewmodel.SessionState
import com.archieapps.fles.ui.viewmodel.SessionViewModel
import com.archieapps.fles.ui.viewmodel.ShareViewModel
import com.archieapps.fles.ui.viewmodel.SharePayload
import com.archieapps.fles.ui.viewmodel.ThreadViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appContainer = remember { AppContainer(applicationContext) }
            val initialShare = remember { SharePayload.fromIntent(intent, appContainer.attachmentIo) }
            FlesTheme {
                FlesApp(appContainer, initialShare)
            }
        }
    }
}

@Composable
fun FlesApp(appContainer: AppContainer, initialShare: SharePayload?) {
    val sessionViewModel = remember { SessionViewModel(appContainer.authRepository) }
    val sessionState by sessionViewModel.state.collectAsState()

    when (sessionState) {
        SessionState.Loading -> LoadingScreen()
        SessionState.Unauthenticated -> AuthNavHost(
            appContainer = appContainer,
            onAuthenticated = sessionViewModel::markAuthenticated
        )
        SessionState.Authenticated -> AuthenticatedNavHost(
            appContainer = appContainer,
            initialShare = initialShare,
            onLogout = sessionViewModel::logout
        )
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
    onAuthenticated: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppRoutes.Login) {
        composable(AppRoutes.Login) {
            val loginViewModel = remember { LoginViewModel(appContainer.authRepository) }
            val state by loginViewModel.state.collectAsState()
            LoginScreen(
                state = state,
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
                openDrawer = { navController.navigate(AppRoutes.Login) },
                onValueChange = keyViewModel::onValueChange,
                onSave = keyViewModel::save,
                onGenerate = keyViewModel::generate,
                onClear = keyViewModel::clear
            )
        }
    }
}

@Composable
private fun AuthenticatedNavHost(
    appContainer: AppContainer,
    initialShare: SharePayload?,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val messagesViewModel = remember { MessagesViewModel(appContainer.channelsRepository) }
    val channelsState by messagesViewModel.state.collectAsState()
    val channels = (channelsState as? UiState.Success)?.data.orEmpty()
    val createChannel: (String, String, String) -> Unit = { name, icon, color -> messagesViewModel.createChannel(name, icon, color) }

    NavHost(navController = navController, startDestination = if (initialShare != null) AppRoutes.Share else AppRoutes.Messages) {
        composable(AppRoutes.Messages) {
            Shell(navController, MainSection.Messages, null, channels, createChannel, onLogout) { openDrawer ->
                MessagesScreen(
                    state = channelsState,
                    openDrawer = openDrawer,
                    onThreadClick = { navController.navigate(AppRoutes.thread(it)) }
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
            Shell(navController, MainSection.Messages, folderId, channels, createChannel, onLogout) { openDrawer ->
                ThreadScreen(
                    state = state,
                    action = action,
                    channels = channels,
                    attachmentIo = appContainer.attachmentIo,
                    keyConfigured = keyConfigured,
                    openDrawer = openDrawer,
                    onBack = { navController.navigate(AppRoutes.Messages) },
                    onDeleteThread = { threadViewModel.deleteThread { navController.navigate(AppRoutes.Messages) } },
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
                    onDownload = threadViewModel::downloadToCache
                )
            }
        }
        composable(AppRoutes.Files) {
            val filesViewModel = remember {
                FilesViewModel(null, false, appContainer.channelsRepository, appContainer.filesRepository)
            }
            val state by filesViewModel.state.collectAsState()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            Shell(navController, MainSection.Files, null, channels, createChannel, onLogout) { openDrawer ->
                FilesScreen(
                    state = state,
                    openDrawer = openDrawer,
                    onQueryChange = filesViewModel::onQueryChange,
                    onModeChange = filesViewModel::onModeChange,
                    onFavoritesToggle = filesViewModel::onFavoritesToggle,
                    onKindChange = filesViewModel::onKindChange,
                    onFileClick = { file -> scope.launch { openFile(context, filesViewModel.downloadToCache(file), file.mimeType) } }
                )
            }
        }
        composable(
            route = AppRoutes.FolderFiles,
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId").orEmpty()
            val filesViewModel = remember(folderId) {
                FilesViewModel(folderId, false, appContainer.channelsRepository, appContainer.filesRepository)
            }
            val state by filesViewModel.state.collectAsState()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            Shell(navController, MainSection.Files, folderId, channels, createChannel, onLogout) { openDrawer ->
                FilesScreen(
                    state = state,
                    openDrawer = openDrawer,
                    onQueryChange = filesViewModel::onQueryChange,
                    onModeChange = filesViewModel::onModeChange,
                    onFavoritesToggle = filesViewModel::onFavoritesToggle,
                    onKindChange = filesViewModel::onKindChange,
                    onFileClick = { file -> scope.launch { openFile(context, filesViewModel.downloadToCache(file), file.mimeType) } }
                )
            }
        }
        composable(AppRoutes.Favorites) {
            val filesViewModel = remember {
                FilesViewModel(null, true, appContainer.channelsRepository, appContainer.filesRepository)
            }
            val state by filesViewModel.state.collectAsState()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            Shell(navController, MainSection.Favorites, null, channels, createChannel, onLogout) { openDrawer ->
                FilesScreen(
                    state = state,
                    openDrawer = openDrawer,
                    onQueryChange = filesViewModel::onQueryChange,
                    onModeChange = filesViewModel::onModeChange,
                    onFavoritesToggle = filesViewModel::onFavoritesToggle,
                    onKindChange = filesViewModel::onKindChange,
                    onFileClick = { file -> scope.launch { openFile(context, filesViewModel.downloadToCache(file), file.mimeType) } },
                    favoritesOnlyInitial = true
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
            Shell(navController, MainSection.Search, null, channels, createChannel, onLogout) { openDrawer ->
                SearchScreen(
                    state = state,
                    openDrawer = openDrawer,
                    onQueryChange = searchViewModel::onQueryChange,
                    onThreadClick = { navController.navigate(AppRoutes.thread(it)) }
                )
            }
        }
        composable(AppRoutes.Key) {
            val keyViewModel = remember { KeyViewModel(appContainer.cryptoRepository) }
            val state by keyViewModel.state.collectAsState()
            Shell(navController, MainSection.Key, null, channels, createChannel, onLogout) { openDrawer ->
                KeyScreen(
                    state = state,
                    openDrawer = openDrawer,
                    onValueChange = keyViewModel::onValueChange,
                    onSave = keyViewModel::save,
                    onGenerate = keyViewModel::generate,
                    onClear = keyViewModel::clear
                )
            }
        }
        composable(AppRoutes.Share) {
            val shareViewModel = remember { ShareViewModel(appContainer.channelsRepository, appContainer.messagesRepository, initialShare) }
            val state by shareViewModel.state.collectAsState()
            ShareScreen(
                state = state,
                onQueryChange = shareViewModel::onQueryChange,
                onSend = { channelId ->
                    shareViewModel.sendTo(channelId) {
                        navController.navigate(AppRoutes.thread(channelId))
                    }
                },
                onClose = { navController.navigate(AppRoutes.Messages) },
                onThreadClick = { navController.navigate(AppRoutes.thread(it)) },
                onCreateChannel = createChannel
            )
        }
    }
}

@Composable
private fun Shell(
    navController: NavHostController,
    section: MainSection,
    activeFolderId: String?,
    channels: List<com.archieapps.fles.ui.model.Channel>,
    onCreateChannel: (String, String, String) -> Unit,
    onLogout: () -> Unit,
    content: @Composable (openDrawer: () -> Unit) -> Unit
) {
    AppShell(
        section = section,
        activeFolderId = activeFolderId,
        channels = channels,
        onNavigateMessages = { navController.navigate(AppRoutes.Messages) },
        onNavigateThread = { navController.navigate(AppRoutes.thread(it)) },
        onNavigateFiles = { navController.navigate(AppRoutes.Files) },
        onNavigateFolderFiles = { navController.navigate(AppRoutes.folderFiles(it)) },
        onNavigateFavorites = { navController.navigate(AppRoutes.Favorites) },
        onNavigateKey = { navController.navigate(AppRoutes.Key) },
        onNavigateSearch = { navController.navigate(AppRoutes.search(it)) },
        onCreateChannel = onCreateChannel,
        onLogout = onLogout,
        content = content
    )
}
