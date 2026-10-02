package com.noches.chunkoidng.ui.screens.pack

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noches.chunkoidng.R
import com.noches.chunkoidng.core.pack.BedrockEngineVersion
import com.noches.chunkoidng.core.pack.PackPlatform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackConverterScreen(
    onNavigateBack: () -> Unit,
    viewModel: PackConverterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "pack.zip"
            var fileSize = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }
            viewModel.onPackSelected(uri, fileName, fileSize)
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveConvertedPack(uri) { success ->
                if (success) {
                    Toast.makeText(context, R.string.pack_conv_success, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pack_conv_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalIconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Outlined.FolderZip, contentDescription = null)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.selectedPackName.isNotBlank()) uiState.selectedPackName else stringResource(R.string.pack_conv_select_file),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (uiState.selectedPackSizeBytes > 0) {
                                    val sizeMb = uiState.selectedPackSizeBytes.toDouble() / (1024.0 * 1024.0)
                                    "%.2f MB".format(sizeMb)
                                } else {
                                    stringResource(R.string.pack_conv_select_file_desc)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
                            Text(stringResource(R.string.common_select))
                        }
                    }

                    if (uiState.detectedPlatform != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.pack_conv_detected_platform,
                                    uiState.detectedPlatform?.name ?: "Unknown"
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.version_picker_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.sourcePlatform == PackPlatform.JAVA && uiState.targetPlatform == PackPlatform.BEDROCK,
                            onClick = {
                                viewModel.setDirection(PackPlatform.JAVA, PackPlatform.BEDROCK)
                            },
                            label = { Text(stringResource(R.string.pack_conv_direction_java_to_bedrock)) },
                            leadingIcon = if (uiState.sourcePlatform == PackPlatform.JAVA) {
                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = uiState.sourcePlatform == PackPlatform.BEDROCK && uiState.targetPlatform == PackPlatform.JAVA,
                            onClick = {
                                viewModel.setDirection(PackPlatform.BEDROCK, PackPlatform.JAVA)
                            },
                            label = { Text(stringResource(R.string.pack_conv_direction_bedrock_to_java)) },
                            leadingIcon = if (uiState.sourcePlatform == PackPlatform.BEDROCK) {
                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }

                    if (uiState.targetPlatform == PackPlatform.BEDROCK) {
                        Text(
                            text = stringResource(R.string.pack_conv_target_version),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (ver in BedrockEngineVersion.ALL.take(3)) {
                                FilterChip(
                                    selected = uiState.targetBedrockVersion == ver,
                                    onClick = { viewModel.setTargetBedrockVersion(ver) },
                                    label = { Text("v${ver.toVersionString()}") }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.pack_conv_target_java_format),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (fmt in listOf(34, 15, 12)) {
                                FilterChip(
                                    selected = uiState.targetJavaFormat == fmt,
                                    onClick = { viewModel.setTargetJavaFormat(fmt) },
                                    label = { Text("Format $fmt") }
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.feature_res_badge),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.pack_conv_opt_atlases), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = uiState.generateAtlases,
                            onCheckedChange = { viewModel.toggleGenerateAtlases(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.pack_conv_opt_flipbook), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = uiState.convertAnimations,
                            onCheckedChange = { viewModel.toggleConvertAnimations(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.pack_conv_opt_lang), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = uiState.convertLang,
                            onCheckedChange = { viewModel.toggleConvertLang(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.pack_conv_opt_sounds), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = uiState.convertSounds,
                            onCheckedChange = { viewModel.toggleConvertSounds(it) }
                        )
                    }
                }
            }

            if (uiState.isConverting) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = uiState.currentStepText.ifBlank { stringResource(R.string.pack_conv_converting) },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (uiState.progressTotal > 0) {
                            LinearProgressIndicator(
                                progress = { (uiState.progressCurrent.toFloat() / uiState.progressTotal.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            if (uiState.errorMessage != null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (uiState.convertedFile != null) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(
                                    text = stringResource(R.string.pack_conv_success),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.pack_conv_success_desc, uiState.filesConvertedCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val ext = if (uiState.targetPlatform == PackPlatform.BEDROCK) "mcpack" else "zip"
                                val defaultName = "${uiState.selectedPackName.substringBeforeLast('.')}_converted.$ext"
                                saveFileLauncher.launch(defaultName)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.pack_conv_save_pack))
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (uiState.selectedPackUri == null) {
                        Toast.makeText(context, R.string.pack_conv_no_file, Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.startConversion()
                    }
                },
                enabled = !uiState.isConverting && uiState.selectedPackUri != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Outlined.Transform, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.pack_conv_btn_start),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
