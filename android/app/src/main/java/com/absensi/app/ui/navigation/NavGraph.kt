package com.absensi.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.absensi.app.ui.PlaceholderScreen

sealed class Screen(val route: String) {
    data object Placeholder : Screen("placeholder")
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Placeholder.route
    ) {
        composable(Screen.Placeholder.route) {
            PlaceholderScreen()
        }
    }
}
