package com.quantedge.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quantedge.app.ui.screens.dashboard.DashboardScreen
import com.quantedge.app.ui.screens.detail.StockDetailScreen
import com.quantedge.app.ui.screens.analysis.ScreenerScreen
import com.quantedge.app.ui.screens.watchlist.WatchlistScreen
import com.quantedge.app.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object StockDetail : Screen("stock/{symbol}") {
        fun createRoute(symbol: String) = "stock/$symbol"
    }
    object Screener : Screen("screener")
    object Watchlist : Screen("watchlist")
    object Settings : Screen("settings")
}

@Composable
fun QuantEdgeNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(280)) +
            fadeIn(tween(280))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(280)) +
            fadeOut(tween(280))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(280)) +
            fadeIn(tween(280))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(280)) +
            fadeOut(tween(280))
        }
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onStockClick = { symbol -> navController.navigate(Screen.StockDetail.createRoute(symbol)) },
                onScreenerClick = { navController.navigate(Screen.Screener.route) },
                onWatchlistClick = { navController.navigate(Screen.Watchlist.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = Screen.StockDetail.route,
            arguments = listOf(navArgument("symbol") { type = NavType.StringType })
        ) { backStackEntry ->
            val symbol = backStackEntry.arguments?.getString("symbol") ?: return@composable
            StockDetailScreen(
                symbol = symbol,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Screener.route) {
            ScreenerScreen(
                onStockClick = { symbol -> navController.navigate(Screen.StockDetail.createRoute(symbol)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Watchlist.route) {
            WatchlistScreen(
                onStockClick = { symbol -> navController.navigate(Screen.StockDetail.createRoute(symbol)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
