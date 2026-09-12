package com.noches.chunkoidng.ui.screens.converter

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noches.chunkoidng.core.conversion.ChunkerFormat
import com.noches.chunkoidng.core.world.Platform
import com.noches.chunkoidng.core.world.WorldInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldConverterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: WorldConverterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // SAF Launchers
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
            viewModel.exportConvertedWorld(it, packAsArchiveExport) { _ ->
                Toast.makeText(context, "导出成功！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showFormatPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("存档转换", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Outlined.History, contentDescription = "转换记录")
                    }
                    if (uiState.stage != ConverterStage.SELECT_SOURCE && uiState.stage != ConverterStage.CONVERTING) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "重新选择")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.stage,
                label = "ConverterStages"
            ) { stage ->
                when (stage) {
                    ConverterStage.SELECT_SOURCE -> {
                        SelectSourceView(
                            onSelectFolder = { folderLauncher.launch(null) },
                            onSelectArchive = { archiveLauncher.launch(arrayOf("*/*")) }
                        )
                    }
                    ConverterStage.STAGING -> {
                        ProgressView(progress = uiState.stagingProgress, message = uiState.stagingMessage)
                    }
                    ConverterStage.CONFIGURE -> {
                        ConfigureView(
                            worldInfo = uiState.worldInfo,
                            targetFormat = uiState.targetFormat,
                            onOpenFormatPicker = { showFormatPicker = true },
                            onStartConversion = { viewModel.startConversion() },
                            includeNether = uiState.includeNether,
                            onToggleNether = { viewModel.toggleDimension("NETHER", it) },
                            includeTheEnd = uiState.includeTheEnd,
                            onToggleTheEnd = { viewModel.toggleDimension("THE_END", it) },
                            overrideWorldName = uiState.overrideWorldName,
                            onUpdateWorldName = viewModel::setOverrideWorldName
                        )
                    }
                    ConverterStage.CONVERTING -> {
                        ConvertingView(
                            progress = uiState.conversionProgress,
                            stageText = uiState.conversionStageText,
                            logs = uiState.conversionLogs,
                            onCancel = { viewModel.cancelConversion() }
                        )
                    }
                    ConverterStage.COMPLETED -> {
                        CompletedView(
                            targetFormat = uiState.targetFormat,
                            isExporting = uiState.isExporting,
                            onExportDirectory = { 
                                packAsArchiveExport = false
                                exportLauncher.launch(null) 
                            },
                            onExportArchive = { 
                                packAsArchiveExport = true
                                exportLauncher.launch(null) 
                            },
                            onNewConversion = { viewModel.reset() }
                        )
                    }
                    ConverterStage.ERROR -> {
                        ErrorView(
                            errorMessage = uiState.errorMessage ?: "未知错误",
                            onRetry = { viewModel.reset() }
                        )
                    }
                }
            }
        }
    }

    if (showFormatPicker) {
        FormatPickerBottomSheet(
            currentFormat = uiState.targetFormat,
            onSelect = {
                viewModel.selectTargetFormat(it)
                showFormatPicker = false
            },
            onDismiss = { showFormatPicker = false }
        )
    }
}

@Composable
private fun SelectSourceView(
    onSelectFolder: () -> Unit,
    onSelectArchive: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "导入源存档",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "支持从目录或 .mcworld/.zip 压缩包导入",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        Card(
            onClick = onSelectFolder,
            modifier = Modifier.fillMaxWidth().height(100.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Folder, 
                    contentDescription = null, 
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("选择文件夹", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Android/data/.../minecraftWorlds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.7f))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            onClick = onSelectArchive,
            modifier = Modifier.fillMaxWidth().height(100.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.FolderZip, 
                    contentDescription = null, 
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("选择压缩包", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(".zip 或 .mcworld", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.7f))
                }
            }
        }
    }
}

