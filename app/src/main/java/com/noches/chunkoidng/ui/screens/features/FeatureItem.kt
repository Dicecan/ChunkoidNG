package com.noches.chunkoidng.ui.screens.features

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Dataset
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import com.noches.chunkoidng.ui.theme.FeatureColors

data class FeatureItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val tags: List<String>,
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
        title = "存档转换",
        subtitle = "基岩版 (BE) ↔ Java 版 (JE) 双向转换\n支持 1.8.8+ 至 1.21+",
        badge = "核心",
        tags = listOf("双向转换", "无损", "跨版本"),
        icon = Icons.Outlined.SwapHoriz,
        accentLight = FeatureColors.ConverterLight,
        accentContainerLight = FeatureColors.ConverterContainerLight,
        accentDark = FeatureColors.ConverterDark,
        accentContainerDark = FeatureColors.ConverterContainerDark,
        span = StaggeredGridItemSpan.FullLine
    ),
    FeatureItem(
        id = "netease_decryptor",
        title = "网易存档解密",
        subtitle = "通过 LevelDB 指针异或算法自动提取并还原网易版加密存档",
        badge = "独家黑科技",
        tags = listOf("流式解密", "LevelDB", "全自动"),
        icon = Icons.Outlined.LockOpen,
        accentLight = FeatureColors.DecryptorLight,
        accentContainerLight = FeatureColors.DecryptorContainerLight,
        accentDark = FeatureColors.DecryptorDark,
        accentContainerDark = FeatureColors.DecryptorContainerDark
    ),
    FeatureItem(
        id = "nbt_editor",
        title = "NBT / LevelDB 编辑",
        subtitle = "层级树形可视化查看，实时增删改查 NBT 与 LevelDB 键值",
        badge = "专业工具",
        tags = listOf("树形折叠", "Hex 预览", "免解压"),
        icon = Icons.Outlined.Dataset,
        accentLight = FeatureColors.NbtEditorLight,
        accentContainerLight = FeatureColors.NbtEditorContainerLight,
        accentDark = FeatureColors.NbtEditorDark,
        accentContainerDark = FeatureColors.NbtEditorContainerDark
    ),
    FeatureItem(
        id = "dimension_pruner",
        title = "维度与区块裁剪",
        subtitle = "智能剔除未修改、无效的区块，精简地图体积",
        badge = "智能瘦身",
        tags = listOf("建筑师预设", "出生点保护", "防闪退"),
        icon = Icons.Outlined.CleaningServices,
        accentLight = FeatureColors.PrunerLight,
        accentContainerLight = FeatureColors.PrunerContainerLight,
        accentDark = FeatureColors.PrunerDark,
        accentContainerDark = FeatureColors.PrunerContainerDark
    ),
    FeatureItem(
        id = "pack_converter",
        title = "材质包双向转换",
        subtitle = "自动转换双端语言文件、音效配置及 manifest",
        badge = "资源工具",
        tags = listOf("贴图映射", "UUID 生成", "自动修复"),
        icon = Icons.Outlined.Palette,
        accentLight = FeatureColors.PackConverterLight,
        accentContainerLight = FeatureColors.PackConverterContainerLight,
        accentDark = FeatureColors.PackConverterDark,
        accentContainerDark = FeatureColors.PackConverterContainerDark
    ),
    FeatureItem(
        id = "sandbox_terminal",
        title = "沙箱终端控制台",
        subtitle = "直通底层 Linux 沙箱与 OpenJDK 17，支持自定义 CLI 命令",
        badge = "极客模式",
        tags = listOf("OpenJDK 17", "Shell", "手势缩放"),
        icon = Icons.Outlined.Terminal,
        accentLight = FeatureColors.TerminalLight,
        accentContainerLight = FeatureColors.TerminalContainerLight,
        accentDark = FeatureColors.TerminalDark,
        accentContainerDark = FeatureColors.TerminalContainerDark
    )
)
