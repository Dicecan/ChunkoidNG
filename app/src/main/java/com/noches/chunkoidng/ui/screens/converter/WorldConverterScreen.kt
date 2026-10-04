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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noches.chunkoidng.R
import com.noches.chunkoidng.core.conversion.ChunkerFormat
import com.noches.chunkoidng.core.conversion.PruningProfile
import com.noches.chunkoidng.core.world.WorldInfo
import com.noches.chunkoidng.ui.components.FormatPickerBottomSheet
import com.noches.chunkoidng.ui.components.SourcePickerCard
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

    LaunchedEffect(Unit) {
        if (uiState.stage == ConverterStage.SELECT_SOURCE) {
            viewModel.loadPreStagedWorld()
        }
    }

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
                Toast.makeText(context, context.getString(R.string.toast_export_success), Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showFormatPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.converter_screen_title), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Outlined.History, contentDescription = stringResource(R.string.nav_history))
                    }
                    if (uiState.stage == ConverterStage.CONFIGURE || uiState.stage == ConverterStage.COMPLETED || uiState.stage == ConverterStage.ERROR) {
                        IconButton(onClick = { viewModel.reset() }) {
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
                            errorMessage = uiState.errorMessage ?: stringResource(R.string.common_unknown),
                            onRetry = { viewModel.reset() }
                        )
                    }
                }
            }

            if (showFormatPicker) {
                FormatPickerBottomSheet(
                    sourceWorld = uiState.worldInfo,
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
            text = stringResource(R.string.converter_import_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.converter_import_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        SourcePickerCard(
            title = stringResource(R.string.source_picker_folder_title),
            subtitle = stringResource(R.string.source_picker_folder_desc),
            icon = Icons.Outlined.Folder,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            badge = stringResource(R.string.converter_badge_recommended),
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
                        Text(worldInfo?.name ?: stringResource(R.string.common_unknown), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                                val platformBadge = worldInfo?.platform?.let { stringResource(it.nameRes) } ?: ""
                                Text(platformBadge, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                                Text(worldInfo?.displayVersion ?: "", modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                }
            }
        }

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
                        val badgeText = if (targetFormat.id == "INPUT") {
                            stringResource(R.string.version_keep_input)
                        } else if (isSamePlatform) {
                            stringResource(R.string.converter_target_badge_same)
                        } else {
                            stringResource(R.string.converter_target_badge_cross)
                        }
                        Text("${stringResource(R.string.converter_target_card_title)} [$badgeText]", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                        Text(targetFormat.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }

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
                                Text(stringResource(R.string.converter_pruning_card_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.converter_pruning_card_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text(stringResource(R.string.converter_pruning_presets), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PruningProfile.values().forEach { profile ->
                                    val profileLabel = when (profile) {
                                        PruningProfile.OVERWORLD_ONLY -> stringResource(R.string.pruner_profile_overworld_title)
                                        PruningProfile.SPEED -> stringResource(R.string.pruner_profile_speed_title)
                                        PruningProfile.FULL -> stringResource(R.string.pruner_profile_full_title)
                                        PruningProfile.CUSTOM -> stringResource(R.string.pruner_profile_custom_title)
                                    }
                                    FilterChip(
                                        selected = pruningProfile == profile,
                                        onClick = { onSelectPruningProfile(profile) },
                                        label = { Text(profileLabel, fontSize = 12.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            DimensionSwitchItem(
                                title = stringResource(R.string.converter_dim_overworld),
                                subtitle = stringResource(R.string.converter_dim_overworld_desc),
                                checked = includeOverworld,
                                onCheckedChange = onToggleOverworld
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            DimensionSwitchItem(
                                title = stringResource(R.string.converter_dim_nether),
                                subtitle = stringResource(R.string.converter_dim_nether_desc),
                                checked = includeNether,
                                onCheckedChange = onToggleNether
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            DimensionSwitchItem(
                                title = stringResource(R.string.converter_dim_the_end),
                                subtitle = stringResource(R.string.converter_dim_the_end_desc),
                                checked = includeTheEnd,
                                onCheckedChange = onToggleTheEnd
                            )
                        }
                    }
                }
            }
        }

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
                                Text(stringResource(R.string.converter_world_settings_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.converter_world_settings_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                label = { Text(stringResource(R.string.converter_override_name_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(stringResource(R.string.converter_override_gamemode_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val modes = listOf(
                                    "DEFAULT" to stringResource(R.string.converter_gamemode_default),
                                    "SURVIVAL" to stringResource(R.string.converter_gamemode_survival),
                                    "CREATIVE" to stringResource(R.string.converter_gamemode_creative),
                                    "ADVENTURE" to stringResource(R.string.converter_gamemode_adventure),
                                    "SPECTATOR" to stringResource(R.string.converter_gamemode_spectator)
                                )
                                modes.forEach { (modeKey, modeTitle) ->
                                    FilterChip(
                                        selected = overrideGameMode == modeKey,
                                        onClick = { onUpdateGameMode(modeKey) },
                                        label = { Text(modeTitle, fontSize = 12.sp) }
                                    )
                                }
                            }

                            Text(stringResource(R.string.converter_override_difficulty_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val diffs = listOf(
                                    "DEFAULT" to stringResource(R.string.converter_gamemode_default),
                                    "PEACEFUL" to stringResource(R.string.converter_difficulty_peaceful),
                                    "EASY" to stringResource(R.string.converter_difficulty_easy),
                                    "NORMAL" to stringResource(R.string.converter_difficulty_normal),
                                    "HARD" to stringResource(R.string.converter_difficulty_hard)
                                )
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
                                    Text(stringResource(R.string.converter_keep_nbt), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(R.string.converter_keep_nbt_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = keepOriginalNbt, onCheckedChange = onToggleKeepOriginalNbt)
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
                shape = ExpressiveShapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.converter_action_start), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    Text(stageText.ifBlank { stringResource(R.string.converter_converting) }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$progress%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                        Toast.makeText(context, context.getString(R.string.toast_logs_copied), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.common_copy), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = ExpressiveShapes.medium
        ) {
            Text(stringResource(R.string.converter_action_abort))
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
        Text(stringResource(R.string.converter_success), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Text(stringResource(R.string.converter_converted_to, targetFormat.displayName), color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onExportDirectory,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = ExpressiveShapes.medium,
            enabled = !isExporting
        ) {
            Icon(Icons.Outlined.Folder, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.converter_export_dir), fontSize = 16.sp)
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
            Text(stringResource(R.string.converter_export_archive, if(targetFormat.platform.isBedrock) ".mcworld" else ".zip"), fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onNewConversion) {
            Text(stringResource(R.string.converter_convert_another))
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
        Text(stringResource(R.string.converter_failed), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(errorMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, shape = ExpressiveShapes.medium) {
            Text(stringResource(R.string.common_retry))
        }
    }
}
