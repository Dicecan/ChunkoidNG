package com.noches.chunkoidng.ui.screens.pruner

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noches.chunkoidng.core.conversion.PruningProfile
import com.noches.chunkoidng.core.world.WorldInfo
import com.noches.chunkoidng.ui.components.SourcePickerCard
import com.noches.chunkoidng.ui.theme.ChipBadgeShape
import com.noches.chunkoidng.ui.theme.ExpressiveShapes
import com.noches.chunkoidng.ui.theme.SquircleIconShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DimensionPrunerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: DimensionPrunerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) viewModel.handleFolderSelected(uri)
    }

    val archiveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.handleArchiveSelected(uri)
    }

    var packAsArchiveExport by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportPrunedWorld(uri, packAsArchiveExport) { success ->
                if (success) {
                    Toast.makeText(context, "导出瘦身存档成功！", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "导出失败，请检查写入权限", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("存档瘦身与维度裁剪", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Outlined.History, contentDescription = "历史记录")
                    }
                    if (uiState.stage == PrunerStage.CONFIGURE || uiState.stage == PrunerStage.COMPLETED || uiState.stage == PrunerStage.ERROR) {
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
                label = "PrunerStageAnimation"
            ) { stage ->
                when (stage) {
                    PrunerStage.SELECT_SOURCE -> {
                        SelectSourceView(
                            onSelectFolder = { folderLauncher.launch(null) },
                            onSelectArchive = { archiveLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }
                        )
                    }
                    PrunerStage.STAGING -> {
                        ProgressView(
                            progress = uiState.stagingProgress,
                            message = uiState.stagingMessage
                        )
                    }
                    PrunerStage.CONFIGURE -> {
                        ConfigurePruningView(
                            worldInfo = uiState.worldInfo,
                            pruningProfile = uiState.pruningProfile,
                            onSelectProfile = { viewModel.selectPruningProfile(it) },
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
                            onStartPruning = { viewModel.startPruning() }
                        )
                    }
                    PrunerStage.PRUNING -> {
                        PruningRunningView(
                            progress = uiState.pruningProgress,
                            stageText = uiState.pruningStageText,
                            logs = uiState.pruningLogs,
                            onCancel = { viewModel.cancelPruning() }
                        )
                    }
                    PrunerStage.COMPLETED -> {
                        PruningCompletedView(
                            originalSize = uiState.originalSizeBytes,
                            prunedSize = uiState.prunedSizeBytes,
                            isExporting = uiState.isExporting,
                            onExportDirectory = {
                                packAsArchiveExport = false
                                exportLauncher.launch(null)
                            },
                            onExportArchive = {
                                packAsArchiveExport = true
                                exportLauncher.launch(null)
                            },
                            onNewPruning = { viewModel.reset() }
                        )
                    }
                    PrunerStage.ERROR -> {
                        PruningErrorView(
                            errorMessage = uiState.errorMessage ?: "发生未知错误",
                            onRetry = { viewModel.reset() }
                        )
                    }
                }
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
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "导入待瘦身世界存档",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "智能剔除未修改、无效的区块与维度，大幅降低地图体积",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Unified MD3E SourcePickerCard
        SourcePickerCard(
            title = "选择存档文件夹",
            subtitle = "Minecraft 存档根目录 (含 level.dat 与 db/ 或 region/)",
            icon = Icons.Outlined.Folder,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            badge = "推荐",
            onClick = onSelectFolder
        )

        Spacer(modifier = Modifier.height(16.dp))

        SourcePickerCard(
            title = "选择压缩包文件",
            subtitle = "支持 .zip 或 .mcworld 格式",
            icon = Icons.Outlined.FolderZip,
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onSelectArchive
        )
    }
}

@Composable
private fun ProgressView(progress: Int, message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = ExpressiveShapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.size(56.dp),
                    strokeWidth = 5.dp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ConfigurePruningView(
    worldInfo: WorldInfo?,
    pruningProfile: PruningProfile,
    onSelectProfile: (PruningProfile) -> Unit,
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
    onStartPruning: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // World Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ExpressiveShapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (worldInfo?.iconBitmap != null) {
                        Image(
                            bitmap = worldInfo.iconBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp).clip(ExpressiveShapes.medium)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(ExpressiveShapes.medium)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Terrain,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = worldInfo?.name ?: "Minecraft 世界",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${worldInfo?.platform?.displayName ?: "Bedrock"} ${worldInfo?.versionName ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val sizeMb = (worldInfo?.sizeBytes ?: 0L) / (1024.0 * 1024.0)
                        Text(
                            text = "当前地图体积: %.1f MB".format(sizeMb),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Slimming Presets (瘦身策略预设)
        item {
            Text(
                text = "选择瘦身策略",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PruningPresetCard(
                    title = "仅保留主世界",
                    subtitle = "剔除下界与末地所有冗余区块，通常可减小 40%~70% 体积",
                    badge = "推荐",
                    isSelected = pruningProfile == PruningProfile.OVERWORLD_ONLY,
                    onClick = { onSelectProfile(PruningProfile.OVERWORLD_ONLY) }
                )

                PruningPresetCard(
                    title = "极速轻量化 (Speed)",
                    subtitle = "仅保留主世界核心区域并保护原生 NBT，适合低配联机",
                    badge = "极速",
                    isSelected = pruningProfile == PruningProfile.SPEED,
                    onClick = { onSelectProfile(PruningProfile.SPEED) }
                )

                PruningPresetCard(
                    title = "全维度保留与整理",
                    subtitle = "保留主世界、下界和末地所有维度，仅对损坏与空置区块进行精简",
                    badge = "无损",
                    isSelected = pruningProfile == PruningProfile.FULL,
                    onClick = { onSelectProfile(PruningProfile.FULL) }
                )

                PruningPresetCard(
                    title = "自定义维度裁剪",
                    subtitle = "自由勾选需要保留或剔除的维度",
                    badge = "高级",
                    isSelected = pruningProfile == PruningProfile.CUSTOM,
                    onClick = { onSelectProfile(PruningProfile.CUSTOM) }
                )
            }
        }

        // Custom dimension checkboxes
        if (pruningProfile == PruningProfile.CUSTOM) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveShapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("自定义保留维度", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        DimensionCheckboxRow(
                            label = "主世界 (Overworld)",
                            checked = includeOverworld,
                            onCheckedChange = onToggleOverworld,
                            icon = Icons.Outlined.Terrain
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DimensionCheckboxRow(
                            label = "下界 (Nether)",
                            checked = includeNether,
                            onCheckedChange = onToggleNether,
                            icon = Icons.Outlined.Whatshot
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DimensionCheckboxRow(
                            label = "末地 (The End)",
                            checked = includeTheEnd,
                            onCheckedChange = onToggleTheEnd,
                            icon = Icons.Outlined.Brightness3
                        )
                    }
                }
            }
        }

        // Advanced Options Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ExpressiveShapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("高级选项", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("保留原始 NBT (-k)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("无损保留所有方块实体与物品附魔属性", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = keepOriginalNbt, onCheckedChange = onToggleKeepOriginalNbt)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = overrideWorldName,
                        onValueChange = onUpdateWorldName,
                        label = { Text("瘦身后世界名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = ExpressiveShapes.medium
                    )
                }
            }
        }

        // Start Pruning Button
        item {
            Button(
                onClick = onStartPruning,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = ExpressiveShapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.CleaningServices, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("开始深度瘦身", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PruningPresetCard(
    title: String,
    subtitle: String,
    badge: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = ExpressiveShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.8.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(ChipBadgeShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            RadioButton(selected = isSelected, onClick = onClick)
        }
    }
}

@Composable
private fun DimensionCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PruningRunningView(
    progress: Int,
    stageText: String,
    logs: List<String>,
    onCancel: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ExpressiveShapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("瘦身执行中...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$progress%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stageText.ifBlank { "正在整理区块索引..." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = ExpressiveShapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Outlined.Close, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("取消瘦身")
        }
    }
}

@Composable
private fun PruningCompletedView(
    originalSize: Long,
    prunedSize: Long,
    isExporting: Boolean,
    onExportDirectory: () -> Unit,
    onExportArchive: () -> Unit,
    onNewPruning: () -> Unit
) {
    val origMb = originalSize / (1024.0 * 1024.0)
    val prunedMb = prunedSize / (1024.0 * 1024.0)
    val savedMb = (originalSize - prunedSize) / (1024.0 * 1024.0)
    val savedPercent = if (originalSize > 0) ((originalSize - prunedSize) * 100.0 / originalSize) else 0.0

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "地图瘦身完成！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "已安全移除指定维度与冗余区块，有效缩减地图存储占用",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Space Saved Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ExpressiveShapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("原始大小", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("%.1f MB".format(origMb), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("瘦身后", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("%.1f MB".format(prunedMb), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("节省空间", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("-%.1f%%".format(savedPercent.coerceAtLeast(0.0)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Export Options
        Button(
            onClick = onExportDirectory,
            enabled = !isExporting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = ExpressiveShapes.medium
        ) {
            Icon(Icons.Outlined.Folder, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isExporting) "正在导出..." else "导出为文件夹")
        }

        Spacer(modifier = Modifier.height(12.dp))

        FilledTonalButton(
            onClick = onExportArchive,
            enabled = !isExporting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = ExpressiveShapes.medium
        ) {
            Icon(Icons.Outlined.FolderZip, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isExporting) "正在导出..." else "导出为压缩包 (.mcworld)")
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onNewPruning) {
            Text("处理另一个世界")
        }
    }
}

@Composable
private fun PruningErrorView(
    errorMessage: String,
    onRetry: () -> Unit
) {
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
            text = "瘦身过程出现异常",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, shape = ExpressiveShapes.medium) {
            Text("重试")
        }
    }
}
