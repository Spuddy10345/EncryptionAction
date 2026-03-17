package com.example.encryptaction.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.encryptaction.domain.model.UserRole
import com.example.encryptaction.ui.screen.admin.AdminScreen
import com.example.encryptaction.ui.screen.auth.LoginScreen
import com.example.encryptaction.ui.screen.auth.RegisterScreen
import com.example.encryptaction.ui.screen.encrypt.EncryptScreen
import com.example.encryptaction.ui.screen.files.FilesScreen
import com.example.encryptaction.ui.screen.keys.KeysScreen
import com.example.encryptaction.ui.screen.profile.ProfileScreen
import com.example.encryptaction.ui.screen.sign.SignScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    currentUserRole: UserRole?,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val onProfileClick = { navController.navigate(NavRoutes.Profile.route) }

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {

        // Auth
        composable(NavRoutes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.Files.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(NavRoutes.Register.route) }
            )
        }

        composable(NavRoutes.Register.route) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(NavRoutes.Files.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Main destinations
        composable(NavRoutes.Files.route) {
            FilesScreen(navController = navController, onProfileClick = onProfileClick)
        }

        composable(NavRoutes.Encrypt.route) {
            EncryptScreen(navController = navController, onProfileClick = onProfileClick)
        }

        composable(NavRoutes.Sign.route) {
            SignScreen(navController = navController, onProfileClick = onProfileClick)
        }

        composable(NavRoutes.Keys.route) {
            KeysScreen(navController = navController, onProfileClick = onProfileClick)
        }

        composable(NavRoutes.Admin.route) {
            if (currentUserRole == UserRole.ADMIN) {
                AdminScreen(navController = navController, onProfileClick = onProfileClick)
            } else {
                navController.navigate(NavRoutes.Files.route) {
                    popUpTo(NavRoutes.Admin.route) { inclusive = true }
                }
            }
        }

        // Profile
        composable(NavRoutes.Profile.route) {
            ProfileScreen(
                navController = navController,
                onLogoutSuccess = {
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
