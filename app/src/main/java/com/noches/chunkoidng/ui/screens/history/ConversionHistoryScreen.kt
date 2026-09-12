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
                    Toast.makeText(context, "导出补救成功！", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, "导出失败: ${err.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("转换记录", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (records.isNotEmpty()) {
                        IconButton(onClick = { 
                            historyManager.clearHistory()
                            records = emptyList()
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "清空记录")
                        }
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { innerPadding ->
        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("暂无转换记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    text = "时间: ${sdf.format(Date(record.timestamp))}  耗时: ${record.durationMs / 1000}s",
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
            title = { Text("存档详细信息", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("名称: ${record.worldName}")
                    Text("来源: ${record.sourcePlatform}")
                    Text("目标: ${record.targetPlatform}")
                    Text("耗时: ${record.durationMs / 1000.0} 秒")
                    if (record.exportedUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("导出位置 (URI):", fontWeight = FontWeight.Bold)
                        Text(record.exportedUri, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("导出位置: 暂未导出", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("关闭")
                }
            },
            dismissButton = {
                if (record.exportedUri == null) {
                    TextButton(onClick = {
                        showDialog = false
                        onRemedyExport()
                    }, enabled = !isExporting) {
                        Text(if (isExporting) "正在导出..." else "补救导出压缩包")
                    }
                }
            }
        )
    }
}

