package com.noches.chunkoidng.ui.screens.nbt

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.gamemods.nbtmanipulator.*
import com.noches.chunkoidng.R
import com.noches.chunkoidng.core.leveldb.LevelDbCategory
import com.noches.chunkoidng.core.leveldb.LevelDbRecord
import java.io.File
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NbtEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: NbtEditorViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.openFileFromUri(it) }
    }

    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = try {
                val path = uri.path
                if (path != null && File(path).exists()) File(path) else null
            } catch (_: Exception) { null }
            if (file != null) {
                viewModel.openFile(file)
            } else {
                viewModel.openFileFromUri(uri)
            }
        }
    }

    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        uri?.let { viewModel.saveAs(it) }
    }

    BackHandler {
        if (!viewModel.handleBack()) {
            onNavigateBack()
        }
    }

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (state.mode == NbtEditorMode.EMPTY) {
                                stringResource(R.string.nbt_editor_title)
                            } else {
                                state.title
                            },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        state.subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!viewModel.handleBack()) {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    when (state.mode) {
                        NbtEditorMode.NBT_TREE -> {
                            IconButton(onClick = { viewModel.saveCurrent() }) {
                                Icon(Icons.Default.Save, contentDescription = stringResource(R.string.nbt_save_file))
                            }
                            IconButton(onClick = {
                                val rootNode = state.treeNodes.firstOrNull { it.depth == 0 }
                                if (rootNode != null) viewModel.requestAddTag(rootNode)
                            }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.nbt_add_tag))
                            }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.nbt_save_as)) },
                                    leadingIcon = { Icon(Icons.Default.SaveAs, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        saveAsLauncher.launch(state.currentFileName.ifEmpty { "level.dat" })
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.nbt_expand_all)) },
                                    leadingIcon = { Icon(Icons.Default.UnfoldMore, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.expandAll()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.nbt_collapse_all)) },
                                    leadingIcon = { Icon(Icons.Default.UnfoldLess, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.collapseAll()
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(stringResource(R.string.nbt_auto_save_title), modifier = Modifier.weight(1f))
                                            Switch(checked = state.autoSave, onCheckedChange = null)
                                        }
                                    },
                                    onClick = {
                                        viewModel.setAutoSave(!state.autoSave)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.nbt_close_workspace)) },
                                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.closeWorkspace()
                                    }
                                )
                            }
                        }
                        NbtEditorMode.WORKSPACE -> {
                            IconButton(onClick = { viewModel.closeWorkspace() }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.nbt_close_workspace))
                            }
                        }
                        NbtEditorMode.CHUNK_GRID -> {
                            IconButton(onClick = { viewModel.toggleChunkBoxSelectMode() }) {
                                Icon(
                                    if (state.chunkBoxSelectMode) Icons.Default.SelectAll else Icons.Default.TouchApp,
                                    contentDescription = null
                                )
                            }
                        }
                        NbtEditorMode.EMPTY -> {}
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state.mode) {
                NbtEditorMode.EMPTY -> {
                    NbtEmptyStateView(
                        onOpenFile = { openFileLauncher.launch(arrayOf("*/*")) },
                        onOpenArchive = { openFileLauncher.launch(arrayOf("*/*")) },
                        onOpenFolder = { openFolderLauncher.launch(null) }
                    )
                }
                NbtEditorMode.WORKSPACE -> {
                    NbtWorkspaceView(
                        state = state,
                        onSelectCategory = { viewModel.setLevelDbCategory(it) },
                        onSearchChange = { viewModel.setLevelDbSearch(it) },
                        onRecordClick = { viewModel.loadLevelDbRecord(it) },
                        onRecordDelete = { viewModel.deleteLevelDbSingleKey(it) },
                        onFileClick = { viewModel.switchToFileInWorkspace(it) },
                        onAddKeyClick = { viewModel.requestAddLevelDbKey() },
                        onOpen2DMap = { viewModel.openLevelDb2DVisualizer() },
                        onOpenPruneDialog = { viewModel.requestLevelDbPruneDialog() }
                    )
                }
                NbtEditorMode.NBT_TREE -> {
                    NbtTreeView(
                        state = state,
                        onSearchChange = { viewModel.setTreeSearchQuery(it) },
                        onToggleExpand = { viewModel.toggleNodeExpanded(it) },
                        onExpandAll = { viewModel.expandAll() },
                        onCollapseAll = { viewModel.collapseAll() },
                        onEditValue = { viewModel.requestEditValue(it) },
                        onAddTag = { viewModel.requestAddTag(it) },
                        onRenameKey = { viewModel.requestRenameTag(it) },
                        onDeleteTag = { viewModel.requestDeleteTag(it) },
                        onCopyPath = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("NBT Path", it.path))
                        },
                        onCopyValue = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("NBT Value", it.displayValue))
                        }
                    )
                }
                NbtEditorMode.CHUNK_GRID -> {
                    NbtChunkGridView(
                        state = state,
                        onSelectChunk = { x, z -> viewModel.selectChunk(x, z) },
                        onToggleSelection = { x, z -> viewModel.toggleChunkInSelection(x, z) },
                        onSelectAll = { viewModel.selectAllPopulatedChunks() },
                        onInvert = { viewModel.invertChunkSelection() },
                        onClear = { viewModel.clearChunkSelection() },
                        onEditChunkNbt = { x, z -> viewModel.loadChunkFromRegion(x, z) },
                        onDeleteChunk = { viewModel.deleteSelectedChunks() },
                        onPruneUseless = { viewModel.requestPruneUselessDialog() },
                        onSelectDimension = { viewModel.loadLevelDbDimension(it) }
                    )
                }
            }

            if (state.isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(state.loadingMessage, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    state.showEditValueDialog?.let { node ->
        NbtEditValueDialog(
            node = node,
            onDismiss = { viewModel.dismissEditValueDialog() },
            onConfirm = { valStr -> viewModel.applyNodeValueUpdate(node, valStr) }
        )
    }

    state.showAddTagDialog?.let { parentNode ->
        NbtAddTagDialog(
            parentNode = parentNode,
            onDismiss = { viewModel.dismissAddTagDialog() },
            onConfirm = { key, type, initialVal -> viewModel.addTagToNode(parentNode, key, type, initialVal) }
        )
    }

    state.showRenameTagDialog?.let { node ->
        NbtRenameTagDialog(
            node = node,
            onDismiss = { viewModel.dismissRenameTagDialog() },
            onConfirm = { newKey -> viewModel.renameTagKey(node, newKey) }
        )
    }

    state.showDeleteTagDialog?.let { node ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteTagDialog() },
            title = { Text(stringResource(R.string.nbt_delete_tag)) },
            text = { Text(stringResource(R.string.nbt_delete_confirm, node.key)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteTag(node) }) {
                    Text(stringResource(R.string.common_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteTagDialog() }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (state.showAddLevelDbKeyDialog) {
        NbtAddLevelDbKeyDialog(
            onDismiss = { viewModel.dismissAddLevelDbKeyDialog() },
            onConfirm = { key, value -> viewModel.addLevelDbKey(key, value) }
        )
    }

    state.showBinaryPreviewDialog?.let { record ->
        NbtBinaryPreviewDialog(
            record = record,
            onDismiss = { viewModel.closeBinaryPreview() },
            onDelete = {
                viewModel.closeBinaryPreview()
                viewModel.deleteLevelDbSingleKey(record)
            }
        )
    }

    if (state.showLevelDbPruneDialog) {
        NbtLevelDbPruneDialog(
            recordCount = state.levelDbRecords.size,
            onDismiss = { viewModel.dismissLevelDbPruneDialog() },
            onStrategySelected = { strategy ->
                if (strategy == 0) {
                    viewModel.dismissLevelDbPruneDialog()
                    viewModel.openLevelDb2DVisualizer()
                } else {
                    viewModel.executeLevelDbPrune(strategy)
                }
            }
        )
    }

    if (state.showPruneUselessDialog) {
        NbtPruneUselessDialog(
            onDismiss = { viewModel.dismissPruneUselessDialog() },
            onOptionSelected = { ticks -> viewModel.executePruneUselessChunks(ticks) }
        )
    }

    if (state.showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUnsavedChangesDialog() },
            title = { Text(stringResource(R.string.nbt_unsaved_title)) },
            text = { Text(stringResource(R.string.nbt_unsaved_message, state.currentFileName)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveCurrent(silent = true)
                    viewModel.dismissUnsavedChangesDialog()
                    onNavigateBack()
                }) {
                    Text(stringResource(R.string.nbt_save_and_exit))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissUnsavedChangesDialog()
                    onNavigateBack()
                }) {
                    Text(stringResource(R.string.nbt_discard_and_exit))
                }
            }
        )
    }
}

