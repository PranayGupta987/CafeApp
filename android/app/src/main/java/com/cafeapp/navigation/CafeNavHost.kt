package com.cafeapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cafeapp.navigation.placeholder.CafeHomePlaceholder
import com.cafeapp.navigation.placeholder.CustomerHomePlaceholder
import com.cafeapp.navigation.placeholder.DevelopmentFoundationScreen
import com.cafeapp.navigation.placeholder.KitchenHomePlaceholder
import com.cafeapp.navigation.placeholder.SplashScreen

@Composable
fun CafeNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = "splash",
    ) {
        composable("splash") {
            SplashScreen(
                onNavigateToCustomer = {
                    navController.navigate("customer/home") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToCafe = {
                    navController.navigate("cafe/home") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToKitchen = {
                    navController.navigate("kitchen/home") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("auth/login") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
            )
        }
        composable("dev/foundation") {
            DevelopmentFoundationScreen(
                onNavigateToAuth = {
                    navController.navigate("auth/login")
                },
            )
        }
        composable("auth/login") {
            com.cafeapp.feature.auth.ui.LoginScreen(
                onLoginSuccess = { role ->
                    val dest = when (role) {
                        "CAFE_ADMIN", "CAFE_STAFF" -> "cafe/home"
                        "KITCHEN" -> "kitchen/home"
                        else -> "customer/home"
                    }
                    navController.navigate(dest) {
                        popUpTo("auth/login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("auth/register")
                },
            )
        }
        composable("auth/register") {
            com.cafeapp.feature.auth.ui.RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("customer/home") {
                        popUpTo("auth/login") { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable("customer/home") {
            CustomerMainScreen(
                onLogout = {
                    navController.navigate("auth/login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable("cafe/home") {
            CafeHomePlaceholder(
                onLogout = {
                    navController.navigate("auth/login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable("kitchen/home") {
            KitchenHomePlaceholder(
                onLogout = {
                    navController.navigate("auth/login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
