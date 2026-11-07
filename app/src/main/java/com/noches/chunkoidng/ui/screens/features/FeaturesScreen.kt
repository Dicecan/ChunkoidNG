package com.noches.chunkoidng.ui.screens.features

import com.noches.chunkoidng.R
import android.widget.Toast
import androidx.compose.ui.res.stringResource

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noches.chunkoidng.ui.theme.ChipBadgeShape
import com.noches.chunkoidng.ui.theme.ExpressiveShapes
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.outlined.CloudDownload
import com.noches.chunkoidng.core.chunker.ChunkerUpdateManager
import com.noches.chunkoidng.core.chunker.ChunkerUpdateStatus
import com.noches.chunkoidng.core.chunker.ChunkerReleaseInfo
import com.noches.chunkoidng.ui.theme.PillShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_SELF_CHECK_LOG_LINES = 200

@Composable
fun FeaturesScreen(
    onFeatureClick: (FeatureItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp
    ) {

        item(span = StaggeredGridItemSpan.FullLine) {
            HeroStatusBanner()
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.feature_screen_core_tools),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(ChipBadgeShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.feature_screen_tool_count, chunkoidFeatures.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.feature_screen_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(chunkoidFeatures, key = { it.id }, span = { it.span }) { feature ->
            FeatureCard(
                feature = feature,
                onClick = { onFeatureClick(feature) }
            )
        }
    }
}

@Composable
private fun HeroStatusBanner() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val runtimeEnv = remember { com.noches.chunkoidng.core.runtime.JavaRuntimeEnvironment(context) }
    val processManager = remember { com.noches.chunkoidng.core.runtime.JavaProcessManager(context, runtimeEnv) }

    var isChecking by remember { mutableStateOf(false) }
    var rootfsReady by remember { mutableStateOf(runtimeEnv.isRootfsReady()) }
    var extractionProgress by remember { mutableStateOf(0) }
    var terminalLogs by remember { mutableStateOf(listOf<String>()) }
    var showTerminal by remember { mutableStateOf(false) }

    val chunkerUpdateStatus by ChunkerUpdateManager.statusFlow.collectAsState()
    var showChunkerUpdateDialog by remember { mutableStateOf(false) }
    val currentChunkerVer = remember(chunkerUpdateStatus) { ChunkerUpdateManager.getCurrentVersion(context) }

    LaunchedEffect(Unit) {
        ChunkerUpdateManager.checkSilently(context)
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(terminalLogs.size) {
        if (terminalLogs.isNotEmpty() && showTerminal) {
            listState.animateScrollToItem(terminalLogs.size - 1)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(400, easing = FastOutSlowInEasing)),
        shape = ExpressiveShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {

            Md3ExpressiveClock()

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 0.85f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_scale"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(if (rootfsReady) Color(0xFF10B981) else MaterialTheme.colorScheme.error)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.feature_screen_status_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                            .clickable(enabled = !isChecking) {
                                isChecking = true
                                showTerminal = true
                                terminalLogs = listOf(context.getString(R.string.cli_log_init))

                                coroutineScope.launch {
                                    if (!runtimeEnv.isRootfsReady()) {
                                        terminalLogs = terminalLogs + context.getString(R.string.cli_log_release)
                                        runtimeEnv.extractRootFS().collect { progress ->
                                            extractionProgress = progress
                                            if (progress % 20 == 0 || progress == 100) {
                                                terminalLogs = terminalLogs + context.getString(R.string.cli_log_progress, progress)
                                            }
                                        }
                                        rootfsReady = runtimeEnv.isRootfsReady()
                                    }

                                    if (rootfsReady) {
                                        terminalLogs = terminalLogs + context.getString(R.string.cli_log_ready)
                                        processManager.executeShellCommand("java -version").collect { logLine ->
                                             terminalLogs = (terminalLogs + logLine).takeLast(MAX_SELF_CHECK_LOG_LINES)
                                        }
                                        val cliJar = File(context.filesDir, "cli.jar")
                                        if (!cliJar.exists()) {
                                            runtimeEnv.ensureCliJar()
                                        }
                                        if (cliJar.exists() && cliJar.length() > 0) {
                                            terminalLogs = (terminalLogs + "[SYSTEM] cli.jar verified (${cliJar.length() / 1024} KB)").takeLast(MAX_SELF_CHECK_LOG_LINES)
                                        }
                                        terminalLogs = (terminalLogs + context.getString(R.string.chunker_update_checking_log)).takeLast(MAX_SELF_CHECK_LOG_LINES)
                                        ChunkerUpdateManager.checkUpdateFlow(context, force = true).collect { updateStatus ->
                                            when (updateStatus) {
                                                is ChunkerUpdateStatus.UpdateAvailable -> {
                                                    terminalLogs = (terminalLogs + context.getString(R.string.chunker_update_found_log, updateStatus.info.version)).takeLast(MAX_SELF_CHECK_LOG_LINES)
                                                }
                                                is ChunkerUpdateStatus.UpToDate -> {
                                                    terminalLogs = (terminalLogs + context.getString(R.string.chunker_update_latest_log, updateStatus.currentVersion)).takeLast(MAX_SELF_CHECK_LOG_LINES)
                                                }
                                                is ChunkerUpdateStatus.Error -> {
                                                    terminalLogs = (terminalLogs + context.getString(R.string.chunker_update_fail_log, updateStatus.message)).takeLast(MAX_SELF_CHECK_LOG_LINES)
                                                }
                                                else -> {}
                                            }
                                        }
                                        terminalLogs = terminalLogs + context.getString(R.string.cli_log_test_done)

                                        delay(2000)
                                        showTerminal = false
                                    } else {
                                        terminalLogs = terminalLogs + context.getString(R.string.cli_log_err)
                                    }
                                    isChecking = false
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.8.dp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (extractionProgress in 1..99) stringResource(R.string.cli_status_extracting, extractionProgress) else stringResource(R.string.cli_status_checking),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = stringResource(R.string.cli_self_test),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.cli_self_test),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SelfCheckItemChip(
                        icon = Icons.Outlined.Memory,
                        title = "OpenJDK 17",
                        status = if (isChecking && !rootfsReady) stringResource(R.string.cli_status_deploying) else if (rootfsReady) stringResource(R.string.cli_status_ready) else stringResource(R.string.cli_status_not_deploy),
                        isSuccess = rootfsReady,
                        modifier = Modifier.weight(1f)
                    )
                    val isChunkerUpdateAvailable = chunkerUpdateStatus is ChunkerUpdateStatus.UpdateAvailable
                    val chunkerStatusText = when (val s = chunkerUpdateStatus) {
                        is ChunkerUpdateStatus.Checking -> stringResource(R.string.cli_status_checking)
                        is ChunkerUpdateStatus.Downloading -> stringResource(R.string.chunker_update_downloading, (s.progress * 100).toInt(), "${s.downloadedBytes / (1024 * 1024)}MB", "${s.totalBytes / (1024 * 1024)}MB")
                        is ChunkerUpdateStatus.Installing -> stringResource(R.string.chunker_update_installing)
                        is ChunkerUpdateStatus.UpdateAvailable -> stringResource(R.string.chunker_update_available, s.info.version)
                        else -> if (isChecking && rootfsReady) stringResource(R.string.cli_engine_testing) else if (rootfsReady) "${stringResource(R.string.cli_engine_ready)} (v$currentChunkerVer)" else stringResource(R.string.cli_engine_waiting)
                    }

                    SelfCheckItemChip(
                        icon = Icons.Outlined.Terminal,
                        title = stringResource(R.string.cli_engine_title),
                        status = chunkerStatusText,
                        isSuccess = rootfsReady && !isChecking,
                        badgeText = if (isChunkerUpdateAvailable) stringResource(R.string.chunker_update_badge) else null,
                        onClick = { showChunkerUpdateDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (showTerminal) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E1E))
                            .padding(12.dp)
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(terminalLogs.size) { index ->
                                Text(
                                    text = terminalLogs[index],
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = Color(0xFF4AF626)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChunkerUpdateDialog) {
        ChunkerUpdateDialog(
            status = chunkerUpdateStatus,
            currentVersion = currentChunkerVer,
            onDismiss = { showChunkerUpdateDialog = false },
            onCheckUpdate = {
                coroutineScope.launch {
                    ChunkerUpdateManager.checkUpdateFlow(context, force = true).collect { }
                }
            },
            onInstallUpdate = { info ->
                coroutineScope.launch {
                    val success = ChunkerUpdateManager.downloadAndInstall(context, info)
                    if (success) {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.chunker_update_success, info.version),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        showChunkerUpdateDialog = false
                    } else {
                        val currentStatus = ChunkerUpdateManager.statusFlow.value
                        val errorMsg = if (currentStatus is ChunkerUpdateStatus.Error) currentStatus.message else "Update failed"
                        android.widget.Toast.makeText(
                            context,
                            errorMsg,
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun Md3ExpressiveClock() {
    var currentTime by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    val configuration = LocalConfiguration.current
    val currentLocale = remember(configuration) {
        val locales = configuration.locales
        if (!locales.isEmpty) locales[0] else Locale.getDefault()
    }
    val hourFormat = remember { SimpleDateFormat("HH", Locale.getDefault()) }
    val minuteFormat = remember { SimpleDateFormat("mm", Locale.getDefault()) }
    val secondFormat = remember { SimpleDateFormat("ss", Locale.getDefault()) }
    val datePattern = stringResource(R.string.features_date_format)
    val dateFormat = remember(datePattern, currentLocale) {
        SimpleDateFormat(datePattern, currentLocale)
    }

    val hourStr = hourFormat.format(currentTime)
    val minStr = minuteFormat.format(currentTime)
    val secStr = secondFormat.format(currentTime)
    val dateStr = dateFormat.format(currentTime)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = hourStr,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.5).sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            val infiniteTransition = rememberInfiniteTransition(label = "colon_blink")
            val colonAlpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "colon_alpha"
            )
            Text(
                text = ":",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = colonAlpha),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .offset(y = (-3).dp)
            )

            Text(
                text = minStr,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${secStr}s",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SelfCheckItemChip(
    icon: ImageVector,
    title: String,
    status: String,
    isSuccess: Boolean,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSuccess) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.error)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onError,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ChunkerUpdateDialog(
    status: ChunkerUpdateStatus,
    currentVersion: String,
    onDismiss: () -> Unit,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: (ChunkerReleaseInfo) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.CloudDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.chunker_update_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (status) {
                    is ChunkerUpdateStatus.UpdateAvailable -> {
                        val info = status.info
                        Text(
                            text = stringResource(R.string.chunker_update_current_ver, currentVersion),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.chunker_update_latest_ver, info.version),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.chunker_update_file_size, "${info.fileSize / (1024 * 1024)} MB"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (info.newlySupportedVersions.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.chunker_update_new_versions_title) + "：" + info.newlySupportedVersions.joinToString(", ") { it.displayName },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stringResource(R.string.chunker_update_cleanup_tip),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.chunker_update_network_tip),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    is ChunkerUpdateStatus.Downloading -> {
                        Text(
                            text = stringResource(R.string.chunker_update_downloading, (status.progress * 100).toInt(), "${status.downloadedBytes / (1024 * 1024)}MB", "${status.totalBytes / (1024 * 1024)}MB"),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { status.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.chunker_update_network_tip),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is ChunkerUpdateStatus.Installing -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.chunker_update_installing),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    is ChunkerUpdateStatus.Checking -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.cli_status_checking),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    is ChunkerUpdateStatus.Error -> {
                        Text(
                            text = status.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.chunker_update_network_tip),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        Text(
                            text = stringResource(R.string.chunker_update_already_latest, currentVersion),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (status) {
                is ChunkerUpdateStatus.UpdateAvailable -> {
                    androidx.compose.material3.Button(
                        onClick = { onInstallUpdate(status.info) }
                    ) {
                        Text(stringResource(R.string.chunker_update_btn_install))
                    }
                }
                is ChunkerUpdateStatus.Downloading, is ChunkerUpdateStatus.Installing -> {
                }
                else -> {
                    androidx.compose.material3.Button(
                        onClick = onCheckUpdate
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.chunker_update_manual_check))
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (status !is ChunkerUpdateStatus.Downloading && status !is ChunkerUpdateStatus.Installing) {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text(if (status is ChunkerUpdateStatus.UpdateAvailable) stringResource(R.string.chunker_update_btn_later) else stringResource(android.R.string.ok))
                }
            }
        }
    )
}