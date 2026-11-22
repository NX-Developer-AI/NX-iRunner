package com.nxteam.irunner.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nxteam.irunner.ui.detail.AppDetailScreen
import com.nxteam.irunner.ui.library.LibraryScreen
import com.nxteam.irunner.ui.logs.LogScreen
import com.nxteam.irunner.ui.settings.SettingsScreen

private object Routes {
    const val LIBRARY = "library"
    const val DETAIL = "detail/{bundleIdentifier}"
    const val SETTINGS = "settings"
    const val LOGS = "logs"

    fun detail(bundleIdentifier: String): String = "detail/${Uri.encode(bundleIdentifier)}"
}

@Composable
fun NxNavigation(incomingUris: List<Uri>, onIncomingUrisConsumed: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.LIBRARY) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                incomingUris = incomingUris,
                onIncomingUrisConsumed = onIncomingUrisConsumed,
                onOpenApp = { bundleIdentifier -> navController.navigate(Routes.detail(bundleIdentifier)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenLogs = { navController.navigate(Routes.LOGS) },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("bundleIdentifier") { type = NavType.StringType }),
        ) { entry ->
            AppDetailScreen(
                bundleIdentifier = entry.arguments?.getString("bundleIdentifier").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LOGS) {
            LogScreen(onBack = { navController.popBackStack() })
        }
    }
}
