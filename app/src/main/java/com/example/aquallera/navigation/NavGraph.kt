package com.example.aquallera.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.aquallera.components.BottomNavBar
import com.example.aquallera.auth.ui.AuthState
import com.example.aquallera.auth.ui.AuthViewModel
import com.example.aquallera.auth.ui.LandingScreen
import com.example.aquallera.auth.ui.LoginScreen
import com.example.aquallera.auth.ui.SignupScreen
import com.example.aquallera.auth.ui.SplashScreen
import com.example.aquallera.auth.ui.VerifyEmailScreen
import com.example.aquallera.core.theme.MidnightBlue
import com.example.aquallera.createorder.ui.CreateOrderScreen
import com.example.aquallera.createorder.ui.OrderConfirmationScreen
import com.example.aquallera.createorder.ui.OrderSuccessScreen
import com.example.aquallera.maps.ui.MapScreen
import com.example.aquallera.store.ui.StoreDetailsScreen
import com.example.aquallera.ui.about.AboutScreen
import com.example.aquallera.ui.orders.OrdersScreen
import com.example.aquallera.ui.profile.EditProfileScreen
import com.example.aquallera.ui.profile.ProfileScreen

object Routes {
    const val SPLASH = "splash"
    const val LANDING = "landing"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val VERIFY_EMAIL = "verify_email"
    const val MAP = "map"
    const val STORE_DETAILS = "store/{stationId}"
    const val CREATE_ORDER = "create_order/{stationId}"
    const val ORDER_CONFIRMATION = "order_confirmation"
    const val ORDER_SUCCESS = "order_success/{orderId}"
    const val ORDERS = "orders"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val ABOUT = "about"

    fun storeDetails(stationId: String) = "store/$stationId"
    fun createOrder(stationId: String) = "create_order/$stationId"
    fun orderSuccess(orderId: String) = "order_success/$orderId"
}

@Composable
fun AqualleraNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val authState by authViewModel.authState.collectAsState()
    val verifyState by authViewModel.verifyState.collectAsState()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            val isLoggedIn = authState is AuthState.Authenticated
            SplashScreen(
                isLoggedIn = isLoggedIn,
                onNavigateToLanding = {
                    navController.navigate(Routes.LANDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToMap = {
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.LANDING) {
            LandingScreen(
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) },
                onNavigateToSignup = { navController.navigate(Routes.SIGNUP) },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToSignup = { navController.navigate(Routes.SIGNUP) },
                onNavigateBack = { navController.popBackStack() },
            )
            LaunchedEffect(authState) {
                if (authState is AuthState.Authenticated) {
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            }
        }

        composable(Routes.SIGNUP) {
            SignupScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) },
                onNavigateToVerifyEmail = {
                    navController.navigate(Routes.VERIFY_EMAIL) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                signupSuccess = authState is AuthState.Authenticated && verifyState.email != null,
            )
        }

        composable(Routes.VERIFY_EMAIL) {
            VerifyEmailScreen(
                viewModel = authViewModel,
                onVerified = {
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.MAP) {
            MainScreen(navController = navController) {
                MapScreen(
                    onStationTap = { stationId ->
                        navController.navigate(Routes.storeDetails(stationId))
                    },
                    onOrderNow = { stationId ->
                        navController.navigate(Routes.createOrder(stationId))
                    },
                )
            }
        }
        composable(
            route = Routes.STORE_DETAILS,
            arguments = listOf(navArgument("stationId") { type = NavType.StringType }),
        ) {
            StoreDetailsScreen(
                onBack = { navController.popBackStack() },
                onOrderNow = { stationId ->
                    navController.navigate(Routes.createOrder(stationId))
                },
            )
        }
        composable(
            route = Routes.CREATE_ORDER,
            arguments = listOf(navArgument("stationId") { type = NavType.StringType }),
        ) {
            CreateOrderScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.ORDER_CONFIRMATION) },
            )
        }
        composable(Routes.ORDER_CONFIRMATION) {
            OrderConfirmationScreen(
                onBack = { navController.popBackStack() },
                onOrderConfirmed = { orderId ->
                    navController.navigate(Routes.orderSuccess(orderId)) {
                        popUpTo(Routes.MAP)
                    }
                },
            )
        }
        composable(
            route = Routes.ORDER_SUCCESS,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderSuccessScreen(
                orderId = orderId,
                onViewOrders = {
                    navController.navigate(Routes.ORDERS) {
                        popUpTo(Routes.MAP) { inclusive = true }
                    }
                },
                onBackToMap = {
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.MAP) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.ORDERS) {
            MainScreen(navController = navController) {
                OrdersScreen()
            }
        }
        composable(Routes.PROFILE) {
            MainScreen(navController = navController) {
                ProfileScreen(navController = navController)
            }
        }
        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(Routes.ABOUT) {
            MainScreen(navController = navController) {
                AboutScreen(navController = navController)
            }
        }
    }
}

@Composable
private fun MainScreen(
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onTabSelected = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.MAP) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

@Composable
fun PlaceholderContent(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            color = MidnightBlue,
        )
    }
}
