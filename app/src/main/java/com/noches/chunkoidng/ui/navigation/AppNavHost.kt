package com.noches.chunkoidng.ui.navigation

import android.content.Intent
import android.net.Uri
import com.noches.chunkoidng.R
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.noches.chunkoidng.ui.screens.decryptor.NetEaseCryptScreen
import com.noches.chunkoidng.ui.screens.features.FeaturesScreen
import com.noches.chunkoidng.ui.screens.pruner.DimensionPrunerScreen
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
        contentWindowInsets = if (isTopLevelScreen) {
            ScaffoldDefaults.contentWindowInsets
        } else {
            WindowInsets(0, 0, 0, 0)
        },
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
                        when (feature.id) {
                            "world_converter" -> navController.navigate("world_converter")
                            "netease_decryptor" -> navController.navigate("netease_crypt")
                            "dimension_pruner" -> navController.navigate("dimension_pruner")
                            "nbt_editor" -> navController.navigate("nbt_editor")
                            "pack_converter" -> navController.navigate("pack_converter")
                            "midi_converter" -> navController.navigate("midi_converter")
                            "sandbox_terminal" -> navController.navigate("console")
                            else -> {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(
                                            R.string.feature_selected_toast,
                                            context.getString(feature.titleRes),
                                            context.getString(feature.badgeRes)
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
            }

            composable("world_converter") {
                com.noches.chunkoidng.ui.screens.converter.WorldConverterScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { navController.navigate("conversion_history") }
                )
            }

            composable("dimension_pruner") {
                DimensionPrunerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { navController.navigate("conversion_history") }
                )
            }

            composable("netease_crypt") {
                NetEaseCryptScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToConverter = {
                        navController.navigate("world_converter") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable("nbt_editor") {
                com.noches.chunkoidng.ui.screens.nbt.NbtEditorScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("pack_converter") {
                com.noches.chunkoidng.ui.screens.pack.PackConverterScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("midi_converter") {
                com.noches.chunkoidng.ui.screens.midi.MidiConverterScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("conversion_history") {
                com.noches.chunkoidng.ui.screens.history.ConversionHistoryScreen(
                    onNavigateBack = { navController.popBackStack() }
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
