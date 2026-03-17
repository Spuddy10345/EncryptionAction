package com.example.encryptaction.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.encryptaction.domain.model.UserRole

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val requiredRole: UserRole? = null
)

val bottomNavItems = listOf(
    BottomNavItem("Files", Icons.Default.Folder, NavRoutes.Files.route),
    BottomNavItem("Encrypt", Icons.Default.Lock, NavRoutes.Encrypt.route),
    BottomNavItem("Sign", Icons.Default.VerifiedUser, NavRoutes.Sign.route),
    BottomNavItem("Keys", Icons.Default.Key, NavRoutes.Keys.route),
    BottomNavItem("Admin", Icons.Default.AdminPanelSettings, NavRoutes.Admin.route, UserRole.ADMIN)
)
