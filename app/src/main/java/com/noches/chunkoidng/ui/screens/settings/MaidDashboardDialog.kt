package com.noches.chunkoidng.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.noches.chunkoidng.R
import com.noches.chunkoidng.maid.Maid
import com.noches.chunkoidng.maid.model.ViolationSeverity
import com.noches.chunkoidng.ui.theme.ChipBadgeShape
import com.noches.chunkoidng.ui.theme.ExpressiveShapes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaidDashboardDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val report = remember { Maid.audit.createReport() }
    val violations by Maid.securityAuditor.violationsFlow.collectAsState()

    val metrics = com.noches.chunkoidng.ui.theme.rememberAdaptiveMetrics()
    val dialogPadding = if (metrics.widthTier == com.noches.chunkoidng.ui.theme.WindowWidthTier.COMPACT_NARROW) 14.dp else 20.dp

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(metrics.dialogWidthFraction)
                .widthIn(max = metrics.dialogMaxWidth)
                .fillMaxHeight(metrics.dialogHeightFraction)
                .heightIn(max = metrics.dialogMaxHeight),
            shape = ExpressiveShapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dialogPadding)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(ChipBadgeShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.maid_dashboard_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.maid_dashboard_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Outlined.Close, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.maid_tab_overview), fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.maid_tab_quotas), fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.maid_tab_audit), fontSize = 13.sp)
                                if (violations.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ) {
                                        Text("${violations.size}")
                                    }
                                }
                            }
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp)

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> DashboardOverviewTab(report)
                        1 -> DashboardQuotasTab(report)
                        2 -> DashboardAuditTab(violations = violations, onClear = { Maid.securityAuditor.clear() })
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val text = buildString {
                                appendLine(context.getString(R.string.maid_report_header))
                                appendLine(context.getString(R.string.maid_report_device, report.deviceModel, context.getString(report.deviceTier.titleRes)))
                                appendLine(context.getString(R.string.maid_report_cpu, report.cpuCores, report.cpuMaxFreqMhz, report.computeScore))
                                appendLine(context.getString(R.string.maid_report_ram, report.totalRamMb, report.freeRamMb))
                                appendLine(context.getString(R.string.maid_report_heap, report.currentHeapUsedMb, report.currentHeapMaxMb))
                                appendLine(context.getString(R.string.maid_report_storage, report.freeStorageMb))
                                appendLine(context.getString(R.string.maid_report_limits, report.activeLimits.jvmMaxMemoryMb, report.activeLimits.parallelism))
                                appendLine(context.getString(R.string.maid_report_events, violations.size))
                                violations.forEach { v ->
                                    appendLine(context.getString(R.string.maid_report_event_line, v.severity.name, v.module, v.type.name, v.message, v.pathOrTarget ?: "N/A"))
                                }
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("MAID™ Report", text))
                            Toast.makeText(context, context.getString(R.string.maid_copied_toast), Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.maid_action_copy_report), fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardOverviewTab(report: com.noches.chunkoidng.maid.model.MaidAuditReport) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MAID™ (Managed Architecture for Integrity & Defense)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.maid_dashboard_acronym_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.maid_tier_label),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        SuggestionChip(
                            onClick = {},
                            label = { Text(stringResource(report.deviceTier.titleRes), fontWeight = FontWeight.Bold) }
                        )
                    }
                    Text(
                        text = stringResource(R.string.maid_overview_device_model, report.deviceModel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.maid_overview_cpu_specs, report.cpuCores, report.cpuMaxFreqMhz, report.computeScore),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.maid_overview_memory_section),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    val heapProgress = if (report.currentHeapMaxMb > 0) {
                        (report.currentHeapUsedMb.toFloat() / report.currentHeapMaxMb.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    LinearProgressIndicator(
                        progress = { heapProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.maid_overview_art_heap, report.currentHeapUsedMb, report.currentHeapMaxMb),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(heapProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                    Text(
                        text = stringResource(R.string.maid_overview_physical_ram, report.totalRamMb, report.freeRamMb),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.maid_overview_free_storage, report.freeStorageMb),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardQuotasTab(report: com.noches.chunkoidng.maid.model.MaidAuditReport) {
    val limits = report.activeLimits
    val items = listOf(
        stringResource(R.string.maid_quota_jvm_heap) to "${limits.jvmMaxMemoryMb} MB",
        stringResource(R.string.maid_quota_parallelism) to stringResource(R.string.maid_quota_parallelism_val, limits.parallelism),
        stringResource(R.string.maid_quota_gc_strategy) to if (limits.useSerialGc) stringResource(R.string.maid_quota_gc_serial) else stringResource(R.string.maid_quota_gc_parallel),
        stringResource(R.string.maid_quota_archive_entry) to "${limits.archiveMaxEntryBytes / (1024 * 1024)} MB",
        stringResource(R.string.maid_quota_archive_total) to "${limits.archiveMaxTotalBytes / (1024 * 1024)} MB",
        stringResource(R.string.maid_quota_archive_count) to stringResource(R.string.maid_quota_archive_count_val, limits.archiveMaxEntries),
        stringResource(R.string.maid_quota_nbt_page) to stringResource(R.string.maid_quota_nbt_page_val, limits.nbtArrayPageSize),
        stringResource(R.string.maid_quota_pack_frames) to stringResource(R.string.maid_quota_pack_frames_val, limits.packMaxFrames),
        stringResource(R.string.maid_quota_midi_notes) to stringResource(R.string.maid_quota_midi_notes_val, limits.midiMaxNotes),
        stringResource(R.string.maid_quota_midi_size) to "${limits.midiMaxFileBytes / (1024 * 1024)} MB"
    )
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {

        items(items) { (k, v) ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = k,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(end = 10.dp)
                    )
                    Text(text = v, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun DashboardAuditTab(
    violations: List<com.noches.chunkoidng.maid.model.SecurityViolation>,
    onClear: () -> Unit
) {
    if (violations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.maid_audit_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.maid_audit_recent_count, violations.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.maid_action_clear_audit), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                items(violations.reversed()) { v ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (v.severity == ViolationSeverity.BLOCKED)
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val severityColor = if (v.severity == ViolationSeverity.BLOCKED)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.tertiary
                                    val severityText = if (v.severity == ViolationSeverity.BLOCKED)
                                        stringResource(R.string.maid_severity_blocked)
                                    else
                                        stringResource(R.string.maid_severity_truncated)

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = severityColor
                                    ) {
                                        Text(
                                            text = severityText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = v.module,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = dateFormat.format(Date(v.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = v.message,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (v.pathOrTarget != null) {
                                Text(
                                    text = stringResource(R.string.maid_audit_target, v.pathOrTarget),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (v.requestedValue != null && v.clampedValue != null) {
                                Text(
                                    text = stringResource(R.string.maid_audit_clamped, v.requestedValue, v.clampedValue),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
