package com.noches.chunkoidng.ui.screens.history

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.noches.chunkoidng.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noches.chunkoidng.core.world.ArchiveManager
import com.noches.chunkoidng.core.world.ConversionHistoryRecord
import com.noches.chunkoidng.core.world.HistoryManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionHistoryScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val historyManager = remember { HistoryManager(context) }
    val archiveManager = remember { ArchiveManager(context) }
    var records by remember { mutableStateOf<List<ConversionHistoryRecord>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var currentExportingRecord by remember { mutableStateOf<ConversionHistoryRecord?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    LaunchedEffect(historyManager) {
        records = withContext(Dispatchers.IO) { historyManager.getRecords() }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val record = currentExportingRecord ?: return@let
            isExporting = true
            scope.launch {
                val isBedrock = record.targetPlatform.contains("Bedrock", ignoreCase = true)
                val result = archiveManager.exportToUri(it, record.worldName, packAsArchive = true, isBedrock = isBedrock)
                isExporting = false
                result.onSuccess { _ ->
                    historyManager.updateExportLocation(record.id, it.toString())
                    records = withContext(Dispatchers.IO) { historyManager.getRecords() }
                    Toast.makeText(context, context.getString(R.string.history_export_success), Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, context.getString(R.string.history_export_fail, err.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.history_back_desc))
                    }
                },
                actions = {
                    if (records.isNotEmpty()) {
                        IconButton(onClick = {
                            historyManager.clearHistory()
                            records = emptyList()
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.history_clear_desc))
                        }
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { innerPadding ->
        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(records) { record ->
                    HistoryItemCard(
                        record = record,
                        isExporting = isExporting && currentExportingRecord?.id == record.id,
                        onRemedyExport = {
                            currentExportingRecord = record
                            exportLauncher.launch(null)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryItemCard(
    record: ConversionHistoryRecord,
    isExporting: Boolean,
    onRemedyExport: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        onClick = { showDialog = true }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val bitmap = remember(record.iconPath) {
                if (record.iconPath != null) BitmapFactory.decodeFile(record.iconPath) else null
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Public, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(record.worldName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${record.sourcePlatform} → ${record.targetPlatform}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
                Text(
                    text = stringResource(R.string.history_item_time, sdf.format(Date(record.timestamp)), record.durationMs / 1000),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.history_detail_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.history_detail_name, record.worldName))
                    Text(stringResource(R.string.history_detail_source, record.sourcePlatform))
                    Text(stringResource(R.string.history_detail_target, record.targetPlatform))
                    Text(stringResource(R.string.history_detail_duration, record.durationMs / 1000.0))
                    if (record.exportedUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.history_detail_export_uri_title), fontWeight = FontWeight.Bold)
                        Text(record.exportedUri, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.history_detail_not_exported), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.history_close))
                }
            },
            dismissButton = {
                if (record.exportedUri == null) {
                    TextButton(onClick = {
                        showDialog = false
                        onRemedyExport()
                    }, enabled = !isExporting) {
                        Text(stringResource(if (isExporting) R.string.history_exporting else R.string.history_export_fallback))
                    }
                }
            }
        )
    }
}

