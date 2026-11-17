package com.jmrc.jaboard.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jmrc.jaboard.data.JaBoardViewModel

private data class MainDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val destinations = listOf(
    MainDestination("home", "Home", Icons.Filled.Dashboard),
    MainDestination("explore", "Explore", Icons.Filled.Explore),
    MainDestination("studio", "Studio", Icons.Filled.Tune),
    MainDestination("settings", "Settings", Icons.Filled.Settings)
)

@Composable
fun JaBoardApp(vm: JaBoardViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val mainRoute = destinations.any { it.route == route }
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    Scaffold(
        bottomBar = {
            if (mainRoute && !wide) NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = route == destination.route,
                        onClick = { selectDestination(navController, destination.route) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            if (mainRoute && wide) NavigationRail {
                destinations.forEach { destination ->
                    NavigationRailItem(
                        selected = route == destination.route,
                        onClick = { selectDestination(navController, destination.route) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                        alwaysShowLabel = true
                    )
                }
            }
            NavHost(navController, startDestination = "home", modifier = androidx.compose.ui.Modifier.weight(1f).fillMaxSize()) {
            composable("home") { HomeScreen(vm, onOpenUtility = { navController.navigate("utility/$it") }, onOpenStudio = { navController.navigate("studio") }) }
            composable("explore") { ExploreScreen(onOpenUtility = { navController.navigate("utility/$it") }) }
            composable("studio") { StudioScreen(vm) }
            composable("settings") { SettingsScreen(vm, onOpenAbout = { navController.navigate("utility/about") }) }
            composable("utility/{name}", arguments = listOf(navArgument("name") { type = NavType.StringType })) { entry ->
                UtilityScreen(entry.arguments?.getString("name").orEmpty(), vm, onBack = { navController.popBackStack() })
            }
            }
        }
    }
}

private fun selectDestination(navController: androidx.navigation.NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