@Composable
private fun NbtEmptyStateView(
    onOpenFile: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenFolder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Dataset,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.nbt_empty_state_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.nbt_empty_state_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenFile() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.InsertDriveFile,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(stringResource(R.string.nbt_open_file), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.nbt_open_file_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenArchive() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.FolderZip,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(stringResource(R.string.nbt_open_archive), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.nbt_open_archive_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenFolder() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(stringResource(R.string.nbt_open_folder), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.nbt_open_folder_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun NbtWorkspaceView(
    state: NbtEditorUiState,
    onSelectCategory: (LevelDbCategory) -> Unit,
    onSearchChange: (String) -> Unit,
    onRecordClick: (LevelDbRecord) -> Unit,
    onRecordDelete: (LevelDbRecord) -> Unit,
    onFileClick: (String) -> Unit,
    onAddKeyClick: () -> Unit,
    onOpen2DMap: () -> Unit,
    onOpenPruneDialog: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(if (state.hasLevelDb) 0 else 1) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = state.activeWorkspaceName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (state.hasLevelDb) {
                        stringResource(R.string.nbt_leveldb_records_count, state.levelDbRecords.size)
                    } else {
                        "${state.workspaceZipEntries.size} files"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                if (state.hasLevelDb) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onOpen2DMap,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.nbt_leveldb_prune_opt_2d), fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onOpenPruneDialog,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.nbt_prune_useless_chunks), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (state.hasLevelDb && state.workspaceZipEntries.isNotEmpty()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.nbt_tab_leveldb)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.nbt_tab_files)) }
                )
            }
        }

        if (state.hasLevelDb && selectedTab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LevelDbCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = state.selectedCategory == cat,
                        onClick = { onSelectCategory(cat) },
                        label = { Text(stringResource(cat.titleRes)) }
                    )
                }
            }

            OutlinedTextField(
                value = state.levelDbSearchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.nbt_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredLevelDbRecords, key = { it.key.contentHashCode() }) { record ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRecordClick(record) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = record.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (record.isNbt) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "NBT",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Key: ${if (record.keyString.isNotEmpty()) record.keyString else "0x" + record.keyToHex()} | ${record.valueSize} B",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = onAddKeyClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.nbt_leveldb_add_key))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.workspaceZipEntries.keys.toList().sorted()) { entryName ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFileClick(entryName) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (entryName.endsWith(".mca", true)) Icons.Outlined.GridView else Icons.Outlined.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entryName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (entryName.endsWith(".mca", true)) "Anvil MCA Region" else "NBT Document",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NbtTreeView(
    state: NbtEditorUiState,
    onSearchChange: (String) -> Unit,
    onToggleExpand: (String) -> Unit,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    onEditValue: (NbtTreeNode) -> Unit,
    onAddTag: (NbtTreeNode) -> Unit,
    onRenameKey: (NbtTreeNode) -> Unit,
    onDeleteTag: (NbtTreeNode) -> Unit,
    onCopyPath: (NbtTreeNode) -> Unit,
    onCopyValue: (NbtTreeNode) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.treeSearchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.nbt_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (state.treeSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = onExpandAll,
                label = { Text(stringResource(R.string.nbt_expand_all)) },
                leadingIcon = { Icon(Icons.Default.UnfoldMore, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            AssistChip(
                onClick = onCollapseAll,
                label = { Text(stringResource(R.string.nbt_collapse_all)) },
                leadingIcon = { Icon(Icons.Default.UnfoldLess, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(state.treeNodes, key = { it.path }) { node ->
                NbtNodeItem(
                    node = node,
                    onToggleExpand = { onToggleExpand(node.path) },
                    onEditValue = { onEditValue(node) },
                    onAddTag = { onAddTag(node) },
                    onRenameKey = { onRenameKey(node) },
                    onDeleteTag = { onDeleteTag(node) },
                    onCopyPath = { onCopyPath(node) },
                    onCopyValue = { onCopyValue(node) }
                )
            }
        }
    }
}

@Composable
private fun NbtNodeItem(
    node: NbtTreeNode,
    onToggleExpand: () -> Unit,
    onEditValue: () -> Unit,
    onAddTag: () -> Unit,
    onRenameKey: () -> Unit,
    onDeleteTag: () -> Unit,
    onCopyPath: () -> Unit,
    onCopyValue: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val typeColor = when (node.tag) {
        is NbtCompound -> Color(0xFF7C4DFF)
        is NbtList<*> -> Color(0xFF00B0FF)
        is NbtString -> Color(0xFF00C853)
        is NbtByte, is NbtShort, is NbtInt, is NbtLong -> Color(0xFFFF9100)
        is NbtFloat, is NbtDouble -> Color(0xFF2979FF)
        is NbtByteArray, is NbtIntArray, is NbtLongArray -> Color(0xFFF50057)
        else -> Color(0xFF888888)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (node.isContainer) onToggleExpand() else onEditValue()
            }
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width((node.depth * 16).dp))

        if (node.isContainer) {
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    if (node.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = typeColor.copy(alpha = 0.15f),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = node.tagTypeName,
                style = MaterialTheme.typography.labelSmall,
                color = typeColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Text(
            text = node.key,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = node.displayValue,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                if (node.isContainer) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.nbt_add_tag)) },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddTag()
                        }
                    )
                }
                if (!node.isContainer) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.nbt_edit_value)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEditValue()
                        }
                    )
                }
                if (node.parentTag is NbtCompound) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.nbt_rename_key)) },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRenameKey()
                        }
                    )
                }
                if (node.parentTag != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.nbt_delete_tag)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDeleteTag()
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.nbt_copy_path)) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onCopyPath()
                    }
                )
                if (!node.isContainer) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.nbt_copy_value)) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onCopyValue()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NbtChunkGridView(
    state: NbtEditorUiState,
    onSelectChunk: (Int, Int) -> Unit,
    onToggleSelection: (Int, Int) -> Unit,
    onSelectAll: () -> Unit,
    onInvert: () -> Unit,
    onClear: () -> Unit,
    onEditChunkNbt: (Int, Int) -> Unit,
    onDeleteChunk: () -> Unit,
    onPruneUseless: () -> Unit,
    onSelectDimension: (Int) -> Unit
) {
    val populatedSet = remember(state.chunkTable, state.populatedWorldChunks, state.isWorldChunkMode) {
        if (state.isWorldChunkMode) {
            state.populatedWorldChunks
        } else {
            state.chunkTable.filter { it.exists }.map { Pair(it.localX, it.localZ) }.toSet()
        }
    }

    val blockEntitiesSet = remember(state.blockEntityWorldChunks) {
        state.blockEntityWorldChunks
    }

    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.isWorldChunkMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.currentDimensionId == 0,
                    onClick = { onSelectDimension(0) },
                    label = { Text(stringResource(R.string.nbt_dim_overworld)) }
                )
                FilterChip(
                    selected = state.currentDimensionId == 1,
                    onClick = { onSelectDimension(1) },
                    label = { Text(stringResource(R.string.nbt_dim_nether)) }
                )
                FilterChip(
                    selected = state.currentDimensionId == 2,
                    onClick = { onSelectDimension(2) },
                    label = { Text(stringResource(R.string.nbt_dim_end)) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (state.isWorldChunkMode) {
                    stringResource(R.string.nbt_dim_stats, populatedSet.size)
                } else {
                    val pct = (populatedSet.size.toFloat() / 1024f) * 100f
                    stringResource(R.string.nbt_region_stats, populatedSet.size, pct)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onSelectAll) { Text(stringResource(R.string.nbt_action_select_all), fontSize = 11.sp) }
                TextButton(onClick = onInvert) { Text(stringResource(R.string.nbt_action_invert), fontSize = 11.sp) }
                TextButton(onClick = onClear) { Text(stringResource(R.string.nbt_action_clear), fontSize = 11.sp) }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.outlineVariant
            val selectionColor = MaterialTheme.colorScheme.error

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, outlineColor, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(state.chunkBoxSelectMode) {
                        if (state.chunkBoxSelectMode) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    dragStart = offset
                                    dragCurrent = offset
                                },
                                onDrag = { change, _ ->
                                    dragCurrent = change.position
                                },
                                onDragEnd = {
                                    val start = dragStart
                                    val end = dragCurrent
                                    if (start != null && end != null) {
                                        val minXPixel = min(start.x, end.x)
                                        val maxXPixel = max(start.x, end.x)
                                        val minZPixel = min(start.y, end.y)
                                        val maxZPixel = max(start.y, end.y)

                                        val cellW = size.width / 32f
                                        val cellH = size.height / 32f

                                        val startCellX = (minXPixel / cellW).toInt().coerceIn(0, 31)
                                        val endCellX = (maxXPixel / cellW).toInt().coerceIn(0, 31)
                                        val startCellZ = (minZPixel / cellH).toInt().coerceIn(0, 31)
                                        val endCellZ = (maxZPixel / cellH).toInt().coerceIn(0, 31)

                                        for (cz in startCellZ..endCellZ) {
                                            for (cx in startCellX..endCellX) {
                                                onToggleSelection(cx, cz)
                                            }
                                        }
                                    }
                                    dragStart = null
                                    dragCurrent = null
                                }
                            )
                        } else {
                            detectTapGestures { offset ->
                                val cellW = size.width / 32f
                                val cellH = size.height / 32f
                                val cx = (offset.x / cellW).toInt().coerceIn(0, 31)
                                val cz = (offset.y / cellH).toInt().coerceIn(0, 31)
                                onSelectChunk(cx, cz)
                            }
                        }
                    }
            ) {
                val cellW = size.width / 32f
                val cellH = size.height / 32f

                for (z in 0 until 32) {
                    for (x in 0 until 32) {
                        val pair = Pair(x, z)
                        val isPopulated = populatedSet.contains(pair)
                        val isSelected = state.selectedChunkSet.contains(pair) ||
                            (state.selectedChunkLocalX == x && state.selectedChunkLocalZ == z)
                        val hasBlockEntities = blockEntitiesSet.contains(pair)

                        val left = x * cellW
                        val top = z * cellH

                        val color = when {
                            isSelected -> selectionColor.copy(alpha = 0.7f)
                            isPopulated -> primaryColor.copy(alpha = 0.85f)
                            else -> outlineColor.copy(alpha = 0.15f)
                        }

                        drawRect(
                            color = color,
                            topLeft = Offset(left, top),
                            size = Size(cellW, cellH)
                        )

                        if (hasBlockEntities) {
                            drawCircle(
                                color = Color(0xFFFFD54F),
                                radius = min(cellW, cellH) / 4f,
                                center = Offset(left + cellW / 2f, top + cellH / 2f)
                            )
                        }
                    }
                }

                for (i in 0..32 step 8) {
                    val lineX = i * cellW
                    val lineY = i * cellH
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(lineX, 0f),
                        end = Offset(lineX, size.height),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(0f, lineY),
                        end = Offset(size.width, lineY),
                        strokeWidth = 1f
                    )
                }

                val ds = dragStart
                val dc = dragCurrent
                if (ds != null && dc != null) {
                    val rx = min(ds.x, dc.x)
                    val ry = min(ds.y, dc.y)
                    val rw = max(1f, kotlin.math.abs(dc.x - ds.x))
                    val rh = max(1f, kotlin.math.abs(dc.y - ds.y))

                    drawRect(
                        color = Color(0x442196F3),
                        topLeft = Offset(rx, ry),
                        size = Size(rw, rh)
                    )
                    drawRect(
                        color = Color(0xFF2196F3),
                        topLeft = Offset(rx, ry),
                        size = Size(rw, rh),
                        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (state.selectedChunkSet.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.nbt_chunk_selected_count, state.selectedChunkSet.size),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDeleteChunk,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.nbt_delete_selected_chunks, state.selectedChunkSet.size))
                    }
                } else if (state.selectedChunkLocalX in 0..31 && state.selectedChunkLocalZ in 0..31) {
                    val cx = state.selectedChunkLocalX
                    val cz = state.selectedChunkLocalZ
                    val isPopulated = populatedSet.contains(Pair(cx, cz))
                    val bx = cx * 16
                    val bz = cz * 16

                    Text(
                        text = stringResource(R.string.nbt_chunk_coord, cx, cz),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.nbt_block_range, bx, bx + 15, bz, bz + 15),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!state.isWorldChunkMode) {
                            Button(
                                onClick = { onEditChunkNbt(cx, cz) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isPopulated) stringResource(R.string.nbt_edit_chunk) else stringResource(R.string.nbt_init_empty_chunk))
                            }
                        }
                        if (isPopulated) {
                            OutlinedButton(
                                onClick = onDeleteChunk,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(R.string.nbt_delete_chunk))
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.nbt_mode_tap),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!state.isWorldChunkMode) {
                            Button(onClick = onPruneUseless) {
                                Icon(Icons.Outlined.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.nbt_prune_useless_chunks))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NbtEditValueDialog(
    node: NbtTreeNode,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Boolean
) {
    val initialText = when (val t = node.tag) {
        is NbtByte -> t.signed.toString()
        is NbtShort -> t.value.toString()
        is NbtInt -> t.value.toString()
        is NbtLong -> t.value.toString()
        is NbtFloat -> t.value.toString()
        is NbtDouble -> t.value.toString()
        is NbtString -> t.value
        else -> ""
    }
    var text by remember { mutableStateOf(initialText) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nbt_edit_value)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${node.tagTypeName} - ${node.key}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        isError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                    supportingText = {
                        if (isError) Text(stringResource(R.string.nbt_invalid_number), color = MaterialTheme.colorScheme.error)
                    },
                    keyboardOptions = if (node.tag is NbtString) KeyboardOptions.Default else KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ok = onConfirm(text)
                if (!ok) isError = true
            }) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NbtAddTagDialog(
    parentNode: NbtTreeNode,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Boolean
) {
    val isParentCompound = parentNode.tag is NbtCompound
    var key by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("String") }
    var initialVal by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val types = listOf("Compound", "List", "String", "Int", "Byte", "Short", "Long", "Float", "Double", "ByteArray", "IntArray", "LongArray")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nbt_add_tag)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isParentCompound) {
                    OutlinedTextField(
                        value = key,
                        onValueChange = {
                            key = it
                            isError = false
                        },
                        label = { Text(stringResource(R.string.nbt_tag_key)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = isError
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.nbt_tag_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        types.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    selectedType = t
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedType !in listOf("Compound", "List", "ByteArray", "IntArray", "LongArray")) {
                    OutlinedTextField(
                        value = initialVal,
                        onValueChange = { initialVal = it },
                        label = { Text(stringResource(R.string.nbt_tag_value)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ok = onConfirm(key.trim(), selectedType, initialVal)
                if (!ok) isError = true
            }) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun NbtRenameTagDialog(
    node: NbtTreeNode,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Boolean
) {
    var key by remember { mutableStateOf(node.key) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nbt_rename_key)) },
        text = {
            OutlinedTextField(
                value = key,
                onValueChange = {
                    key = it
                    isError = false
                },
                label = { Text(stringResource(R.string.nbt_tag_key)) },
                modifier = Modifier.fillMaxWidth(),
                isError = isError
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val ok = onConfirm(key.trim())
                if (!ok) isError = true
            }) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun NbtAddLevelDbKeyDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Boolean
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nbt_leveldb_add_key)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = {
                        key = it
                        isError = false
                    },
                    label = { Text(stringResource(R.string.nbt_leveldb_key_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.nbt_leveldb_val_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ok = onConfirm(key.trim(), value)
                if (!ok) isError = true
            }) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun NbtBinaryPreviewDialog(
    record: LevelDbRecord,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(record.displayName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Key: ${record.keyString.ifEmpty { "0x" + record.keyToHex() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Size: ${record.valueSize} bytes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "0x" + record.keyToHex(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.nbt_delete_entry))
            }
        }
    )
}

@Composable
private fun NbtLevelDbPruneDialog(
    recordCount: Int,
    onDismiss: () -> Unit,
    onStrategySelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nbt_leveldb_prune_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.nbt_leveldb_prune_msg, recordCount, recordCount),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onStrategySelected(0) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.nbt_leveldb_prune_opt_2d))
                }
                OutlinedButton(
                    onClick = { onStrategySelected(1) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.nbt_leveldb_prune_opt_core))
                }
                OutlinedButton(
                    onClick = { onStrategySelected(2) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.nbt_leveldb_prune_opt_medium))
                }
                OutlinedButton(
                    onClick = { onStrategySelected(3) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.nbt_leveldb_prune_opt_large))
                }
                OutlinedButton(
                    onClick = { onStrategySelected(4) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.nbt_leveldb_prune_opt_all, recordCount))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun NbtPruneUselessDialog(
    onDismiss: () -> Unit,
    onOptionSelected: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nbt_prune_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onOptionSelected(0L) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.nbt_prune_option_zero))
                }
                OutlinedButton(
                    onClick = { onOptionSelected(20L) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.nbt_prune_option_low))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
