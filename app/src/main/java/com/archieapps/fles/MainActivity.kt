package com.archieapps.fles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.archieapps.fles.ui.components.AppShell
import com.archieapps.fles.ui.components.MainSection
import com.archieapps.fles.ui.screens.FilesScreen
import com.archieapps.fles.ui.screens.KeyScreen
import com.archieapps.fles.ui.screens.LoginScreen
import com.archieapps.fles.ui.screens.MessagesScreen
import com.archieapps.fles.ui.screens.SearchScreen
import com.archieapps.fles.ui.screens.ShareScreen
import com.archieapps.fles.ui.screens.ThreadScreen
import com.archieapps.fles.ui.theme.FlesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlesTheme {
                FlesApp()
            }
        }
    }
}

@Composable
fun FlesApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onEnter = { navController.navigate("messages") { popUpTo("login") { inclusive = true } } },
                onKeyClick = { navController.navigate("key") }
            )
        }
        composable("messages") {
            Shell(navController, MainSection.Messages, null) { openDrawer ->
                MessagesScreen(openDrawer = openDrawer, onThreadClick = { navController.navigate("thread/$it") })
            }
        }
        composable(
            route = "thread/{folderId}",
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId").orEmpty()
            Shell(navController, MainSection.Messages, folderId) { openDrawer ->
                ThreadScreen(folderId = folderId, openDrawer = openDrawer, onBack = { navController.navigate("messages") })
            }
        }
        composable("files") {
            Shell(navController, MainSection.Files, null) { openDrawer ->
                FilesScreen(openDrawer = openDrawer)
            }
        }
        composable(
            route = "files/{folderId}",
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId").orEmpty()
            Shell(navController, MainSection.Files, folderId) { openDrawer ->
                FilesScreen(openDrawer = openDrawer, folderId = folderId)
            }
        }
        composable("favorites") {
            Shell(navController, MainSection.Favorites, null) { openDrawer ->
                FilesScreen(openDrawer = openDrawer, favoritesOnlyInitial = true)
            }
        }
        composable(
            route = "search?query={query}",
            arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            Shell(navController, MainSection.Search, null) { openDrawer ->
                SearchScreen(
                    openDrawer = openDrawer,
                    initialQuery = backStackEntry.arguments?.getString("query").orEmpty(),
                    onThreadClick = { navController.navigate("thread/$it") }
                )
            }
        }
        composable("key") {
            Shell(navController, MainSection.Key, null) { openDrawer ->
                KeyScreen(openDrawer = openDrawer)
            }
        }
        composable("share") {
            ShareScreen(
                onClose = { navController.navigate("messages") },
                onThreadClick = { navController.navigate("thread/$it") }
            )
        }
    }
}

@Composable
private fun Shell(
    navController: NavHostController,
    section: MainSection,
    activeFolderId: String?,
    content: @Composable (openDrawer: () -> Unit) -> Unit
) {
    AppShell(
        section = section,
        activeFolderId = activeFolderId,
        onNavigateMessages = { navController.navigate("messages") },
        onNavigateThread = { navController.navigate("thread/$it") },
        onNavigateFiles = { navController.navigate("files") },
        onNavigateFolderFiles = { navController.navigate("files/$it") },
        onNavigateFavorites = { navController.navigate("favorites") },
        onNavigateKey = { navController.navigate("key") },
        onNavigateSearch = { navController.navigate("search?query=$it") },
        content = content
    )
}
