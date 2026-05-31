package com.lowderancorp.inioli.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.lowderancorp.inioli.ui.screens.HomeScreen
import com.lowderancorp.inioli.ui.screens.LoginScreen
import com.lowderancorp.inioli.ui.screens.ReceiveStockDetailScreen
import com.lowderancorp.inioli.ui.screens.ReceiveStockScreen
import com.lowderancorp.inioli.ui.screens.ReturnScreen
import com.lowderancorp.inioli.ui.screens.SaleScreen
import com.lowderancorp.inioli.ui.screens.SplashScreen
import com.lowderancorp.inioli.ui.screens.StockAdjustmentScreen

private object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val Home = "home"
    const val ReceiveStock = "receive_stock"
    const val ReceiveStockDetail = "receive_stock/{stockJourneyId}"
    const val Sale = "sale"
    const val ReturnStock = "return_stock"
    const val StockAdjustment = "stock_adjustment"

    fun receiveStockDetail(stockJourneyId: Int): String {
        return "receive_stock/$stockJourneyId"
    }
}

@Composable
fun InioliApp(
    appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
) {
    val navController = rememberNavController()
    val loginUiState by appViewModel.loginUiState.collectAsStateWithLifecycle()
    val sessionState by appViewModel.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        val destinationRoute = when (sessionState) {
            SessionState.Loading -> Routes.Splash
            SessionState.LoggedOut -> Routes.Login
            is SessionState.LoggedIn -> Routes.Home
        }

        if (navController.currentDestination?.route != destinationRoute) {
            navController.navigate(destinationRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {
        composable(Routes.Splash) {
            SplashScreen()
        }
        composable(Routes.Login) {
            LoginScreen(
                uiState = loginUiState,
                onUsernameChange = appViewModel::onUsernameChange,
                onPasswordChange = appViewModel::onPasswordChange,
                onLoginClick = appViewModel::login
            )
        }
        composable(Routes.Home) {
            val loggedInState = sessionState as? SessionState.LoggedIn
            if (loggedInState == null) {
                SplashScreen()
            } else {
                HomeScreen(
                    username = loggedInState.session.username,
                    onLogoutClick = appViewModel::logout,
                    onReceiveStockClick = { navController.navigate(Routes.ReceiveStock) },
                    onSaleClick = { navController.navigate(Routes.Sale) },
                    onReturnClick = { navController.navigate(Routes.ReturnStock) },
                    onStockAdjustmentClick = { navController.navigate(Routes.StockAdjustment) }
                )
            }
        }
        composable(Routes.ReceiveStock) {
            val loggedInState = sessionState as? SessionState.LoggedIn
            if (loggedInState == null) {
                SplashScreen()
            } else {
                ReceiveStockScreen(
                    viewModel = viewModel(
                        factory = ReceiveStockViewModel.Factory(loggedInState.session.accessToken)
                    ),
                    onStockJourneyClick = { stockJourneyId ->
                        navController.navigate(Routes.receiveStockDetail(stockJourneyId))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        composable(
            route = Routes.ReceiveStockDetail,
            arguments = listOf(
                navArgument("stockJourneyId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val loggedInState = sessionState as? SessionState.LoggedIn
            val stockJourneyId = backStackEntry.arguments?.getInt("stockJourneyId")
            if (loggedInState == null || stockJourneyId == null) {
                SplashScreen()
            } else {
                ReceiveStockDetailScreen(
                    viewModel = viewModel(
                        factory = ReceiveStockDetailViewModel.Factory(
                            accessToken = loggedInState.session.accessToken,
                            stockJourneyId = stockJourneyId
                        )
                    ),
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        composable(Routes.Sale) {
            SaleScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Routes.ReturnStock) {
            ReturnScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Routes.StockAdjustment) {
            StockAdjustmentScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
