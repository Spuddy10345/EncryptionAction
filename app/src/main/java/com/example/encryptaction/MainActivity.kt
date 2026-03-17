package com.example.encryptaction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.encryptaction.ui.navigation.AppNavGraph
import com.example.encryptaction.ui.navigation.NavRoutes
import com.example.encryptaction.ui.navigation.bottomNavItems
import com.example.encryptaction.ui.theme.EncryptActionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EncryptActionTheme {
                val mainViewModel: MainViewModel = hiltViewModel()
                val session by mainViewModel.session.collectAsState()
                val navController = rememberNavController()
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStack?.destination?.route

                val startDestination = if (session != null) NavRoutes.Files.route
                else NavRoutes.Login.route

                val bottomNavRoutes = bottomNavItems.map { it.route }
                val showBottomBar = session != null && currentRoute in bottomNavRoutes

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                val visibleItems = bottomNavItems.filter { item ->
                                    item.requiredRole == null || item.requiredRole == session?.role
                                }
                                visibleItems.forEach { item ->
                                    NavigationBarItem(
                                        selected = currentRoute == item.route,
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                navController.navigate(item.route) {
                                                    popUpTo(NavRoutes.Files.route) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = { Text(item.label) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        currentUserRole = session?.role,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
