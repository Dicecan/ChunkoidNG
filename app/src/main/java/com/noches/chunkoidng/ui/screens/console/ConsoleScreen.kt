package com.noches.chunkoidng.ui.screens.console

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noches.chunkoidng.R
import com.noches.chunkoidng.core.runtime.JavaProcessManager
import com.noches.chunkoidng.core.runtime.JavaRuntimeEnvironment
import com.noches.chunkoidng.core.runtime.TerminalEasterEggEngine
import androidx.annotation.StringRes
import com.noches.chunkoidng.ui.theme.WindowWidthTier
import com.noches.chunkoidng.ui.theme.rememberAdaptiveMetrics
import kotlinx.coroutines.launch

private const val MAX_CONSOLE_LOG_LINES = 2000

private data class QuickCommand(
    @StringRes val labelRes: Int,
    val command: String
)

private val quickCommands = listOf(
    QuickCommand(R.string.console_cmd_chunker_help, "java -jar cli.jar"),
    QuickCommand(R.string.console_cmd_java_version, "java -version"),
    QuickCommand(R.string.console_cmd_list_dir, "ls"),
    QuickCommand(R.string.console_cmd_current_path, "pwd"),
    QuickCommand(R.string.console_cmd_kernel_info, "uname -a"),
    QuickCommand(R.string.console_cmd_help, "help")
)

private fun trimLogs(logs: MutableList<String>) {
    if (logs.size > MAX_CONSOLE_LOG_LINES) {
        logs.subList(0, logs.size - MAX_CONSOLE_LOG_LINES).clear()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val metrics = rememberAdaptiveMetrics()

    val runtimeEnv = remember { JavaRuntimeEnvironment(context) }
    val processManager = remember { JavaProcessManager(context, runtimeEnv) }

    var command by remember { mutableStateOf("") }
    val logs = remember {
        mutableStateListOf(
            context.getString(R.string.console_log_ready),
            context.getString(R.string.console_log_copy_tip)
        )
    }
    var isExecuting by remember { mutableStateOf(false) }
    var isMatrixMode by remember { mutableStateOf(false) }
    var konamiUnlocked by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.scrollToItem(logs.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runtimeEnv.ensureCliJar()
        }
    }

    fun executeCommand(cmd: String) {
        if (cmd.isNotBlank() && !isExecuting) {
            val trimmedCmd = cmd.trim()
            command = ""

            val easterResponse = TerminalEasterEggEngine.evaluate(trimmedCmd)
            if (easterResponse != null) {
                if (easterResponse.clearLogs) {
                    logs.clear()
                } else {
                    logs.add("$ $trimmedCmd")
                }
                if (easterResponse.toggleMatrixMode == true) {
                    isMatrixMode = !isMatrixMode
                }
                if (easterResponse.unlockKonami) {
                    konamiUnlocked = true
                }
                logs.addAll(easterResponse.lines)
                trimLogs(logs)
                return
            }

            logs.add("$ $trimmedCmd")
            isExecuting = true
            coroutineScope.launch {
                val batch = mutableListOf<String>()
                var lastFlush = System.currentTimeMillis()
                processManager.executeShellCommand(trimmedCmd).collect { outputLine ->
                    batch.add(outputLine)
                    val now = System.currentTimeMillis()
                    if (now - lastFlush >= 50 || batch.size >= 15) {
                        logs.addAll(batch)
                        trimLogs(logs)
                        batch.clear()
                        lastFlush = now
                    }
                }
                if (batch.isNotEmpty()) {
                    logs.addAll(batch)
                    trimLogs(logs)
                    batch.clear()
                }
                isExecuting = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.console_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "OpenJDK 17 • aarch64 • Chunker CLI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val fullLog = logs.joinToString("\n")
                            clipboardManager.setText(AnnotatedString(fullLog))
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.toast_logs_copied))
                            }
                        }
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.common_copy),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            logs.clear()
                            logs.add(context.getString(R.string.console_log_cleared))
                        }
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.common_clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(horizontal = if (metrics.widthTier == WindowWidthTier.COMPACT_NARROW) 8.dp else 12.dp, vertical = 6.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isMatrixMode) Color(0xFF041409) else Color(0xFF0F1117))
                    .border(
                        1.dp,
                        if (isMatrixMode) Color(0xFF00FF66).copy(alpha = 0.45f) else Color(0xFF23283B),
                        RoundedCornerShape(18.dp)
                    )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isMatrixMode) Color(0xFF072210) else Color(0xFF161924))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))

                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "~/filesDir/rootfs",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (isMatrixMode) Color(0xFF50FA7B) else Color(0xFF8B949E),
                        fontWeight = FontWeight.Medium
                    )

                    if (konamiUnlocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF332A00))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+30 LIVES",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF1FA8C)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF212638))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(if (isExecuting) R.string.console_status_running else R.string.console_status_idle),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExecuting) Color(0xFFFFB86C) else Color(0xFF50FA7B)
                        )
                    }
                }

                SelectionContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(logs.size) { index ->
                            val line = logs[index]
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = if (metrics.widthTier == WindowWidthTier.COMPACT_NARROW) 11.5.sp else 12.5.sp,
                                    lineHeight = if (metrics.widthTier == WindowWidthTier.COMPACT_NARROW) 16.sp else 17.5.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                color = when {
                                    line.startsWith("[EASTER]") -> Color(0xFFF1FA8C)
                                    line.contains("[HEROBRINE]") || line.contains("[MAID™ BLOCKED]") -> Color(0xFFFF79C6)
                                    line.startsWith("[SYSTEM]") -> Color(0xFF56B6C2)
                                    line.startsWith("$") -> Color(0xFF61AFEF)
                                    line.contains("Missing required options", ignoreCase = true) ||
                                    line.contains("Error", ignoreCase = true) ||
                                    line.contains("failed", ignoreCase = true) ||
                                    line.contains("Permission denied", ignoreCase = true) -> Color(0xFFE06C75)
                                    line.trim().startsWith("-") || line.trim().startsWith("'--") -> Color(0xFFE5C07B)
                                    line.startsWith("Usage:") -> Color(0xFF98C379)
                                    isMatrixMode -> Color(0xFF50FA7B)
                                    else -> Color(0xFFABB2BF)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickCommands) { qc ->
                    SuggestionChip(
                        onClick = { command = qc.command },
                        label = {
                            Text(
                                stringResource(qc.labelRes),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.console_input_hint), fontSize = 13.sp) },
                    enabled = !isExecuting,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { executeCommand(command) }
                    ),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = { executeCommand(command) },
                    modifier = Modifier.size(50.dp),
                    containerColor = if (isExecuting) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.console_run_btn))
                    }
                }
            }
        }
    }
}
