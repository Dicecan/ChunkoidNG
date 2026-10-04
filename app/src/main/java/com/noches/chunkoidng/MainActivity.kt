package com.noches.chunkoidng

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noches.chunkoidng.core.permission.PermissionHelper
import com.noches.chunkoidng.core.settings.AppPreferences
import com.noches.chunkoidng.core.settings.LocaleHelper
import com.noches.chunkoidng.ui.navigation.MainAppScaffold
import com.noches.chunkoidng.ui.screens.settings.SettingsViewModel
import com.noches.chunkoidng.ui.theme.ChunkoidNGTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val uiState by settingsViewModel.uiState.collectAsState()
            val baseContext = LocalContext.current
            val localizedContext = LocaleHelper.applyLocale(baseContext, uiState.appLanguage)
            val activity = this@MainActivity

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val preferences = AppPreferences(baseContext)
                    if (!preferences.notificationPermissionRequested &&
                        !PermissionHelper.hasNotificationRuntimePermission(baseContext)
                    ) {
                        preferences.notificationPermissionRequested = true
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides activity,
                LocalConfiguration provides localizedContext.resources.configuration
            ) {
                ChunkoidNGTheme(dynamicColor = uiState.dynamicColorEnabled) {
                    MainAppScaffold()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    ChunkoidNGTheme {
        MainAppScaffold()
    }
}
