package com.noches.chunkoidng.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.noches.chunkoidng.ui.components.ChunkoidNavigationBar
import com.noches.chunkoidng.ui.components.ChunkoidTopAppBar
import com.noches.chunkoidng.ui.screens.about.AboutScreen
import com.noches.chunkoidng.ui.screens.features.FeaturesScreen
import com.noches.chunkoidng.ui.screens.settings.SettingsScreen
import com.noches.chunkoidng.ui.screens.tutorial.TutorialScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Features.route
    val currentScreen = Screen.topLevelScreens.find { it.route == currentRoute } ?: Screen.Features

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val isTopLevelScreen = Screen.topLevelScreens.any { it.route == currentRoute }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(if (isTopLevelScreen) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier),
        topBar = {
            if (isTopLevelScreen) {
                ChunkoidTopAppBar(
                    currentScreen = currentScreen,
                    scrollBehavior = scrollBehavior,
                    onOpenWiki = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://chunkoid.top/docs/index.html"))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    onOpenTerminal = {
                        navController.navigate("console")
                    }
                )
            }
        },
        bottomBar = {
            if (isTopLevelScreen) {
                ChunkoidNavigationBar(
                    currentScreen = currentScreen,
                    onNavigateTo = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Features.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                androidx.compose.animation.scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200, easing = androidx.compose.animation.core.FastOutLinearInEasing)) +
                androidx.compose.animation.scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(200, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                )
            }
        ) {
            composable(Screen.Features.route) {
                FeaturesScreen(
                    onFeatureClick = { feature ->
                        if (feature.id == "sandbox_terminal") {
                            navController.navigate("console")
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "已选择功能: ${feature.title} [${feature.badge}]"
                                )
                            }
                        }
                    }
                )
            }

            composable("console") {
                com.noches.chunkoidng.ui.screens.console.ConsoleScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Tutorial.route) {
                TutorialScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(Screen.About.route) {
                AboutScreen()
            }
        }
    }
}
