package com.noches.chunkoidng.ui.screens.decryptor

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noches.chunkoidng.R
import com.noches.chunkoidng.core.decryptor.CryptMode
import com.noches.chunkoidng.ui.components.SourcePickerCard
import com.noches.chunkoidng.ui.theme.ExpressiveShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetEaseCryptScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConverter: () -> Unit,
    viewModel: NetEaseCryptViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { e.printStackTrace() }
            viewModel.handleFolderSelected(it)
        }
    }

    val archiveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { e.printStackTrace() }
            viewModel.handleArchiveSelected(it)
        }
    }

    var packAsArchiveExport by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            } catch (e: Exception) { e.printStackTrace() }
            viewModel.exportResultWorld(it, packAsArchiveExport) { _ ->
                Toast.makeText(context, context.getString(R.string.toast_export_success), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.mode == CryptMode.DECRYPT) stringResource(R.string.decryptor_mode_decrypt) else stringResource(R.string.decryptor_mode_encrypt),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (uiState.stage == CryptUiStage.COMPLETED || uiState.stage == CryptUiStage.ERROR) {
                        IconButton(onClick = { viewModel.resetState() }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.source_picker_reselect))
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                targetState = uiState.stage,
                label = "CryptStageAnimation"
            ) { stage ->
                when (stage) {
                    CryptUiStage.SELECT_SOURCE -> {
                        SelectSourceView(
                            currentMode = uiState.mode,
                            onSelectMode = { viewModel.setMode(it) },
                            customKey = uiState.customKey,
                            onUpdateCustomKey = { viewModel.setCustomKey(it) },
                            onSelectFolder = { folderLauncher.launch(null) },
                            onSelectArchive = { archiveLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }
                        )
                    }
                    CryptUiStage.STAGING -> {
                        ProgressView(
                            progress = uiState.stagingProgress,
                            title = stringResource(R.string.common_loading),
                            message = uiState.stagingMessage
                        )
                    }
                    CryptUiStage.PROCESSING -> {
                        ProcessingView(
                            mode = uiState.mode,
                            progress = uiState.processProgress,
                            statusText = uiState.statusText,
                            currentFile = uiState.currentFileText,
                            logs = uiState.logs
                        )
                    }
                    CryptUiStage.COMPLETED -> {
                        CompletedView(
                            uiState = uiState,
                            onExportDirectory = {
                                packAsArchiveExport = false
                                exportLauncher.launch(null)
                            },
                            onExportArchive = {
                                packAsArchiveExport = true
                                exportLauncher.launch(null)
                            },
                            onGoToConverter = {
                                viewModel.prepareHandoffToConverter {
                                    onNavigateToConverter()
                                }
                            },
                            onNewTask = { viewModel.resetState() }
                        )
                    }
                    CryptUiStage.ERROR -> {
                        ErrorView(
                            errorMessage = uiState.errorMessage ?: stringResource(R.string.common_unknown),
                            onRetry = { viewModel.resetState() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectSourceView(
    currentMode: CryptMode,
    onSelectMode: (CryptMode) -> Unit,
    customKey: String,
    onUpdateCustomKey: (String) -> Unit,
    onSelectFolder: () -> Unit,
    onSelectArchive: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = currentMode == CryptMode.DECRYPT,
                onClick = { onSelectMode(CryptMode.DECRYPT) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {
                    Icon(
                        Icons.Outlined.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            ) {
                Text(stringResource(R.string.decryptor_mode_decrypt), fontWeight = FontWeight.Bold)
            }

            SegmentedButton(
                selected = currentMode == CryptMode.PASSIVE_ENCRYPT,
                onClick = { onSelectMode(CryptMode.PASSIVE_ENCRYPT) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            ) {
                Text(stringResource(R.string.decryptor_mode_encrypt), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ExpressiveShapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (currentMode == CryptMode.DECRYPT) Icons.Outlined.Info else Icons.Outlined.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.decryptor_screen_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.decryptor_screen_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SourcePickerCard(
            title = stringResource(R.string.source_picker_folder_title),
            subtitle = stringResource(R.string.source_picker_folder_desc),
            icon = Icons.Outlined.Folder,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onSelectFolder
        )

        Spacer(modifier = Modifier.height(16.dp))

        SourcePickerCard(
            title = stringResource(R.string.source_picker_zip_title),
            subtitle = stringResource(R.string.source_picker_zip_desc),
            icon = Icons.Outlined.FolderZip,
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onSelectArchive
        )
    }
}

@Composable
private fun ProgressView(progress: Int, title: String, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(progress = { progress / 100f }, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("$progress%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ProcessingView(
    mode: CryptMode,
    progress: Int,
    statusText: String,
    currentFile: String,
    logs: List<String>
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.scrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ExpressiveShapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText.ifBlank { stringResource(R.string.converter_converting) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                )

                if (currentFile.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentFile,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = ExpressiveShapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                ) {
                    items(logs) { line ->
                        val color = when {
                            line.contains("[SUCCESS]") -> Color(0xFF4CAF50)
                            line.contains("[VERIFY]") -> Color(0xFF2196F3)
                            line.contains("[ERROR]") -> MaterialTheme.colorScheme.error
                            line.contains("[INFO]") -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = color,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedView(
    uiState: CryptUiState,
    onExportDirectory: () -> Unit,
    onExportArchive: () -> Unit,
    onGoToConverter: () -> Unit,
    onNewTask: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.converter_success),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "${stringResource(R.string.converter_override_name_label)}: ${uiState.worldName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ExpressiveShapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.crypt_duration), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text("${uiState.durationMs / 1000.0} s", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("LevelDB", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text("${uiState.filesProcessed}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                if (uiState.keyHex.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Key (Hex)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text("0x${uiState.keyHex}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                if (uiState.mode == CryptMode.DECRYPT && uiState.ldbVerified) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("LevelDB Verified", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text("0x57FB...", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.mode == CryptMode.DECRYPT) {
            Button(
                onClick = onGoToConverter,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = ExpressiveShapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.SwapHoriz, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.decryptor_action_to_converter), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick = onExportArchive,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = ExpressiveShapes.medium,
            enabled = !uiState.isExporting
        ) {
            Icon(Icons.Outlined.FolderZip, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.converter_export_archive, ".mcworld"))
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onExportDirectory,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = ExpressiveShapes.medium,
            enabled = !uiState.isExporting
        ) {
            Icon(Icons.Outlined.Folder, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.converter_export_dir))
        }
    }
}

@Composable
private fun ErrorView(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.converter_failed),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = ExpressiveShapes.medium
        ) {
            Text(stringResource(R.string.common_retry))
        }
    }
}
