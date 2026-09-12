package com.noches.chunkoidng

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.noches.chunkoidng.ui.navigation.MainAppScaffold
import com.noches.chunkoidng.ui.theme.ChunkoidNGTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChunkoidNGTheme {
                MainAppScaffold()
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