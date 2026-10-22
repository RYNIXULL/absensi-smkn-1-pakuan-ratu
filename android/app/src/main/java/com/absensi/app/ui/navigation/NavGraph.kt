package com.absensi.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.absensi.app.ui.auth.DevicePendingScreen
import com.absensi.app.ui.auth.LoginScreen
import com.absensi.app.ui.auth.LoginViewModel

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object DevicePending : Screen("device_pending")
    data object SiswaHome : Screen("siswa_home")
    data object GuruHome : Screen("guru_home")
    data object OperatorHome : Screen("operator_home")
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { role ->
                    val destination = when (role.lowercase()) {
                        "siswa" -> Screen.SiswaHome.route
                        "guru" -> Screen.GuruHome.route
                        "operator" -> Screen.OperatorHome.route
                        else -> Screen.Login.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onDevicePending = {
                    navController.navigate(Screen.DevicePending.route)
                }
            )
        }

        composable(Screen.DevicePending.route) {
            DevicePendingScreen(
                onBackToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.DevicePending.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SiswaHome.route) {
            SiswaHomeScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.GuruHome.route) {
            GuruHomeScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.OperatorHome.route) {
            OperatorHomeScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
