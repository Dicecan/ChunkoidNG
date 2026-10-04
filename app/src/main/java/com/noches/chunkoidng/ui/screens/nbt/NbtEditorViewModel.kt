package com.noches.chunkoidng.ui.screens.nbt

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.gamemods.nbtmanipulator.*
import com.noches.chunkoidng.R
import com.noches.chunkoidng.core.leveldb.BedrockLevelDbHelper
import com.noches.chunkoidng.core.leveldb.LevelDbCategory
import com.noches.chunkoidng.core.leveldb.LevelDbRecord
import com.noches.chunkoidng.core.nbt.MinecraftSemanticDescriptor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class NbtEditorMode {
    EMPTY,
    WORKSPACE,
    NBT_TREE,
    CHUNK_GRID
}

data class NbtEditorUiState(
    val mode: NbtEditorMode = NbtEditorMode.EMPTY,
    val title: String = "",
    val subtitle: String? = null,
    val isDirty: Boolean = false,
    val autoSave: Boolean = false,
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val userMessage: String? = null,

    val currentFileName: String = "",
    val currentFilePath: String? = null,
    val currentFileUri: Uri? = null,
    val currentNbtFile: NbtFile? = null,
    val isCompressed: Boolean = true,
    val isLittleEndian: Boolean = false,
    val readHeaders: Boolean = false,
    val headerVersion: Int? = null,

    val activeWorkspaceName: String = "",
    val originalZipBytes: ByteArray? = null,
    val selectedZipEntryName: String? = null,
    val workspaceZipEntries: Map<String, ByteArray> = emptyMap(),

    val hasLevelDb: Boolean = false,
    val levelDbRecords: List<LevelDbRecord> = emptyList(),
    val filteredLevelDbRecords: List<LevelDbRecord> = emptyList(),
    val selectedCategory: LevelDbCategory = LevelDbCategory.ALL,
    val levelDbSearchQuery: String = "",
    val currentLevelDbRecord: LevelDbRecord? = null,

    val currentRegionFile: AnvilRegionFile? = null,
    val currentRegionCachedFile: File? = null,
    val currentChunkX: Int = 0,
    val currentChunkZ: Int = 0,
    val chunkTable: List<AnvilRegionFile.ChunkInfo> = emptyList(),
    val selectedChunkLocalX: Int = -1,
    val selectedChunkLocalZ: Int = -1,
    val selectedChunkSet: Set<Pair<Int, Int>> = emptySet(),
    val isWorldChunkMode: Boolean = false,
    val populatedWorldChunks: Set<Pair<Int, Int>> = emptySet(),
    val blockEntityWorldChunks: Set<Pair<Int, Int>> = emptySet(),
    val currentDimensionId: Int = 0,

    val treeNodes: List<NbtTreeNode> = emptyList(),
    val expandedPaths: Set<String> = emptySet(),
    val treeSearchQuery: String = "",

    val showEditValueDialog: NbtTreeNode? = null,
    val showArrayEditorDialog: NbtTreeNode? = null,
    val showAddTagDialog: NbtTreeNode? = null,
    val showRenameTagDialog: NbtTreeNode? = null,
    val showDeleteTagDialog: NbtTreeNode? = null,
    val showAddLevelDbKeyDialog: Boolean = false,
    val showLevelDbPruneDialog: Boolean = false,
    val showUnsavedChangesDialog: Boolean = false,
    val showBinaryPreviewDialog: LevelDbRecord? = null,
    val showPruneUselessDialog: Boolean = false,
    val chunkBoxSelectMode: Boolean = false
)

class NbtEditorViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val MAX_ARCHIVE_ENTRIES = 100_000
        const val MAX_ARCHIVE_ENTRY_BYTES = 512L * 1024 * 1024
        const val MAX_ARCHIVE_TOTAL_BYTES = 2L * 1024 * 1024 * 1024
    }

    private val _uiState = MutableStateFlow(NbtEditorUiState())
    val uiState: StateFlow<NbtEditorUiState> = _uiState.asStateFlow()

    private var levelDbHelper: BedrockLevelDbHelper? = null
    private var levelDbCachedFolder: File? = null
    private var allLevelDbRecords: List<LevelDbRecord> = emptyList()

    private val prefs by lazy {
        application.getSharedPreferences("nbt_editor_prefs", Context.MODE_PRIVATE)
    }

    init {
        val autoSave = prefs.getBoolean("nbt_auto_save", false)
        _uiState.update { it.copy(autoSave = autoSave) }
        restoreWorkspaceSession()
    }

    fun setAutoSave(enabled: Boolean) {
        prefs.edit().putBoolean("nbt_auto_save", enabled).apply()
        _uiState.update { it.copy(autoSave = enabled) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun openFileFromUri(uri: Uri) {
        val context = getApplication<Application>()
        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "NBT File"
        _uiState.update {
            it.copy(
                isLoading = true,
                loadingMessage = context.getString(R.string.nbt_loading_read_file),
                currentFileUri = uri,
                currentFilePath = null,
                currentFileName = fileName
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (AnvilRegionFile.isRegionFileName(fileName)) {
                    loadRegionFromUri(uri, fileName)
                    return@launch
                }

                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                userMessage = context.getString(R.string.nbt_load_failed)
                            )
                        }
                    }
                    return@launch
                }

                if (isZipArchive(bytes)) {
                    handleZipArchive(bytes, fileName)
                } else {
                    withContext(Dispatchers.Main) {
                        clearActiveWorkspace()
                        parseNbtBytes(bytes, fileName)
                    }
                }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "${context.getString(R.string.nbt_load_failed)}: ${t.message ?: t.javaClass.simpleName}"
                        )
                    }
                }
            }
        }
    }

    fun openFile(file: File) {
        val context = getApplication<Application>()
        _uiState.update {
            it.copy(
                isLoading = true,
                loadingMessage = context.getString(R.string.nbt_loading_load_world),
                currentFileUri = null,
                currentFilePath = file.absolutePath,
                currentFileName = file.name
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (file.isDirectory && (File(file, "CURRENT").exists() || file.name.equals("db", true))) {
                    loadLevelDbFromFolder(file)
                    return@launch
                }

                if (file.isFile && (file.name == "CURRENT" || file.name.endsWith(".ldb") || file.name.endsWith(".log"))) {
                    val parent = file.parentFile
                    if (parent != null && (parent.name.equals("db", true) || File(parent, "CURRENT").exists())) {
                        loadLevelDbFromFolder(parent)
                        return@launch
                    }
                }

                if (AnvilRegionFile.isRegionFileName(file.name)) {
                    loadRegionFromFile(file)
                    return@launch
                }

                val bytes = file.readBytes()
                if (isZipArchive(bytes)) {
                    handleZipArchive(bytes, file.name)
                } else {
                    withContext(Dispatchers.Main) {
                        clearActiveWorkspace()
                        parseNbtBytes(bytes, file.name)
                    }
                }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "${context.getString(R.string.nbt_load_failed)}: ${t.message ?: t.javaClass.simpleName}"
                        )
                    }
                }
            }
        }
    }

    private suspend fun handleZipArchive(bytes: ByteArray, archiveName: String) {
        val context = getApplication<Application>()
        val workspaceEntries = mutableMapOf<String, ByteArray>()
        var hasLevelDb = false
        var entryCount = 0
        var totalBytes = 0L

        val cacheFolder = File(context.cacheDir, "nbt_active_leveldb")
        if (cacheFolder.exists()) cacheFolder.deleteRecursively()
        val archiveRoot = cacheFolder.canonicalFile

        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (++entryCount > MAX_ARCHIVE_ENTRIES) throw IOException("Archive contains too many entries")
                    val canonicalEntry = File(cacheFolder, entry.name).canonicalFile
                    if (canonicalEntry.path != archiveRoot.path && !canonicalEntry.path.startsWith(archiveRoot.path + File.separator)) {
                        throw SecurityException("Archive contains an invalid path")
                    }
                    if (!entry.isDirectory) {
                        val name = entry.name
                        if (name.startsWith("db/") || name.startsWith("db\\")) {
                            hasLevelDb = true
                            val relName = name.substringAfter("db/").substringAfter("db\\")
                            if (relName.isNotEmpty() && relName != "LOCK") {
                                cacheFolder.mkdirs()
                                val targetFile = File(cacheFolder, relName)
                                var entryBytes = 0L
                                targetFile.parentFile?.mkdirs()
                                targetFile.outputStream().use { out ->
                                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                    while (true) {
                                        val read = zis.read(buffer)
                                        if (read < 0) break
                                        entryBytes += read
                                        totalBytes += read
                                        if (entryBytes > MAX_ARCHIVE_ENTRY_BYTES || totalBytes > MAX_ARCHIVE_TOTAL_BYTES) {
                                            throw IOException("Archive exceeds extraction limits")
                                        }
                                        out.write(buffer, 0, read)
                                    }
                                }
                            }
                        } else if (name.endsWith(".dat", ignoreCase = true) ||
                            name.endsWith(".nbt", ignoreCase = true) ||
                            name.endsWith(".dat_old", ignoreCase = true) ||
                            name.endsWith(".mca", ignoreCase = true) ||
                            name.endsWith(".mcr", ignoreCase = true) ||
                            name.equals("levelname.txt", ignoreCase = true)
                        ) {
                            val value = zis.readBytes()
                            totalBytes += value.size
                            if (value.size > MAX_ARCHIVE_ENTRY_BYTES || totalBytes > MAX_ARCHIVE_TOTAL_BYTES) {
                                throw IOException("Archive exceeds extraction limits")
                            }
                            workspaceEntries[name] = value
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            cacheFolder.deleteRecursively()
            throw e
        }

        if (hasLevelDb) {
            workspaceEntries["db/"] = byteArrayOf()
        }

        saveWorkspaceToCache(bytes, archiveName)

        var records: List<LevelDbRecord> = emptyList()
        if (hasLevelDb) {
            levelDbCachedFolder = cacheFolder
            levelDbHelper?.close()
            levelDbHelper = BedrockLevelDbHelper(cacheFolder)
            records = try {
                levelDbHelper?.getAllRecords() ?: emptyList()
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                cacheFolder.deleteRecursively()
                throw IOException("Unable to open LevelDB: ${t.message ?: t.javaClass.simpleName}", t)
            }
            allLevelDbRecords = records
        }

        withContext(Dispatchers.Main) {
            _uiState.update {
                it.copy(
                    mode = NbtEditorMode.WORKSPACE,
                    title = archiveName,
                subtitle = if (hasLevelDb) context.getString(R.string.nbt_workspace_leveldb) else context.getString(R.string.nbt_workspace_file_count, workspaceEntries.size),
                    isLoading = false,
                    activeWorkspaceName = archiveName,
                    originalZipBytes = bytes,
                    selectedZipEntryName = if (hasLevelDb) "db/" else null,
                    workspaceZipEntries = workspaceEntries,
                    hasLevelDb = hasLevelDb,
                    levelDbRecords = records,
                    filteredLevelDbRecords = records,
                    selectedCategory = LevelDbCategory.ALL,
                    levelDbSearchQuery = "",
                    isDirty = false
                )
            }
        }
    }

    private suspend fun loadLevelDbFromFolder(folder: File) {
        val context = getApplication<Application>()
        val cacheFolder = File(context.cacheDir, "nbt_active_leveldb")
        if (cacheFolder.exists()) cacheFolder.deleteRecursively()
        cacheFolder.mkdirs()

        folder.listFiles()?.forEach { f ->
            if (!f.isDirectory && f.name != "LOCK") {
                if (f.name == "CURRENT" || f.name.startsWith("MANIFEST") || f.name.endsWith(".ldb") || f.name.endsWith(".log")) {
                    cacheFolder.mkdirs()
                }
                f.copyTo(File(cacheFolder, f.name), overwrite = true)
            }
        }

        levelDbCachedFolder = cacheFolder
        levelDbHelper?.close()
        levelDbHelper = BedrockLevelDbHelper(cacheFolder)

        val records = try {
            levelDbHelper?.getAllRecords() ?: emptyList()
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            cacheFolder.deleteRecursively()
            throw IOException("Unable to open LevelDB: ${t.message ?: t.javaClass.simpleName}", t)
        }
        allLevelDbRecords = records

        withContext(Dispatchers.Main) {
            _uiState.update {
                it.copy(
                    mode = NbtEditorMode.WORKSPACE,
                    title = folder.name,
                    subtitle = context.getString(R.string.nbt_workspace_leveldb),
                    isLoading = false,
                    activeWorkspaceName = folder.name,
                    hasLevelDb = true,
                    levelDbRecords = records,
                    filteredLevelDbRecords = records,
                    selectedCategory = LevelDbCategory.ALL,
                    levelDbSearchQuery = "",
                    workspaceZipEntries = emptyMap(),
                    isDirty = false
                )
            }
        }
    }

    private fun loadRegionFromUri(uri: Uri, fileName: String) {
        val context = getApplication<Application>()
        try {
            val cacheRegion = File(context.cacheDir, "nbt_active_region.mca")
            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheRegion.outputStream().use { output -> input.copyTo(output) }
            }
            openAnvilRegion(cacheRegion, fileName)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    userMessage = "${context.getString(R.string.nbt_load_failed)}: ${e.message}"
                )
            }
        }
    }

    private fun loadRegionFromFile(file: File) {
        val context = getApplication<Application>()
        try {
            val cacheRegion = File(context.cacheDir, "nbt_active_region.mca")
            file.copyTo(cacheRegion, overwrite = true)
            openAnvilRegion(cacheRegion, file.name)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    userMessage = "${context.getString(R.string.nbt_load_failed)}: ${e.message}"
                )
            }
        }
    }

    private fun openAnvilRegion(regionFile: File, displayName: String) {
        val context = getApplication<Application>()
        _uiState.value.currentRegionFile?.close()
        val anvil = AnvilRegionFile(regionFile)
        val table = anvil.getChunkTable()
        _uiState.update {
            it.copy(
                mode = NbtEditorMode.CHUNK_GRID,
                title = displayName,
                subtitle = context.getString(R.string.nbt_workspace_anvil),
                isLoading = false,
                currentRegionFile = anvil,
                currentRegionCachedFile = regionFile,
                chunkTable = table,
                selectedChunkLocalX = -1,
                selectedChunkLocalZ = -1,
                selectedChunkSet = emptySet(),
                isWorldChunkMode = false,
                isDirty = false
            )
        }
    }

    fun switchToFileInWorkspace(entryName: String) {
        val state = _uiState.value
        val context = getApplication<Application>()
        if (entryName.startsWith("db/")) {
            _uiState.update {
                it.copy(
                    mode = NbtEditorMode.WORKSPACE,
                    selectedZipEntryName = entryName,
                    currentFileName = "db/"
                )
            }
            return
        }

        val entryBytes = state.workspaceZipEntries[entryName] ?: return
        val fileName = entryName.substringAfterLast('/')

        if (AnvilRegionFile.isRegionFileName(entryName)) {
            try {
                val cacheRegion = File(context.cacheDir, "nbt_active_region.mca")
                cacheRegion.writeBytes(entryBytes)
                _uiState.update { it.copy(selectedZipEntryName = entryName) }
                openAnvilRegion(cacheRegion, fileName)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "${context.getString(R.string.nbt_load_failed)}: ${e.message}")
                }
            }
        } else {
            _uiState.update { it.copy(selectedZipEntryName = entryName) }
            parseNbtBytes(entryBytes, fileName)
        }
    }

    fun loadLevelDbRecord(record: LevelDbRecord) {
        val helper = levelDbHelper ?: return
        val context = getApplication<Application>()

        if (record.isNbt) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = context.getString(R.string.nbt_loading_read_file)
                )
            }
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val rawBytes = helper.get(record.key)
                    if (rawBytes == null) {
                        withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    userMessage = context.getString(R.string.nbt_load_failed)
                                )
                            }
                        }
                        return@launch
                    }

                    val nbtFile = BedrockLevelDbHelper.readBedrockNbt(rawBytes, record.hasMultipleCompounds)
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                mode = NbtEditorMode.NBT_TREE,
                                title = record.displayName,
                            subtitle = context.getString(R.string.nbt_workspace_db_record, record.valueSize),
                                isLoading = false,
                                currentNbtFile = nbtFile,
                                currentFileName = record.displayName,
                                currentLevelDbRecord = record,
                                isDirty = false,
                                isCompressed = false,
                                isLittleEndian = true,
                                readHeaders = false
                            )
                        }
                        setupNbtTree(nbtFile, record.displayName)
                    }
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                userMessage = "${context.getString(R.string.nbt_load_failed)}: ${t.message}"
                            )
                        }
                    }
                }
            }
        } else {
            _uiState.update { it.copy(showBinaryPreviewDialog = record) }
        }
    }

    fun closeBinaryPreview() {
        _uiState.update { it.copy(showBinaryPreviewDialog = null) }
    }

    fun loadChunkFromRegion(localX: Int, localZ: Int) {
        val region = _uiState.value.currentRegionFile ?: return
        val info = region.getChunkInfo(localX, localZ)
        if (info.exists) {
            try {
                val chunkNbt = region.readChunkNbt(localX, localZ)
                if (chunkNbt != null) {
                    _uiState.update {
                        it.copy(
                            mode = NbtEditorMode.NBT_TREE,
                            title = "Chunk [$localX, $localZ]",
                            subtitle = region.file.name,
                            currentNbtFile = chunkNbt,
                            currentFileName = "Chunk [$localX, $localZ]",
                            currentChunkX = localX,
                            currentChunkZ = localZ,
                            isDirty = false,
                            isCompressed = true,
                            isLittleEndian = false,
                            readHeaders = false
                        )
                    }
                    setupNbtTree(chunkNbt, "Chunk [$localX, $localZ]")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = e.message) }
            }
        } else {
            val root = NbtCompound()
            root["DataVersion"] = NbtInt(3465)
            val newChunk = NbtFile("", root)
            _uiState.update {
                it.copy(
                    mode = NbtEditorMode.NBT_TREE,
                    title = "Chunk [$localX, $localZ]",
                    subtitle = region.file.name,
                    currentNbtFile = newChunk,
                    currentFileName = "Chunk [$localX, $localZ]",
                    currentChunkX = localX,
                    currentChunkZ = localZ,
                    isDirty = true,
                    isCompressed = true,
                    isLittleEndian = false,
                    readHeaders = false
                )
            }
            setupNbtTree(newChunk, "Chunk [$localX, $localZ]")
        }
    }

    private fun parseNbtBytes(bytes: ByteArray, fileName: String) {
        val attempts = listOf(
            Triple(true, false, false),
            Triple(false, true, true),
            Triple(true, false, true),
            Triple(true, true, false),
            Triple(true, true, true),
            Triple(false, false, false),
            Triple(false, false, true),
            Triple(false, true, false)
        )

        var loadedNbt: NbtFile? = null
        var comp = true
        var little = false
        var headers = false
        var version: Int? = null

        for ((c, l, h) in attempts) {
            try {
                val nbt = NbtIO.readNbtFile(ByteArrayInputStream(bytes), c, l, h)
                if (nbt.tag !is NbtEnd) {
                    loadedNbt = nbt
                    comp = c
                    little = l
                    headers = h
                    version = nbt.version
                    break
                }
            } catch (_: Exception) {}
        }

        if (loadedNbt == null) {
            val context = getApplication<Application>()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    userMessage = context.getString(R.string.nbt_load_failed)
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                mode = NbtEditorMode.NBT_TREE,
                title = fileName,
                subtitle = if (loadedNbt.name.isNotEmpty()) loadedNbt.name else "Root",
                isLoading = false,
                currentNbtFile = loadedNbt,
                currentFileName = fileName,
                isCompressed = comp,
                isLittleEndian = little,
                readHeaders = headers,
                headerVersion = version,
                isDirty = false
            )
        }
        setupNbtTree(loadedNbt, fileName)
    }

    private fun setupNbtTree(nbtFile: NbtFile, rootName: String) {
        val name = if (nbtFile.name.isNotEmpty()) nbtFile.name else rootName
        val paths = mutableSetOf(name)
        _uiState.update {
            it.copy(
                expandedPaths = paths,
                treeSearchQuery = ""
            )
        }
        rebuildTreeNodes()
    }

    fun toggleNodeExpanded(path: String) {
        val paths = _uiState.value.expandedPaths.toMutableSet()
        if (paths.contains(path)) {
            paths.remove(path)
        } else {
            paths.add(path)
        }
        _uiState.update { it.copy(expandedPaths = paths) }
        rebuildTreeNodes()
    }

    fun expandAll() {
        val root = _uiState.value.currentNbtFile?.tag ?: return
        val rootKey = _uiState.value.currentNbtFile?.name?.ifEmpty { "Root" } ?: "Root"
        val paths = mutableSetOf<String>()

        fun collect(tag: NbtTag, path: String) {
            paths.add(path)
            when (tag) {
                is NbtCompound -> tag.forEach { (k, v) -> collect(v, "$path/$k") }
                is NbtList<*> -> tag.forEachIndexed { i, v -> collect(v, "$path/[$i]") }
                is NbtByteArray, is NbtIntArray, is NbtLongArray -> Unit
                else -> {}
            }
        }
        collect(root, rootKey)
        _uiState.update { it.copy(expandedPaths = paths) }
        rebuildTreeNodes()
    }

    fun collapseAll() {
        val rootKey = _uiState.value.currentNbtFile?.name?.ifEmpty { "Root" } ?: "Root"
        _uiState.update { it.copy(expandedPaths = setOf(rootKey)) }
        rebuildTreeNodes()
    }

    fun setTreeSearchQuery(query: String) {
        _uiState.update { it.copy(treeSearchQuery = query) }
        rebuildTreeNodes()
    }

    private fun rebuildTreeNodes() {
        val state = _uiState.value
        val rootTag = state.currentNbtFile?.tag ?: run {
            _uiState.update { it.copy(treeNodes = emptyList()) }
            return
        }
        val rootKey = state.currentNbtFile?.name?.ifEmpty { "Root" } ?: "Root"
        val query = state.treeSearchQuery.trim().lowercase()
        val result = mutableListOf<NbtTreeNode>()
        val context = getApplication<Application>()

        if (query.isNotEmpty()) {
            fun searchTraverse(tag: NbtTag, key: String, parent: NbtTag?, depth: Int, path: String, index: Int) {
                val node = NbtTreeNode(key, tag, parent, depth, false, path, index)
                val semantic = MinecraftSemanticDescriptor.describeNbt(key, path)
                val semanticMatch = if (semantic != null) {
                    context.getString(semantic.titleRes).lowercase().contains(query) ||
                        (semantic.guideRes != null && context.getString(semantic.guideRes).lowercase().contains(query))
                } else false
                val matches = key.lowercase().contains(query) ||
                    path.lowercase().contains(query) ||
                    node.displayValue.lowercase().contains(query) ||
                    semanticMatch
                if (matches) {
                    result.add(node)
                }
                when (tag) {
                    is NbtCompound -> tag.forEach { (k, child) -> searchTraverse(child, k, tag, depth + 1, "$path/$k", -1) }
                    is NbtList<*> -> tag.forEachIndexed { i, child -> searchTraverse(child, "[$i]", tag, depth + 1, "$path/[$i]", i) }
                    is NbtByteArray, is NbtIntArray, is NbtLongArray -> Unit
                    else -> {}
                }
            }
            searchTraverse(rootTag, rootKey, null, 0, rootKey, -1)
        } else {
            fun traverse(tag: NbtTag, key: String, parent: NbtTag?, depth: Int, path: String, index: Int) {
                val isExpanded = state.expandedPaths.contains(path)
                val node = NbtTreeNode(key, tag, parent, depth, isExpanded, path, index)
                result.add(node)

                if (isExpanded) {
                    when (tag) {
                        is NbtCompound -> tag.forEach { (k, child) -> traverse(child, k, tag, depth + 1, "$path/$k", -1) }
                        is NbtList<*> -> tag.forEachIndexed { i, child -> traverse(child, "[$i]", tag, depth + 1, "$path/[$i]", i) }
                        is NbtByteArray, is NbtIntArray, is NbtLongArray -> Unit
                        else -> {}
                    }
                }
            }
            traverse(rootTag, rootKey, null, 0, rootKey, -1)
        }

        _uiState.update { it.copy(treeNodes = result) }
    }

    fun requestEditValue(node: NbtTreeNode) {
        if (node.tag is NbtByteArray || node.tag is NbtIntArray || node.tag is NbtLongArray) {
            _uiState.update { it.copy(showArrayEditorDialog = node) }
        } else {
            _uiState.update { it.copy(showEditValueDialog = node) }
        }
    }

    fun dismissEditValueDialog() {
        _uiState.update { it.copy(showEditValueDialog = null) }
    }

    fun dismissArrayEditorDialog() {
        _uiState.update { it.copy(showArrayEditorDialog = null) }
    }

    /** Updates one primitive array element without materializing the whole array as tree nodes. */
    fun applyArrayElementUpdate(node: NbtTreeNode, index: Int, input: String): Boolean {
        val tag = node.tag
        val valid = when (tag) {
            is NbtByteArray -> input.toByteOrNull()?.let { value ->
                if (index in tag.value.indices) {
                    tag.value[index] = value
                    true
                } else false
            }
            is NbtIntArray -> input.toIntOrNull()?.let { value ->
                if (index in tag.value.indices) {
                    tag.value[index] = value
                    true
                } else false
            }
            is NbtLongArray -> input.toLongOrNull()?.let { value ->
                if (index in tag.value.indices) {
                    tag.value[index] = value
                    true
                } else false
            }
            else -> false
        } ?: false

        if (!valid) return false

        _uiState.update { it.copy(isDirty = true) }
        rebuildTreeNodes()
        return true
    }

    fun applyNodeValueUpdate(node: NbtTreeNode, input: String): Boolean {
        val currentTag = node.tag
        val updatedTag: NbtTag? = try {
            when (currentTag) {
                is NbtByte -> input.toByteOrNull()?.let { NbtByte(it) }
                is NbtShort -> input.toShortOrNull()?.let { NbtShort(it) }
                is NbtInt -> input.toIntOrNull()?.let { NbtInt(it) }
                is NbtLong -> input.toLongOrNull()?.let { NbtLong(it) }
                is NbtFloat -> input.toFloatOrNull()?.let { NbtFloat(it) }
                is NbtDouble -> input.toDoubleOrNull()?.let { NbtDouble(it) }
                is NbtString -> NbtString(input)
                else -> null
            }
        } catch (_: Exception) { null }

        if (updatedTag == null) return false

        val parent = node.parentTag
        when (parent) {
            is NbtCompound -> parent[node.key] = updatedTag
            is NbtList<*> -> {
                if (node.listIndex in 0 until parent.size) {
                    @Suppress("UNCHECKED_CAST")
                    (parent as MutableList<NbtTag>)[node.listIndex] = updatedTag
                }
            }
            is NbtByteArray -> {
                if (node.listIndex in 0 until parent.value.size && updatedTag is NbtByte) {
                    parent.value[node.listIndex] = updatedTag.signed
                }
            }
            is NbtIntArray -> {
                if (node.listIndex in 0 until parent.value.size && updatedTag is NbtInt) {
                    parent.value[node.listIndex] = updatedTag.value
                }
            }
            is NbtLongArray -> {
                if (node.listIndex in 0 until parent.value.size && updatedTag is NbtLong) {
                    parent.value[node.listIndex] = updatedTag.value
                }
            }
            null -> _uiState.value.currentNbtFile?.tag = updatedTag
            else -> {}
        }

        node.tag = updatedTag
        _uiState.update { it.copy(isDirty = true, showEditValueDialog = null) }
        rebuildTreeNodes()
        return true
    }

    fun requestAddTag(parentNode: NbtTreeNode) {
        _uiState.update { it.copy(showAddTagDialog = parentNode) }
    }

    fun dismissAddTagDialog() {
        _uiState.update { it.copy(showAddTagDialog = null) }
    }

    fun addTagToNode(parentNode: NbtTreeNode, key: String, typeName: String, initialValue: String): Boolean {
        val newTag: NbtTag? = try {
            when (typeName) {
                "Compound" -> NbtCompound()
                "List" -> NbtList<NbtTag>()
                "String" -> NbtString(initialValue)
                "Int" -> NbtInt(initialValue.toIntOrNull() ?: 0)
                "Byte" -> NbtByte(initialValue.toByteOrNull() ?: 0)
                "Short" -> NbtShort(initialValue.toShortOrNull() ?: 0)
                "Long" -> NbtLong(initialValue.toLongOrNull() ?: 0L)
                "Float" -> NbtFloat(initialValue.toFloatOrNull() ?: 0f)
                "Double" -> NbtDouble(initialValue.toDoubleOrNull() ?: 0.0)
                "ByteArray" -> NbtByteArray(byteArrayOf())
                "IntArray" -> NbtIntArray(intArrayOf())
                "LongArray" -> NbtLongArray(longArrayOf())
                else -> null
            }
        } catch (_: Exception) { null }

        if (newTag == null) return false

        when (val p = parentNode.tag) {
            is NbtCompound -> {
                if (key.isEmpty()) return false
                p[key] = newTag
            }
            is NbtList<*> -> {
                @Suppress("UNCHECKED_CAST")
                (p as MutableList<NbtTag>).add(newTag)
            }
            is NbtByteArray -> {
                if (newTag is NbtByte) {
                    val list = p.value.toMutableList()
                    list.add(newTag.signed)
                    p.value = list.toByteArray()
                }
            }
            is NbtIntArray -> {
                if (newTag is NbtInt) {
                    val list = p.value.toMutableList()
                    list.add(newTag.value)
                    p.value = list.toIntArray()
                }
            }
            is NbtLongArray -> {
                if (newTag is NbtLong) {
                    val list = p.value.toMutableList()
                    list.add(newTag.value)
                    p.value = list.toLongArray()
                }
            }
            else -> return false
        }

        val paths = _uiState.value.expandedPaths.toMutableSet()
        paths.add(parentNode.path)
        _uiState.update {
            it.copy(
                isDirty = true,
                showAddTagDialog = null,
                expandedPaths = paths
            )
        }
        rebuildTreeNodes()
        return true
    }

    fun requestRenameTag(node: NbtTreeNode) {
        _uiState.update { it.copy(showRenameTagDialog = node) }
    }

    fun dismissRenameTagDialog() {
        _uiState.update { it.copy(showRenameTagDialog = null) }
    }

    fun renameTagKey(node: NbtTreeNode, newKey: String): Boolean {
        val parent = node.parentTag as? NbtCompound ?: return false
        if (newKey.isEmpty() || (newKey != node.key && parent.containsKey(newKey))) return false

        val tag = parent.remove(node.key) ?: return false
        parent[newKey] = tag
        node.key = newKey
        _uiState.update { it.copy(isDirty = true, showRenameTagDialog = null) }
        rebuildTreeNodes()
        return true
    }

    fun requestDeleteTag(node: NbtTreeNode) {
        _uiState.update { it.copy(showDeleteTagDialog = node) }
    }

    fun dismissDeleteTagDialog() {
        _uiState.update { it.copy(showDeleteTagDialog = null) }
    }

    fun confirmDeleteTag(node: NbtTreeNode) {
        when (val parent = node.parentTag) {
            is NbtCompound -> parent.remove(node.key)
            is NbtList<*> -> {
                if (node.listIndex in 0 until parent.size) {
                    parent.removeAt(node.listIndex)
                }
            }
            is NbtByteArray -> {
                if (node.listIndex in 0 until parent.value.size) {
                    val list = parent.value.toMutableList()
                    list.removeAt(node.listIndex)
                    parent.value = list.toByteArray()
                }
            }
            is NbtIntArray -> {
                if (node.listIndex in 0 until parent.value.size) {
                    val list = parent.value.toMutableList()
                    list.removeAt(node.listIndex)
                    parent.value = list.toIntArray()
                }
            }
            is NbtLongArray -> {
                if (node.listIndex in 0 until parent.value.size) {
                    val list = parent.value.toMutableList()
                    list.removeAt(node.listIndex)
                    parent.value = list.toLongArray()
                }
            }
            else -> {}
        }
        _uiState.update { it.copy(isDirty = true, showDeleteTagDialog = null) }
        rebuildTreeNodes()
    }

    fun setLevelDbCategory(category: LevelDbCategory) {
        _uiState.update {
            it.copy(selectedCategory = category)
        }
        applyLevelDbFilter()
    }

    fun setLevelDbSearch(query: String) {
        _uiState.update {
            it.copy(levelDbSearchQuery = query)
        }
        applyLevelDbFilter()
    }

    private fun applyLevelDbFilter() {
        val cat = _uiState.value.selectedCategory
        val q = _uiState.value.levelDbSearchQuery.trim().lowercase()
        val context = getApplication<Application>()

        val filtered = allLevelDbRecords.filter { record ->
            val matchCat = (cat == LevelDbCategory.ALL || record.category == cat)
            if (!matchCat) return@filter false
            if (q.isEmpty()) return@filter true

            val semantic = MinecraftSemanticDescriptor.describeLevelDbRecord(record)
            val semanticMatch = if (semantic != null) {
                context.getString(semantic.titleRes).lowercase().contains(q) ||
                    (semantic.guideRes != null && context.getString(semantic.guideRes).lowercase().contains(q))
            } else false

            record.displayName.lowercase().contains(q) ||
                record.keyString.lowercase().contains(q) ||
                record.keyToHex().contains(q) ||
                (record.chunkX != null && record.chunkX.toString() == q) ||
                (record.chunkZ != null && record.chunkZ.toString() == q) ||
                semanticMatch
        }

        _uiState.update { it.copy(filteredLevelDbRecords = filtered) }
    }

    fun requestAddLevelDbKey() {
        _uiState.update { it.copy(showAddLevelDbKeyDialog = true) }
    }

    fun dismissAddLevelDbKeyDialog() {
        _uiState.update { it.copy(showAddLevelDbKeyDialog = false) }
    }

    fun addLevelDbKey(key: String, initialValue: String): Boolean {
        val helper = levelDbHelper ?: return false
        if (key.isEmpty()) return false
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        val valBytes = initialValue.toByteArray(Charsets.UTF_8)
        helper.put(keyBytes, valBytes)
        syncLevelDbToStorage()
        reloadLevelDbRecords()
        _uiState.update { it.copy(isDirty = true, showAddLevelDbKeyDialog = false) }
        return true
    }

    fun deleteLevelDbSingleKey(record: LevelDbRecord) {
        val helper = levelDbHelper ?: return
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                helper.delete(record.key)
                syncLevelDbToStorage()
                reloadLevelDbRecords()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(userMessage = context.getString(R.string.nbt_leveldb_delete_key_success, record.displayName))
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(userMessage = t.message) }
                }
            }
        }
    }

    fun deleteLevelDbChunk(chunkX: Int, chunkZ: Int, dimensionId: Int?) {
        val helper = levelDbHelper ?: return
        val context = getApplication<Application>()
        _uiState.update {
            it.copy(
                isLoading = true,
                loadingMessage = context.getString(R.string.nbt_loading_delete_chunks)
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deleted = helper.deleteChunk(chunkX, chunkZ, dimensionId)
                syncLevelDbToStorage()
                reloadLevelDbRecords()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = context.getString(R.string.nbt_leveldb_delete_chunk_success, chunkX, chunkZ, deleted)
                        )
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = t.message
                        )
                    }
                }
            }
        }
    }

    fun openLevelDb2DVisualizer() {
        val helper = levelDbHelper ?: return
        val context = getApplication<Application>()
        _uiState.update {
            it.copy(
                isLoading = true,
                loadingMessage = context.getString(R.string.nbt_loading_scan_chunks)
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val chunkSummary = helper.getChunkCoordinateSummary(0)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            mode = NbtEditorMode.CHUNK_GRID,
                            title = "LevelDB 2D Map",
                            subtitle = "Overworld",
                            isLoading = false,
                            isWorldChunkMode = true,
                            populatedWorldChunks = chunkSummary.populated,
                            blockEntityWorldChunks = chunkSummary.blockEntities,
                            currentDimensionId = 0,
                            selectedChunkSet = emptySet(),
                            selectedChunkLocalX = -1,
                            selectedChunkLocalZ = -1
                        )
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = t.message ?: "Failed to scan chunks"
                        )
                    }
                }
            }
        }
    }

    fun loadLevelDbDimension(dimId: Int) {
        val helper = levelDbHelper ?: return
        val context = getApplication<Application>()
        _uiState.update {
            it.copy(
                isLoading = true,
                loadingMessage = context.getString(R.string.nbt_loading_scan_chunks)
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val chunkSummary = helper.getChunkCoordinateSummary(dimId)
                val dimName = when (dimId) {
                    1 -> "Nether"
                    2 -> "The End"
                    else -> "Overworld"
                }
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            subtitle = dimName,
                            isLoading = false,
                            populatedWorldChunks = chunkSummary.populated,
                            blockEntityWorldChunks = chunkSummary.blockEntities,
                            currentDimensionId = dimId,
                            selectedChunkSet = emptySet(),
                            selectedChunkLocalX = -1,
                            selectedChunkLocalZ = -1
                        )
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = t.message ?: "Failed to load dimension"
                        )
                    }
                }
            }
        }
    }

    fun requestLevelDbPruneDialog() {
        _uiState.update { it.copy(showLevelDbPruneDialog = true) }
    }

    fun dismissLevelDbPruneDialog() {
        _uiState.update { it.copy(showLevelDbPruneDialog = false) }
    }

    fun executeLevelDbPrune(strategy: Int) {
        val helper = levelDbHelper ?: return
        val context = getApplication<Application>()
        _uiState.update {
            it.copy(
                showLevelDbPruneDialog = false,
                isLoading = true,
                loadingMessage = context.getString(R.string.nbt_loading_prune_chunks)
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deleted = when (strategy) {
                    1 -> helper.deleteChunksOutsideRange(-16, 16, -16, 16)
                    2 -> helper.deleteChunksOutsideRange(-32, 32, -32, 32)
                    3 -> helper.deleteChunksOutsideRange(-64, 64, -64, 64)
                    4 -> {
                        val chunks = allLevelDbRecords.filter { it.chunkX != null && it.chunkZ != null }
                            .map { Pair(it.chunkX!!, it.chunkZ!!) }.distinct()
                        helper.deleteChunks(chunks)
                    }
                    else -> 0
                }
                syncLevelDbToStorage()
                reloadLevelDbRecords()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = context.getString(R.string.nbt_leveldb_prune_success, deleted)
                        )
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = t.message
                        )
                    }
                }
            }
        }
    }

    fun toggleChunkBoxSelectMode() {
        _uiState.update { it.copy(chunkBoxSelectMode = !it.chunkBoxSelectMode) }
    }

    fun selectChunk(x: Int, z: Int) {
        _uiState.update {
            it.copy(
                selectedChunkLocalX = x,
                selectedChunkLocalZ = z
            )
        }
    }

    fun toggleChunkInSelection(x: Int, z: Int) {
        val set = _uiState.value.selectedChunkSet.toMutableSet()
        val pair = Pair(x, z)
        if (set.contains(pair)) {
            set.remove(pair)
        } else {
            set.add(pair)
        }
        _uiState.update {
            it.copy(
                selectedChunkSet = set,
                selectedChunkLocalX = x,
                selectedChunkLocalZ = z
            )
        }
    }

    fun selectAllPopulatedChunks() {
        val state = _uiState.value
        val targets = if (state.isWorldChunkMode) {
            state.populatedWorldChunks
        } else {
            state.chunkTable.filter { it.exists }.map { Pair(it.localX, it.localZ) }.toSet()
        }
        _uiState.update { it.copy(selectedChunkSet = targets) }
    }

    fun invertChunkSelection() {
        val state = _uiState.value
        val allChunks = if (state.isWorldChunkMode) {
            state.populatedWorldChunks
        } else {
            state.chunkTable.filter { it.exists }.map { Pair(it.localX, it.localZ) }.toSet()
        }
        val inverted = allChunks - state.selectedChunkSet
        _uiState.update { it.copy(selectedChunkSet = inverted) }
    }

    fun clearChunkSelection() {
        _uiState.update { it.copy(selectedChunkSet = emptySet()) }
    }

    fun deleteSelectedChunks() {
        val state = _uiState.value
        val context = getApplication<Application>()
        if (state.isWorldChunkMode) {
            val helper = levelDbHelper ?: return
            val targets = if (state.selectedChunkSet.isNotEmpty()) {
                state.selectedChunkSet
            } else if (state.selectedChunkLocalX != -1 && state.selectedChunkLocalZ != -1) {
                setOf(Pair(state.selectedChunkLocalX, state.selectedChunkLocalZ))
            } else emptySet()

            if (targets.isEmpty()) return

            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = context.getString(R.string.nbt_loading_delete_chunks)
                )
            }
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val deleted = helper.deleteChunks(targets, state.currentDimensionId)
                    syncLevelDbToStorage()
                    val chunkSummary = helper.getChunkCoordinateSummary(state.currentDimensionId)
                    reloadLevelDbRecords()
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                populatedWorldChunks = chunkSummary.populated,
                                blockEntityWorldChunks = chunkSummary.blockEntities,
                                selectedChunkSet = emptySet(),
                                userMessage = context.getString(R.string.nbt_delete_chunks_batch_success, targets.size)
                            )
                        }
                    }
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(isLoading = false, userMessage = t.message)
                        }
                    }
                }
            }
        } else {
            val region = state.currentRegionFile ?: return
            val targets = if (state.selectedChunkSet.isNotEmpty()) {
                state.selectedChunkSet
            } else if (state.selectedChunkLocalX != -1 && state.selectedChunkLocalZ != -1) {
                setOf(Pair(state.selectedChunkLocalX, state.selectedChunkLocalZ))
            } else emptySet()

            if (targets.isEmpty()) return

            try {
                region.deleteChunks(targets)
                syncRegionFileToStorage()
                val table = region.getChunkTable()
                _uiState.update {
                    it.copy(
                        chunkTable = table,
                        selectedChunkSet = emptySet(),
                        userMessage = context.getString(R.string.nbt_delete_chunks_batch_success, targets.size)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = e.message) }
            }
        }
    }

    fun requestPruneUselessDialog() {
        _uiState.update { it.copy(showPruneUselessDialog = true) }
    }

    fun dismissPruneUselessDialog() {
        _uiState.update { it.copy(showPruneUselessDialog = false) }
    }

    fun executePruneUselessChunks(maxInhabitedTime: Long) {
        val state = _uiState.value
        val region = state.currentRegionFile ?: return
        val context = getApplication<Application>()
        _uiState.update {
            it.copy(
                showPruneUselessDialog = false,
                isLoading = true,
                loadingMessage = context.getString(R.string.nbt_loading_prune_chunks)
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val useless = region.scanUselessChunks(maxInhabitedTimeTicks = maxInhabitedTime)
                val coords = useless.map { Pair(it.first.localX, it.first.localZ) }
                if (coords.isNotEmpty()) {
                    region.deleteChunks(coords)
                    syncRegionFileToStorage()
                }
                val table = region.getChunkTable()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            chunkTable = table,
                            selectedChunkSet = emptySet(),
                            userMessage = context.getString(R.string.nbt_prune_success, coords.size)
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(isLoading = false, userMessage = e.message)
                    }
                }
            }
        }
    }

    fun saveCurrent(silent: Boolean = false) {
        val state = _uiState.value
        val context = getApplication<Application>()

        val levelDb = levelDbHelper
        val record = state.currentLevelDbRecord
        if (levelDb != null && record != null && state.currentNbtFile != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val bytes = BedrockLevelDbHelper.writeBedrockNbt(state.currentNbtFile, record.hasMultipleCompounds)
                    levelDb.put(record.key, bytes)
                    syncLevelDbToStorage()
                    reloadLevelDbRecords()
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isDirty = false,
                                userMessage = if (!silent) context.getString(R.string.nbt_leveldb_save_success, record.displayName) else null
                            )
                        }
                    }
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(userMessage = "${context.getString(R.string.nbt_save_failed, t.message)}")
                        }
                    }
                }
            }
            return
        }

        val region = state.currentRegionFile
        if (region != null && state.currentNbtFile != null) {
            try {
                region.writeChunkNbt(state.currentChunkX, state.currentChunkZ, state.currentNbtFile)
                syncRegionFileToStorage()
                _uiState.update {
                    it.copy(
                        isDirty = false,
                        userMessage = if (!silent) context.getString(R.string.nbt_save_success) else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "${context.getString(R.string.nbt_save_failed, e.message)}")
                }
            }
            return
        }

        val zipBytes = state.originalZipBytes
        val zipEntry = state.selectedZipEntryName
        if (zipBytes != null && zipEntry != null && state.currentNbtFile != null) {
            try {
                val updatedNbt = serializeCurrentNbt()
                val entries = state.workspaceZipEntries.toMutableMap()
                entries[zipEntry] = updatedNbt
                val updatedZip = updateZipWithNbt(zipBytes, zipEntry, updatedNbt)

                saveWorkspaceToCache(updatedZip, state.activeWorkspaceName)

                if (state.currentFilePath != null) {
                    val file = File(state.currentFilePath)
                    val bak = File("${state.currentFilePath}.bak")
                    if (file.exists()) file.copyTo(bak, overwrite = true)
                    atomicWriteFile(file, updatedZip)
                } else if (state.currentFileUri != null) {
                    context.contentResolver.openOutputStream(state.currentFileUri)?.use { it.write(updatedZip) }
                }

                _uiState.update {
                    it.copy(
                        originalZipBytes = updatedZip,
                        workspaceZipEntries = entries,
                        isDirty = false,
                        userMessage = if (!silent) context.getString(R.string.nbt_zip_save_success, zipEntry) else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "${context.getString(R.string.nbt_save_failed, e.message)}")
                }
            }
            return
        }

        val path = state.currentFilePath
        val nbtFile = state.currentNbtFile
        if (path != null && nbtFile != null) {
            try {
                val file = File(path)
                val bak = File("$path.bak")
                if (file.exists()) file.copyTo(bak, overwrite = true)

                val bytes = ByteArrayOutputStream().also { output ->
                    if (!state.readHeaders) {
                        NbtIO.writeNbtFile(output, nbtFile, state.isCompressed, state.isLittleEndian)
                    } else {
                        output.write(serializeCurrentNbt())
                    }
                }.toByteArray()
                atomicWriteFile(file, bytes)
                _uiState.update {
                    it.copy(
                        isDirty = false,
                        userMessage = if (!silent) context.getString(R.string.nbt_save_success) else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "${context.getString(R.string.nbt_save_failed, e.message)}")
                }
            }
        } else if (state.currentFileUri != null) {
            saveToUri(state.currentFileUri, silent)
        }
    }

    fun saveAs(uri: Uri) {
        saveToUri(uri, silent = false)
    }

    private fun saveToUri(uri: Uri, silent: Boolean) {
        val state = _uiState.value
        val context = getApplication<Application>()
        val nbtFile = state.currentNbtFile ?: return

        try {
            val zipBytes = state.originalZipBytes
            val zipEntry = state.selectedZipEntryName
            if (zipBytes != null && zipEntry != null) {
                val updatedNbt = serializeCurrentNbt()
                val entries = state.workspaceZipEntries.toMutableMap()
                entries[zipEntry] = updatedNbt
                val updatedZip = updateZipWithNbt(zipBytes, zipEntry, updatedNbt)

                val output = context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("Unable to open output stream")
                output.use { it.write(updatedZip) }
                _uiState.update {
                    it.copy(
                        originalZipBytes = updatedZip,
                        workspaceZipEntries = entries,
                        isDirty = false,
                        userMessage = if (!silent) context.getString(R.string.nbt_save_success) else null
                    )
                }
                return
            }

            val output = context.contentResolver.openOutputStream(uri)
                ?: throw IOException("Unable to open output stream")
            output.use { stream ->
                if (!state.readHeaders) {
                    NbtIO.writeNbtFile(stream, nbtFile, state.isCompressed, state.isLittleEndian)
                } else {
                    val nbtBytes = serializeCurrentNbt()
                    stream.write(nbtBytes)
                }
            }
            _uiState.update {
                it.copy(
                    isDirty = false,
                    userMessage = if (!silent) context.getString(R.string.nbt_save_success) else null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(userMessage = "${context.getString(R.string.nbt_save_failed, e.message)}")
            }
        }
    }

    private fun serializeCurrentNbt(): ByteArray {
        val state = _uiState.value
        val nbtFile = state.currentNbtFile ?: return byteArrayOf()
        val baos = ByteArrayOutputStream()
        if (!state.readHeaders) {
            NbtIO.writeNbtFile(baos, nbtFile, state.isCompressed, state.isLittleEndian)
        } else {
            val nbtBaos = ByteArrayOutputStream()
            NbtIO.writeNbtFile(nbtBaos, nbtFile, state.isCompressed, state.isLittleEndian)
            val nbtBytes = nbtBaos.toByteArray()
            val finalStream = LittleEndianDataOutputStream(baos)
            finalStream.writeInt(state.headerVersion ?: 0)
            finalStream.writeInt(nbtBytes.size)
            finalStream.write(nbtBytes)
            finalStream.flush()
        }
        return baos.toByteArray()
    }

    private fun updateZipWithNbt(originalZip: ByteArray, entryNameToReplace: String, newNbtBytes: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipInputStream(ByteArrayInputStream(originalZip)).use { zis ->
            ZipOutputStream(baos).use { zos ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newEntry = ZipEntry(entry.name)
                    zos.putNextEntry(newEntry)
                    if (entry.name == entryNameToReplace) {
                        zos.write(newNbtBytes)
                    } else {
                        zis.copyTo(zos)
                    }
                    zos.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        return baos.toByteArray()
    }

    private fun atomicWriteFile(file: File, bytes: ByteArray) {
        val temp = File(file.parentFile, ".${file.name}.tmp")
        temp.outputStream().use { it.write(bytes) }
        if (!temp.renameTo(file)) {
            temp.delete()
            throw IOException("Unable to commit file")
        }
    }

    private fun updateZipWithLevelDbFolder(originalZip: ByteArray, levelDbFolder: File): ByteArray {
        val baos = ByteArrayOutputStream()
        val writtenEntries = mutableSetOf<String>()

        ZipInputStream(ByteArrayInputStream(originalZip)).use { zis ->
            ZipOutputStream(baos).use { zos ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.startsWith("db/") || name.startsWith("db\\")) {
                        val relName = name.substringAfter("db/").substringAfter("db\\")
                        val file = File(levelDbFolder, relName)
                        if (file.exists() && !file.isDirectory) {
                            val newEntry = ZipEntry("db/$relName")
                            zos.putNextEntry(newEntry)
                            file.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                            writtenEntries.add(relName)
                        }
                    } else {
                        val newEntry = ZipEntry(name)
                        zos.putNextEntry(newEntry)
                        zis.copyTo(zos)
                        zos.closeEntry()
                    }
                    entry = zis.nextEntry
                }

                levelDbFolder.listFiles()?.forEach { f ->
                    if (!f.isDirectory && !writtenEntries.contains(f.name) && f.name != "LOCK") {
                        val newEntry = ZipEntry("db/${f.name}")
                        zos.putNextEntry(newEntry)
                        f.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }
        return baos.toByteArray()
    }

    private fun syncLevelDbToStorage() {
        val state = _uiState.value
        val zipBytes = state.originalZipBytes
        val folder = levelDbCachedFolder
        val context = getApplication<Application>()

        if (zipBytes != null && folder != null) {
            val updatedZip = updateZipWithLevelDbFolder(zipBytes, folder)
            saveWorkspaceToCache(updatedZip, state.activeWorkspaceName)
            _uiState.update { it.copy(originalZipBytes = updatedZip) }

            if (state.currentFilePath != null) {
                val file = File(state.currentFilePath)
                val bak = File("${state.currentFilePath}.bak")
                if (file.exists()) file.copyTo(bak, overwrite = true)
                atomicWriteFile(file, updatedZip)
            } else if (state.currentFileUri != null) {
                context.contentResolver.openOutputStream(state.currentFileUri)?.use { it.write(updatedZip) }
            }
        } else if (state.currentFilePath != null && folder != null) {
            val originalFolder = File(state.currentFilePath)
            if (originalFolder.isDirectory) {
                folder.listFiles()?.forEach { f ->
                    if (!f.isDirectory && f.name != "LOCK") {
                        f.copyTo(File(originalFolder, f.name), overwrite = true)
                    }
                }
            }
        }
    }

    private fun syncRegionFileToStorage() {
        val state = _uiState.value
        val zipBytes = state.originalZipBytes
        val zipEntry = state.selectedZipEntryName
        val regionFile = state.currentRegionCachedFile
        val context = getApplication<Application>()

        if (zipBytes != null && zipEntry != null && regionFile != null) {
            val regionBytes = regionFile.readBytes()
            val entries = state.workspaceZipEntries.toMutableMap()
            entries[zipEntry] = regionBytes
            val updatedZip = updateZipWithNbt(zipBytes, zipEntry, regionBytes)
            saveWorkspaceToCache(updatedZip, state.activeWorkspaceName)
            _uiState.update {
                it.copy(
                    originalZipBytes = updatedZip,
                    workspaceZipEntries = entries
                )
            }

            if (state.currentFilePath != null) {
                val file = File(state.currentFilePath)
                val bak = File("${state.currentFilePath}.bak")
                if (file.exists()) file.copyTo(bak, overwrite = true)
                atomicWriteFile(file, updatedZip)
            } else if (state.currentFileUri != null) {
                context.contentResolver.openOutputStream(state.currentFileUri)?.use { it.write(updatedZip) }
            }
        } else if (regionFile != null) {
            if (state.currentFilePath != null) {
                val file = File(state.currentFilePath)
                val bak = File("${state.currentFilePath}.bak")
                if (file.exists()) file.copyTo(bak, overwrite = true)
                regionFile.copyTo(file, overwrite = true)
            } else if (state.currentFileUri != null) {
                context.contentResolver.openOutputStream(state.currentFileUri)?.use { out ->
                    regionFile.inputStream().use { it.copyTo(out) }
                }
            }
        }
    }

    private fun reloadLevelDbRecords() {
        val helper = levelDbHelper ?: return
        val records = try {
            helper.getAllRecords()
        } catch (_: Throwable) {
            emptyList()
        }
        allLevelDbRecords = records
        _uiState.update { it.copy(levelDbRecords = records) }
        applyLevelDbFilter()
    }

    private fun isZipArchive(bytes: ByteArray): Boolean {
        return bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            (bytes[2] == 0x03.toByte() || bytes[2] == 0x05.toByte() || bytes[2] == 0x07.toByte())
    }

    private fun saveWorkspaceToCache(bytes: ByteArray, name: String) {
        val context = getApplication<Application>()
        try {
            val cacheFile = File(context.cacheDir, "nbt_cached_workspace.zip")
            cacheFile.writeBytes(bytes)
            prefs.edit().putString("nbt_last_workspace_name", name).apply()
        } catch (_: Exception) {}
    }

    private fun restoreWorkspaceSession() {
        val context = getApplication<Application>()
        val cacheFile = File(context.cacheDir, "nbt_cached_workspace.zip")
        val savedName = prefs.getString("nbt_last_workspace_name", null)

        if (cacheFile.exists() && savedName != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val bytes = cacheFile.readBytes()
                    if (isZipArchive(bytes)) {
                        handleZipArchive(bytes, savedName)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun closeWorkspace() {
        val context = getApplication<Application>()
        val cacheFile = File(context.cacheDir, "nbt_cached_workspace.zip")
        if (cacheFile.exists()) cacheFile.delete()
        prefs.edit().remove("nbt_last_workspace_name").apply()

        clearActiveWorkspace()
        _uiState.update {
            NbtEditorUiState(autoSave = it.autoSave)
        }
    }

    private fun clearActiveWorkspace() {
        levelDbHelper?.close()
        levelDbHelper = null
        levelDbCachedFolder = null
        allLevelDbRecords = emptyList()
        _uiState.value.currentRegionFile?.close()
    }

    fun handleBack(): Boolean {
        val state = _uiState.value
        if (state.mode == NbtEditorMode.NBT_TREE && (state.hasLevelDb || state.workspaceZipEntries.isNotEmpty() || state.currentRegionFile != null)) {
            if (state.currentRegionFile != null) {
                _uiState.update {
                    it.copy(
                        mode = NbtEditorMode.CHUNK_GRID,
                        title = it.currentRegionFile?.file?.name ?: "MCA",
                        subtitle = getApplication<Application>().getString(R.string.nbt_workspace_anvil)
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        mode = NbtEditorMode.WORKSPACE,
                        title = it.activeWorkspaceName,
                        subtitle = if (it.hasLevelDb) getApplication<Application>().getString(R.string.nbt_workspace_leveldb) else getApplication<Application>().getString(R.string.nbt_workspace_file_count, it.workspaceZipEntries.size)
                    )
                }
            }
            return true
        }

        if (state.mode == NbtEditorMode.CHUNK_GRID && state.workspaceZipEntries.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    mode = NbtEditorMode.WORKSPACE,
                    title = it.activeWorkspaceName,
                    subtitle = if (it.hasLevelDb) getApplication<Application>().getString(R.string.nbt_workspace_leveldb) else getApplication<Application>().getString(R.string.nbt_workspace_file_count, it.workspaceZipEntries.size)
                )
            }
            return true
        }

        if (state.isDirty && state.currentNbtFile != null) {
            if (state.autoSave) {
                saveCurrent(silent = true)
                return false
            } else {
                _uiState.update { it.copy(showUnsavedChangesDialog = true) }
                return true
            }
        }

        return false
    }

    fun dismissUnsavedChangesDialog() {
        _uiState.update { it.copy(showUnsavedChangesDialog = false) }
    }

    override fun onCleared() {
        super.onCleared()
        clearActiveWorkspace()
    }
}
