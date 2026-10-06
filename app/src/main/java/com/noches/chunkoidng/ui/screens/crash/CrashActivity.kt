package com.noches.chunkoidng.ui.screens.crash

import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noches.chunkoidng.MainActivity
import com.noches.chunkoidng.R
import com.noches.chunkoidng.core.crash.CrashLogActions
import com.noches.chunkoidng.core.crash.CrashLogManager
import com.noches.chunkoidng.core.settings.AppPreferences
import com.noches.chunkoidng.core.settings.LocaleHelper
import com.noches.chunkoidng.ui.components.AppLogo
import com.noches.chunkoidng.ui.theme.ChunkoidNGTheme
import com.noches.chunkoidng.ui.theme.ExpressiveShapes
import java.io.File
import kotlin.system.exitProcess

class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filePath = intent.getStringExtra(EXTRA_CRASH_FILE)
        val summary = intent.getStringExtra(EXTRA_CRASH_SUMMARY) ?: "Unknown exception"
        val crashFile = filePath?.let { File(it) }?.takeIf { it.exists() }
        val fullReportText = crashFile?.let(CrashLogManager::readCrashLog)?.ifEmpty { summary } ?: summary
        val preferences = AppPreferences(this)
        val localizedContext = LocaleHelper.applyLocale(this, preferences.appLanguage)

        setContent {
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration
            ) {
                ChunkoidNGTheme(dynamicColor = preferences.dynamicColorEnabled) {
                    CrashScreen(
                        summary = summary,
                        fullReportText = fullReportText,
                        onCopyLog = {
                            CrashLogActions.copyLog(localizedContext, fullReportText)
                        },
                        onShareLog = {
                            CrashLogActions.shareLog(localizedContext, crashFile, fullReportText)
                        },
                        onRestartApp = {
                            val restartIntent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(restartIntent)
                            finish()
                            Process.killProcess(Process.myPid())
                            exitProcess(0)
                        },
                        onSafeModeRestart = {
                            try {
                                val workspaceDir = File(filesDir, "workspace")
                                if (workspaceDir.exists()) {
                                    workspaceDir.deleteRecursively()
                                }
                                cacheDir.listFiles()?.forEach { file ->
                                    if (file.name.startsWith("nbt_") || file.name.startsWith("midi_") || file.name.endsWith(".tmp")) {
                                        file.deleteRecursively()
                                    }
                                }
                                getSharedPreferences("chunkoid_nbt_prefs", MODE_PRIVATE).edit().clear().apply()
                                getSharedPreferences("chunkoid_prefs", MODE_PRIVATE).edit().apply {
                                    remove("last_open_file")
                                    remove("last_selected_source")
                                    apply()
                                }
                            } catch (_: Exception) {}

                            val restartIntent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                putExtra("safe_mode", true)
                            }
                            startActivity(restartIntent)
                            finish()
                            Process.killProcess(Process.myPid())
                            exitProcess(0)
                        },
                        onExitApp = {
                            finishAffinity()
                            Process.killProcess(Process.myPid())
                            exitProcess(0)
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_CRASH_FILE = "extra_crash_file"
        const val EXTRA_CRASH_SUMMARY = "extra_crash_summary"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrashScreen(
    summary: String,
    fullReportText: String,
    onCopyLog: () -> Unit,
    onShareLog: () -> Unit,
    onRestartApp: () -> Unit,
    onSafeModeRestart: () -> Unit,
    onExitApp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLogo(
                            size = 32.dp,
                            iconSize = 20.dp,
                            shape = ExpressiveShapes.small
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.crash_screen_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveShapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.BugReport,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.crash_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.crash_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveShapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.crash_summary_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveShapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.crash_stacktrace_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val horizontalScrollState = rememberScrollState()
                        SelectionContainer {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(horizontalScrollState)
                            ) {
                                Text(
                                    text = fullReportText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onCopyLog,
                            modifier = Modifier.weight(1f),
                            shape = ExpressiveShapes.small
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.crash_action_copy))
                        }

                        FilledTonalButton(
                            onClick = onShareLog,
                            modifier = Modifier.weight(1f),
                            shape = ExpressiveShapes.small
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.crash_action_share))
                        }
                    }

                    FilledTonalButton(
                        onClick = onSafeModeRestart,
                        modifier = Modifier.fillMaxWidth(),
                        shape = ExpressiveShapes.small,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.crash_action_safe_restart))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onRestartApp,
                            modifier = Modifier.weight(1f),
                            shape = ExpressiveShapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.crash_action_restart))
                        }

                        OutlinedButton(
                            onClick = onExitApp,
                            modifier = Modifier.weight(1f),
                            shape = ExpressiveShapes.small
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.crash_action_exit))
                        }
                    }
                }
            }
        }
    }
}
