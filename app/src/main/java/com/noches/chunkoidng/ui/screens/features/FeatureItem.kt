package com.noches.chunkoidng.ui.screens.features

import com.noches.chunkoidng.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Dataset
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import com.noches.chunkoidng.ui.theme.FeatureColors

data class FeatureItem(
    val id: String,
    val titleRes: Int,
    val subtitleRes: Int,
    val badgeRes: Int,
    val tagsRes: List<Int>,
    val icon: ImageVector,
    val accentLight: Color,
    val accentContainerLight: Color,
    val accentDark: Color,
    val accentContainerDark: Color,
    val isPrimaryFeatured: Boolean = false,
    val span: StaggeredGridItemSpan = StaggeredGridItemSpan.SingleLane
)

val chunkoidFeatures = listOf(
    FeatureItem(
        id = "world_converter",
        titleRes = R.string.feature_conv_title,
        subtitleRes = R.string.feature_conv_subtitle,
        badgeRes = R.string.feature_conv_badge,
        tagsRes = listOf(R.string.feature_conv_tag_1, R.string.feature_conv_tag_2, R.string.feature_conv_tag_3),
        icon = Icons.Outlined.SwapHoriz,
        accentLight = FeatureColors.ConverterLight,
        accentContainerLight = FeatureColors.ConverterContainerLight,
        accentDark = FeatureColors.ConverterDark,
        accentContainerDark = FeatureColors.ConverterContainerDark,
        span = StaggeredGridItemSpan.FullLine
    ),
    FeatureItem(
        id = "netease_decryptor",
        titleRes = R.string.feature_decrypt_title,
        subtitleRes = R.string.feature_decrypt_subtitle,
        badgeRes = R.string.feature_decrypt_badge,
        tagsRes = listOf(R.string.feature_decrypt_tag_1, R.string.feature_decrypt_tag_2, R.string.feature_decrypt_tag_3),
        icon = Icons.Outlined.LockOpen,
        accentLight = FeatureColors.DecryptorLight,
        accentContainerLight = FeatureColors.DecryptorContainerLight,
        accentDark = FeatureColors.DecryptorDark,
        accentContainerDark = FeatureColors.DecryptorContainerDark
    ),
    FeatureItem(
        id = "dimension_pruner",
        titleRes = R.string.feature_prune_title,
        subtitleRes = R.string.feature_prune_subtitle,
        badgeRes = R.string.feature_prune_badge,
        tagsRes = listOf(R.string.feature_prune_tag_1, R.string.feature_prune_tag_2, R.string.feature_prune_tag_3),
        icon = Icons.Outlined.CleaningServices,
        accentLight = FeatureColors.PrunerLight,
        accentContainerLight = FeatureColors.PrunerContainerLight,
        accentDark = FeatureColors.PrunerDark,
        accentContainerDark = FeatureColors.PrunerContainerDark
    ),
    FeatureItem(
        id = "nbt_editor",
        titleRes = R.string.feature_nbt_title,
        subtitleRes = R.string.feature_nbt_subtitle,
        badgeRes = R.string.feature_nbt_badge,
        tagsRes = listOf(R.string.feature_nbt_tag_1, R.string.feature_nbt_tag_2, R.string.feature_nbt_tag_3),
        icon = Icons.Outlined.Dataset,
        accentLight = FeatureColors.NbtEditorLight,
        accentContainerLight = FeatureColors.NbtEditorContainerLight,
        accentDark = FeatureColors.NbtEditorDark,
        accentContainerDark = FeatureColors.NbtEditorContainerDark
    ),
    FeatureItem(
        id = "pack_converter",
        titleRes = R.string.feature_res_title,
        subtitleRes = R.string.feature_res_subtitle,
        badgeRes = R.string.feature_res_badge,
        tagsRes = listOf(R.string.feature_res_tag_1, R.string.feature_res_tag_2, R.string.feature_res_tag_3),
        icon = Icons.Outlined.Palette,
        accentLight = FeatureColors.PackConverterLight,
        accentContainerLight = FeatureColors.PackConverterContainerLight,
        accentDark = FeatureColors.PackConverterDark,
        accentContainerDark = FeatureColors.PackConverterContainerDark
    ),
    FeatureItem(
        id = "midi_converter",
        titleRes = R.string.feature_midi_title,
        subtitleRes = R.string.feature_midi_subtitle,
        badgeRes = R.string.feature_midi_badge,
        tagsRes = listOf(R.string.feature_midi_tag_1, R.string.feature_midi_tag_2, R.string.feature_midi_tag_3),
        icon = Icons.Outlined.MusicNote,
        accentLight = FeatureColors.MidiConverterLight,
        accentContainerLight = FeatureColors.MidiConverterContainerLight,
        accentDark = FeatureColors.MidiConverterDark,
        accentContainerDark = FeatureColors.MidiConverterContainerDark
    ),
    FeatureItem(
        id = "sandbox_terminal",
        titleRes = R.string.feature_cli_title,
        subtitleRes = R.string.feature_cli_subtitle,
        badgeRes = R.string.feature_cli_badge,
        tagsRes = listOf(R.string.feature_cli_tag_1, R.string.feature_cli_tag_2, R.string.feature_cli_tag_3),
        icon = Icons.Outlined.Terminal,
        accentLight = FeatureColors.TerminalLight,
        accentContainerLight = FeatureColors.TerminalContainerLight,
        accentDark = FeatureColors.TerminalDark,
        accentContainerDark = FeatureColors.TerminalContainerDark
    )
)
