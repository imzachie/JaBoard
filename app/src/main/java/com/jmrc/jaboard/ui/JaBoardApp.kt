package com.jmrc.jaboard.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
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
    val view = LocalView.current
    val isLightSurface = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() > 0.5f
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightNavigationBars = isLightSurface
            controller.isAppearanceLightStatusBars = isLightSurface
        }
    }
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val mainRoute = destinations.any { it.route == route }
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        bottomBar = {
            if (mainRoute && !wide) NavigationBar(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                destinations.forEach { destination ->
                    val selected = route == destination.route
                    val scale by animateFloatAsState(if (selected) 1.12f else 1f, animationSpec = tween(220, easing = FastOutSlowInEasing), label = "navIconScale")
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectDestination(navController, destination.route) },
                        icon = {
                            Icon(destination.icon, contentDescription = destination.label, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
                        },
                        label = {
                            AnimatedVisibility(visible = selected, enter = fadeIn(tween(140)), exit = fadeOut(tween(90))) { Text(destination.label) }
                        },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer)
                    )
                }
            }
        }
    ) { innerPadding ->
        Row(Modifier.fillMaxSize().padding(innerPadding)) {
            if (mainRoute && wide) NavigationRail(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                destinations.forEach { destination ->
                    NavigationRailItem(
                        selected = route == destination.route,
                        onClick = { selectDestination(navController, destination.route) },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                        alwaysShowLabel = true
                    )
                }
            }
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize().weight(1f),
                enterTransition = { fadeIn(tween(240, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(240)) { it / 14 } },
                exitTransition = { fadeOut(tween(130)) + slideOutHorizontally(tween(130)) { -it / 24 } },
                popEnterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 16 } },
                popExitTransition = { fadeOut(tween(120)) + slideOutHorizontally(tween(120)) { it / 24 } }
            ) {
                composable("home") { HomeScreen(vm, onOpenUtility = { navController.navigate("utility/$it") }, onOpenStudio = { selectDestination(navController, "studio") }) }
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

private fun selectDestination(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
