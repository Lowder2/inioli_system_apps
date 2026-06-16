package com.lowderancorp.inioli.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.lowderancorp.inioli.data.auth.UserSession
import com.lowderancorp.inioli.ui.screens.HomeScreen
import com.lowderancorp.inioli.ui.screens.LoginScreen
import com.lowderancorp.inioli.ui.screens.ReceiveStockDetailScreen
import com.lowderancorp.inioli.ui.screens.ReceiveStockScreen
import com.lowderancorp.inioli.ui.screens.ReturnScreen
import com.lowderancorp.inioli.ui.screens.SaleScreen
import com.lowderancorp.inioli.ui.screens.SplashScreen
import com.lowderancorp.inioli.ui.screens.StockAdjustmentScreen

private object Routes {
    const val ReceiveStockRefreshResult = "receive_stock_refresh"
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
private fun LoggedInContent(
    sessionState: SessionState,
    content: @Composable (UserSession) -> Unit
) {
    val loggedInState = sessionState as? SessionState.LoggedIn
    if (loggedInState == null) {
        SplashScreen()
    } else {
        content(loggedInState.session)
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
            LoggedInContent(sessionState = sessionState) { session ->
                HomeScreen(
                    username = session.username,
                    onLogoutClick = appViewModel::logout,
                    onReceiveStockClick = { navController.navigate(Routes.ReceiveStock) },
                    onSaleClick = { navController.navigate(Routes.Sale) },
                    onReturnClick = { navController.navigate(Routes.ReturnStock) },
                    onStockAdjustmentClick = { navController.navigate(Routes.StockAdjustment) }
                )
            }
        }
        composable(Routes.ReceiveStock) {
            LoggedInContent(sessionState = sessionState) {
                val receiveStockViewModel: ReceiveStockViewModel = viewModel(
                    factory = ReceiveStockViewModel.Factory
                )
                val receiveStockBackStackEntry = remember(navController) {
                    navController.getBackStackEntry(Routes.ReceiveStock)
                }
                val refreshRequested by receiveStockBackStackEntry.savedStateHandle
                    .getStateFlow(Routes.ReceiveStockRefreshResult, false)
                    .collectAsStateWithLifecycle()

                LaunchedEffect(refreshRequested) {
                    if (refreshRequested) {
                        receiveStockBackStackEntry.savedStateHandle[Routes.ReceiveStockRefreshResult] = false
                        receiveStockViewModel.refresh()
                    }
                }

                ReceiveStockScreen(
                    viewModel = receiveStockViewModel,
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
            val stockJourneyId = backStackEntry.arguments?.getInt("stockJourneyId")
            if (stockJourneyId == null) {
                SplashScreen()
            } else {
                LoggedInContent(sessionState = sessionState) {
                    ReceiveStockDetailScreen(
                        viewModel = viewModel(
                            factory = ReceiveStockDetailViewModel.Factory(
                                stockJourneyId = stockJourneyId
                            )
                        ),
                        onBackClick = { navController.popBackStack() },
                        onCloseSuccess = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Routes.ReceiveStockRefreshResult, true)
                            navController.popBackStack()
                        }
                    )
                }
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