@Composable
private fun ProgressView(progress: Int, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(progress = { progress / 100f }, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("$progress%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfigureView(
    worldInfo: WorldInfo?,
    targetFormat: ChunkerFormat,
    onOpenFormatPicker: () -> Unit,
    onStartConversion: () -> Unit,
    includeNether: Boolean,
    onToggleNether: (Boolean) -> Unit,
    includeTheEnd: Boolean,
    onToggleTheEnd: (Boolean) -> Unit,
    overrideWorldName: String,
    onUpdateWorldName: (String) -> Unit
) {
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // World Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (worldInfo?.iconBitmap != null) {
                        Image(
                            bitmap = worldInfo.iconBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Public, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(worldInfo?.name ?: "Unknown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                                Text(worldInfo?.platform?.displayName ?: "", modifier = Modifier.padding(horizontal=4.dp))
                            }
                            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                                Text(worldInfo?.displayVersion ?: "", modifier = Modifier.padding(horizontal=4.dp))
                            }
                        }
                    }
                }
            }
        }

        // Target Format
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenFormatPicker,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("转换至", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha=0.7f))
                        Text(targetFormat.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    Icon(Icons.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }

        // Advanced Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isAdvancedExpanded = !isAdvancedExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("高级选项 (维度、名称)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Icon(if (isAdvancedExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                    }

                    AnimatedVisibility(visible = isAdvancedExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            OutlinedTextField(
                                value = overrideWorldName,
                                onValueChange = onUpdateWorldName,
                                label = { Text("重命名导出存档 (可选)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("保留下界 (Nether)")
                                Switch(checked = includeNether, onCheckedChange = onToggleNether)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("保留末地 (The End)")
                                Switch(checked = includeTheEnd, onCheckedChange = onToggleTheEnd)
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onStartConversion,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("开始转换", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ConvertingView(progress: Int, stageText: String, logs: List<String>, onCancel: () -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stageText.ifBlank { "正在转换..." }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$progress%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(12.dp)) {
                items(logs) { line ->
                    Text(line, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = androidx.compose.ui.graphics.Color(0xFFD4D4D4))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
            Text("中止转换")
        }
    }
}

@Composable
private fun CompletedView(targetFormat: ChunkerFormat, isExporting: Boolean, onExportDirectory: () -> Unit, onExportArchive: () -> Unit, onNewConversion: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("转换成功！", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("已转换为 ${targetFormat.displayName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(48.dp))

        FilledTonalButton(
            onClick = onExportDirectory,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isExporting
        ) {
            Icon(Icons.Outlined.Folder, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("选择文件夹并导出 (解包)", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onExportArchive,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isExporting
        ) {
            Icon(Icons.Outlined.FolderZip, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("选择文件夹并导出 (打包 ${if(targetFormat.platform.isBedrock) ".mcworld" else ".zip"})", fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onNewConversion) {
            Text("返回主页")
        }
    }
}

@Composable
private fun ErrorView(errorMessage: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("发生错误", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(errorMessage, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormatPickerBottomSheet(
    currentFormat: ChunkerFormat,
    onSelect: (ChunkerFormat) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var expandedGroup by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, start = 16.dp, end = 16.dp)) {
            Text("选择目标平台与版本", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
            
            // Bedrock Card
            Card(
                modifier = Modifier.fillMaxWidth().clickable { expandedGroup = if (expandedGroup == "BEDROCK") null else "BEDROCK" },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Smartphone, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("基岩版 (Bedrock)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("支持 1.12 ~ 1.21+", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (expandedGroup == "BEDROCK") Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                    }

                    AnimatedVisibility(visible = expandedGroup == "BEDROCK") {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            ChunkerFormat.BEDROCK_FORMATS.groupBy { it.group }.forEach { (groupName, formats) ->
                                Text(groupName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    formats.forEach { fmt ->
                                        val isSelected = fmt.id == currentFormat.id
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                            modifier = Modifier.clickable { onSelect(fmt) }
                                        ) {
                                            Text(
                                                fmt.displayName.replace("Bedrock ", ""),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (fmt.isPopular) FontWeight.Bold else FontWeight.Normal),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Java Card
            Card(
                modifier = Modifier.fillMaxWidth().clickable { expandedGroup = if (expandedGroup == "JAVA") null else "JAVA" },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Java 版", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("支持 1.8.8 ~ 1.21+", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (expandedGroup == "JAVA") Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                    }

                    AnimatedVisibility(visible = expandedGroup == "JAVA") {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            ChunkerFormat.JAVA_FORMATS.groupBy { it.group }.forEach { (groupName, formats) ->
                                Text(groupName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    formats.forEach { fmt ->
                                        val isSelected = fmt.id == currentFormat.id
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                            modifier = Modifier.clickable { onSelect(fmt) }
                                        ) {
                                            Text(
                                                fmt.displayName.replace("Java ", ""),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (fmt.isPopular) FontWeight.Bold else FontWeight.Normal),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

