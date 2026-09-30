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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import kotlinx.coroutines.launch

private const val MAX_CONSOLE_LOG_LINES = 2000

private val quickCommands = listOf(
    "Chunker 帮助" to "java -jar cli.jar",
    "Java 版本" to "java -version",
    "查看目录" to "ls",
    "当前路径" to "pwd",
    "内核信息" to "uname -a"
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

    val runtimeEnv = remember { JavaRuntimeEnvironment(context) }
    val processManager = remember { JavaProcessManager(context, runtimeEnv) }

    var command by remember { mutableStateOf("") }
    val logs = remember {
        mutableStateListOf(
            "[SYSTEM] OpenJDK 17.0.18 沙箱终端已就绪",
            "[SYSTEM] 支持长按文本选词复制，或点右上角一键复制完整输出"
        )
    }
    var isExecuting by remember { mutableStateOf(false) }

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
            logs.add("$ $cmd")
            command = ""
            isExecuting = true
            coroutineScope.launch {
                val batch = mutableListOf<String>()
                var lastFlush = System.currentTimeMillis()
                processManager.executeShellCommand(cmd).collect { outputLine ->
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
                            logs.add("[SYSTEM] Console cleared")
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
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F1117))
                    .border(1.dp, Color(0xFF23283B), RoundedCornerShape(18.dp))
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161924))
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
                        color = Color(0xFF8B949E),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF212638))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isExecuting) "RUNNING" else "IDLE",
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
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.5.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                color = when {
                                    line.startsWith("[SYSTEM]") -> Color(0xFF56B6C2)
                                    line.startsWith("$") -> Color(0xFF61AFEF)
                                    line.contains("Missing required options", ignoreCase = true) ||
                                    line.contains("Error", ignoreCase = true) ||
                                    line.contains("failed", ignoreCase = true) ||
                                    line.contains("Permission denied", ignoreCase = true) -> Color(0xFFE06C75)
                                    line.trim().startsWith("-") || line.trim().startsWith("'--") -> Color(0xFFE5C07B)
                                    line.startsWith("Usage:") -> Color(0xFF98C379)
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
                items(quickCommands) { (label, cmd) ->
                    SuggestionChip(
                        onClick = { command = cmd },
                        label = {
                            Text(
                                label,
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
