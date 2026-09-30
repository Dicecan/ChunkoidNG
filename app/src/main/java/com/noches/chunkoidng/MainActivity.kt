package com.noches.chunkoidng

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
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

            CompositionLocalProvider(LocalContext provides localizedContext) {
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