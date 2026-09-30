package com.noches.chunkoidng.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noches.chunkoidng.R
import com.noches.chunkoidng.core.conversion.ChunkerFormat
import com.noches.chunkoidng.core.version.MinecraftVersion
import com.noches.chunkoidng.core.version.RelationType
import com.noches.chunkoidng.core.version.RiskLevel
import com.noches.chunkoidng.core.version.VersionMappingTable
import com.noches.chunkoidng.core.world.Platform
import com.noches.chunkoidng.core.world.WorldInfo
import com.noches.chunkoidng.ui.theme.ExpressiveShapes
import com.noches.chunkoidng.ui.theme.SquircleIconShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatPickerBottomSheet(
    sourceWorld: WorldInfo?,
    currentFormat: ChunkerFormat,
    onSelectFormat: (ChunkerFormat) -> Unit,
    onDismiss: () -> Unit
) {
    val sourcePlatform = sourceWorld?.platform ?: Platform.BEDROCK
    val sourceVersion = remember(sourceWorld) {
        if (sourceWorld != null) {
            MinecraftVersion.parse(sourceWorld.versionName, sourceWorld.platform, sourceWorld.versionId)
        } else null
    }

    var selectedTab by remember { mutableStateOf(if (sourcePlatform == Platform.BEDROCK) 1 else 0) }
    var selectedFilterIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentRelation = remember(sourceVersion, currentFormat) {
        VersionMappingTable.evaluateRelation(sourceVersion, currentFormat)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = ExpressiveShapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(SquircleIconShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.version_picker_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.version_picker_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.common_close))
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; selectedFilterIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    icon = {}
                ) {
                    Text(stringResource(R.string.platform_bedrock), maxLines = 1, softWrap = false, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }

                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; selectedFilterIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    icon = {}
                ) {
                    Text(stringResource(R.string.platform_java), maxLines = 1, softWrap = false, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }

                SegmentedButton(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; selectedFilterIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    icon = {}
                ) {
                    Text(stringResource(R.string.version_keep_input), maxLines = 1, softWrap = false, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (selectedTab != 2) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilterIndex == 0,
                        onClick = { selectedFilterIndex = 0 },
                        label = { Text(stringResource(R.string.version_filter_all), fontSize = 11.5.sp) }
                    )
                    FilterChip(
                        selected = selectedFilterIndex == 1,
                        onClick = { selectedFilterIndex = 1 },
                        label = { Text(stringResource(R.string.version_filter_recommended), fontSize = 11.5.sp) }
                    )
                    FilterChip(
                        selected = selectedFilterIndex == 2,
                        onClick = { selectedFilterIndex = 2 },
                        label = { Text(stringResource(R.string.version_filter_upgrade), fontSize = 11.5.sp) }
                    )
                    FilterChip(
                        selected = selectedFilterIndex == 3,
                        onClick = { selectedFilterIndex = 3 },
                        label = { Text(stringResource(R.string.version_filter_downgrade), fontSize = 11.5.sp) }
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.version_search_placeholder), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.common_clear), modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = ExpressiveShapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            AnimatedVisibility(
                visible = currentRelation.relationType == RelationType.DOWNGRADE && currentRelation.warningKey != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val warningText = when (currentRelation.warningKey) {
                    "risk_height_truncation" -> stringResource(R.string.risk_height_truncation)
                    "risk_block_flattening" -> stringResource(R.string.risk_block_flattening)
                    else -> stringResource(R.string.risk_general_downgrade)
                }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = ExpressiveShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.risk_title),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = warningText,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            val allFormats = when (selectedTab) {
                0 -> ChunkerFormat.BEDROCK_FORMATS
                1 -> ChunkerFormat.JAVA_FORMATS
                else -> listOf(ChunkerFormat.FORMAT_INPUT)
            }

            val filteredFormats = allFormats.filter { format ->
                val matchesSearch = if (searchQuery.isBlank()) true else {
                    format.displayName.contains(searchQuery, ignoreCase = true) ||
                    format.id.contains(searchQuery, ignoreCase = true) ||
                    format.group.contains(searchQuery, ignoreCase = true)
                }
                if (!matchesSearch) return@filter false

                if (selectedTab == 2) return@filter true

                val rel = VersionMappingTable.evaluateRelation(sourceVersion, format)
                when (selectedFilterIndex) {
                    1 -> rel.relationType == RelationType.RECOMMENDED_MATCH
                    2 -> rel.relationType == RelationType.UPGRADE
                    3 -> rel.relationType == RelationType.DOWNGRADE
                    else -> true
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredFormats, key = { it.id }) { format ->
                    val isSelected = format.id == currentFormat.id
                    val relation = remember(sourceVersion, format) {
                        VersionMappingTable.evaluateRelation(sourceVersion, format)
                    }

                    Card(
                        onClick = { onSelectFormat(format) },
                        shape = ExpressiveShapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        ),
                        border = if (isSelected) {
                            BorderStroke(1.8.dp, MaterialTheme.colorScheme.primary)
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHighest
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (format.id == "INPUT") Icons.Outlined.Tune
                                    else if (format.platform == Platform.BEDROCK) Icons.Outlined.PhoneAndroid
                                    else Icons.Outlined.Computer,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = format.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )

                                    if (format.id != "INPUT") {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        when (relation.relationType) {
                                            RelationType.RECOMMENDED_MATCH -> {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.version_badge_recommended),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            RelationType.UPGRADE -> {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.version_badge_upgrade),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                            RelationType.DOWNGRADE -> {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.version_badge_downgrade),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.tertiary
                                                    )
                                                }
                                            }
                                            else -> {}
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (format.id == "INPUT") stringResource(R.string.version_keep_input_desc)
                                    else stringResource(R.string.version_series_format, format.group),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = stringResource(R.string.common_select),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
