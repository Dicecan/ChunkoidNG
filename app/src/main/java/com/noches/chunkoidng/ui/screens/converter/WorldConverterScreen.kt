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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.noches.chunkoidng.core.conversion.PruningProfile
import com.noches.chunkoidng.core.world.Platform
import com.noches.chunkoidng.core.world.WorldInfo
import com.noches.chunkoidng.ui.theme.ExpressiveShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldConverterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: WorldConverterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Auto load if pre-staged from Decryptor
    LaunchedEffect(Unit) {
        if (uiState.stage == ConverterStage.SELECT_SOURCE) {
            viewModel.loadPreStagedWorld()
        }
    }

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
                    Text("世界存档转换", fontWeight = FontWeight.Bold)
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
                    if (uiState.stage == ConverterStage.CONFIGURE || uiState.stage == ConverterStage.COMPLETED || uiState.stage == ConverterStage.ERROR) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "重新选择")
                        }
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                targetState = uiState.stage,
                label = "ConverterStageAnimation"
            ) { stage ->
                when (stage) {
                    ConverterStage.SELECT_SOURCE -> {
                        SelectSourceView(
                            onSelectFolder = { folderLauncher.launch(null) },
                            onSelectArchive = { archiveLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }
                        )
                    }
                    ConverterStage.STAGING -> {
                        ProgressView(
                            progress = uiState.stagingProgress,
                            message = uiState.stagingMessage
                        )
                    }
                    ConverterStage.CONFIGURE -> {
                        ConfigureView(
                            worldInfo = uiState.worldInfo,
                            targetFormat = uiState.targetFormat,
                            onOpenFormatPicker = { showFormatPicker = true },
                            onStartConversion = { viewModel.startConversion() },
                            pruningProfile = uiState.pruningProfile,
                            onSelectPruningProfile = { viewModel.setPruningProfile(it) },
                            includeOverworld = uiState.includeOverworld,
                            onToggleOverworld = { viewModel.toggleDimension("OVERWORLD", it) },
                            includeNether = uiState.includeNether,
                            onToggleNether = { viewModel.toggleDimension("NETHER", it) },
                            includeTheEnd = uiState.includeTheEnd,
                            onToggleTheEnd = { viewModel.toggleDimension("THE_END", it) },
                            keepOriginalNbt = uiState.keepOriginalNbt,
                            onToggleKeepOriginalNbt = { viewModel.setKeepOriginalNbt(it) },
                            overrideWorldName = uiState.overrideWorldName,
                            onUpdateWorldName = { viewModel.setOverrideWorldName(it) },
                            overrideGameMode = uiState.overrideGameMode,
                            onUpdateGameMode = { viewModel.setOverrideGameMode(it) },
                            overrideDifficulty = uiState.overrideDifficulty,
                            onUpdateDifficulty = { viewModel.setOverrideDifficulty(it) }
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
                            errorMessage = uiState.errorMessage ?: "发生未知错误",
                            onRetry = { viewModel.reset() }
                        )
                    }
                }
            }

            if (showFormatPicker) {
                FormatPickerDialog(
                    sourcePlatform = uiState.worldInfo?.platform ?: Platform.BEDROCK,
                    currentFormat = uiState.targetFormat,
                    onSelectFormat = {
                        viewModel.selectTargetFormat(it)
                        showFormatPicker = false
                    },
                    onDismiss = { showFormatPicker = false }
                )
            }
        }
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "导入源世界存档",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "支持从系统目录或 .mcworld / .zip 压缩包直接导入",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        Card(
            onClick = onSelectFolder,
            modifier = Modifier.fillMaxWidth().height(100.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = ExpressiveShapes.large
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Folder, 
                    contentDescription = null, 
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("选择文件夹", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    Text("Minecraft 存档根目录 (含 level.dat)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            onClick = onSelectArchive,
            modifier = Modifier.fillMaxWidth().height(100.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = ExpressiveShapes.large
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.FolderZip, 
                    contentDescription = null, 
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("选择压缩包", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    Text(".zip 或 .mcworld 格式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    pruningProfile: PruningProfile,
    onSelectPruningProfile: (PruningProfile) -> Unit,
    includeOverworld: Boolean,
    onToggleOverworld: (Boolean) -> Unit,
    includeNether: Boolean,
    onToggleNether: (Boolean) -> Unit,
    includeTheEnd: Boolean,
    onToggleTheEnd: (Boolean) -> Unit,
    keepOriginalNbt: Boolean,
    onToggleKeepOriginalNbt: (Boolean) -> Unit,
    overrideWorldName: String,
    onUpdateWorldName: (String) -> Unit,
    overrideGameMode: String,
    onUpdateGameMode: (String) -> Unit,
    overrideDifficulty: String,
    onUpdateDifficulty: (String) -> Unit
) {
    var isPruningExpanded by remember { mutableStateOf(true) }
    var isWorldSettingsExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // World Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = ExpressiveShapes.large
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
                        Text(worldInfo?.name ?: "Unknown World", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                                Text(worldInfo?.platform?.displayName ?: "", modifier = Modifier.padding(horizontal = 4.dp))
                            }
                            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                                Text(worldInfo?.displayVersion ?: "", modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        // Target Format & Version Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenFormatPicker,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = ExpressiveShapes.large
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val isSamePlatform = worldInfo != null && worldInfo.platform == targetFormat.platform
                        val badgeText = if (targetFormat.id == "INPUT") "保持原版本" else if (isSamePlatform) "版本升降级" else "双端跨格式转换"
                        Text("目标版本 [$badgeText]", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                        Text(targetFormat.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }

        // Dimension Pruning Card (维度与区块裁剪)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ExpressiveShapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isPruningExpanded = !isPruningExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("维度与区块裁剪策略", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("智能剔除无效维度，缩减体积防闪退", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(
                            imageVector = if (isPruningExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(
                        visible = isPruningExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text("快速裁剪预设", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PruningProfile.values().forEach { profile ->
                                    FilterChip(
                                        selected = pruningProfile == profile,
                                        onClick = { onSelectPruningProfile(profile) },
                                        label = { Text(profile.displayName, fontSize = 12.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Dimension Switches
                            DimensionSwitchItem(
                                title = "主世界 (Overworld)",
                                subtitle = "游戏主维度与建筑核心",
                                checked = includeOverworld,
                                onCheckedChange = onToggleOverworld
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            DimensionSwitchItem(
                                title = "下界 / 地狱 (The Nether)",
                                subtitle = "地狱维度，关闭可显著降低文件大小",
                                checked = includeNether,
                                onCheckedChange = onToggleNether
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            DimensionSwitchItem(
                                title = "末路之地 (The End)",
                                subtitle = "末地与末地城，关闭可大幅加速转换",
                                checked = includeTheEnd,
                                onCheckedChange = onToggleTheEnd
                            )
                        }
                    }
                }
            }
        }

        // World Settings Overrides Card (世界规则与元数据)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ExpressiveShapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isWorldSettingsExpanded = !isWorldSettingsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("世界参数与规则覆盖", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("世界名称、默认游戏模式与难度", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(
                            imageVector = if (isWorldSettingsExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(
                        visible = isWorldSettingsExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = overrideWorldName,
                                onValueChange = onUpdateWorldName,
                                label = { Text("目标世界名称") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Game Mode Choice
                            Text("默认游戏模式", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val modes = listOf("DEFAULT" to "保持原样", "SURVIVAL" to "生存", "CREATIVE" to "创造", "ADVENTURE" to "冒险", "SPECTATOR" to "旁观")
                                modes.forEach { (modeKey, modeTitle) ->
                                    FilterChip(
                                        selected = overrideGameMode == modeKey,
                                        onClick = { onUpdateGameMode(modeKey) },
                                        label = { Text(modeTitle, fontSize = 12.sp) }
                                    )
                                }
                            }

                            // Difficulty Choice
                            Text("默认游戏难度", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val diffs = listOf("DEFAULT" to "保持原样", "PEACEFUL" to "和平", "EASY" to "简单", "NORMAL" to "普通", "HARD" to "困难")
                                diffs.forEach { (diffKey, diffTitle) ->
                                    FilterChip(
                                        selected = overrideDifficulty == diffKey,
                                        onClick = { onUpdateDifficulty(diffKey) },
                                        label = { Text(diffTitle, fontSize = 12.sp) }
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("保留未修改的原始 NBT (-k)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("在同格式裁剪或版本升降级时尽可能继承标签", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = keepOriginalNbt, onCheckedChange = onToggleKeepOriginalNbt)
                            }
                        }
                    }
                }
            }
        }

        // Convert Button
        item {
            Button(
                onClick = onStartConversion,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = ExpressiveShapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始转换", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DimensionSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ConvertingView(
    progress: Int,
    stageText: String,
    logs: List<String>,
    onCancel: () -> Unit
) {
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.scrollToItem(logs.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                    Text(stageText.ifBlank { "正在转换..." }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$progress%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logs Terminal
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = ExpressiveShapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(12.dp)
                    ) {
                        items(logs) { line ->
                            Text(
                                text = line,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        val text = logs.joinToString("\n")
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "复制日志", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = ExpressiveShapes.medium
        ) {
            Text("中止转换")
        }
    }
}

@Composable
private fun CompletedView(
    targetFormat: ChunkerFormat,
    isExporting: Boolean,
    onExportDirectory: () -> Unit,
    onExportArchive: () -> Unit,
    onNewConversion: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("转换成功！", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Text("已转换为 ${targetFormat.displayName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onExportDirectory,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = ExpressiveShapes.medium,
            enabled = !isExporting
        ) {
            Icon(Icons.Outlined.Folder, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("导出到文件夹 (解包)", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onExportArchive,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = ExpressiveShapes.medium,
            enabled = !isExporting
        ) {
            Icon(Icons.Outlined.FolderZip, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("导出为压缩包 (${if(targetFormat.platform.isBedrock) ".mcworld" else ".zip"})", fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onNewConversion) {
            Text("转换其他世界")
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
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("转换失败", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(errorMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, shape = ExpressiveShapes.medium) {
            Text("重试")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatPickerDialog(
    sourcePlatform: Platform,
    currentFormat: ChunkerFormat,
    onSelectFormat: (ChunkerFormat) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(if (sourcePlatform == Platform.BEDROCK) 1 else 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("选择目标版本与格式", fontWeight = FontWeight.Bold)
                Text("支持跨版本升级、降级或双端格式互转", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("基岩版 (BE)", fontSize = 12.sp)
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Java 版 (JE)", fontSize = 12.sp)
                    }
                    SegmentedButton(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("保持原版本", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val formats = when (selectedTab) {
                    0 -> ChunkerFormat.BEDROCK_FORMATS
                    1 -> ChunkerFormat.JAVA_FORMATS
                    else -> listOf(ChunkerFormat.FORMAT_INPUT)
                }

                LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                    items(formats) { format ->
                        val isSelected = format.id == currentFormat.id
                        ListItem(
                            headlineContent = {
                                Text(
                                    format.displayName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            supportingContent = {
                                Text(
                                    if (format.id == "INPUT") "不转换格式，仅应用维度裁剪与规则覆盖" else format.group,
                                    fontSize = 11.sp
                                )
                            },
                            trailingContent = {
                                if (isSelected) {
                                    Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.clickable { onSelectFormat(format) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
