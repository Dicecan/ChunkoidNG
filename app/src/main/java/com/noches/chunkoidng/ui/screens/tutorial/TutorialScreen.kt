package com.noches.chunkoidng.ui.screens.tutorial

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noches.chunkoidng.ui.theme.ChipBadgeShape
import com.noches.chunkoidng.ui.theme.ExpressiveShapes

data class FaqItem(
    val question: String,
    val answer: String,
    val category: String = "常见问题"
)

private val officialFaqList = listOf(
    FaqItem(
        question = "打开转换后的文件夹文件很少，游戏无法读取（没有 level.dat/db/region）？",
        answer = "请重新转换一次，并在转换时观察实时日志流输出。通常是因为输入源的压缩包内存在多层文件夹嵌套，或源世界缺少核心数据库指针文件。建议直接将存档解压为文件夹后再选取导入。"
    ),
    FaqItem(
        question = "转换时出现 Termux environment not initialized 错误？",
        answer = "这表示应用的 RootFS 运行环境未完全解压或被安全清理软件破坏。请到【设置】中心点击【重置并重新初始化沙箱环境】，或重新授予应用完整的存储权限。"
    ),
    FaqItem(
        question = "转换大存档时，切到后台或者息屏后软件突然中断退出？",
        answer = "部分手机厂商系统（如 HyperOS、ColorOS、OriginOS、HarmonyOS）后台策略较激进。请在【设置】中开启【后台唤醒锁 (WakeLock)】，并将 Chunkoid 的电池策略设为【无限制/允许后台高耗电运行】，同时在多任务界面锁定软件卡片。"
    ),
    FaqItem(
        question = "手机运存较小（4GB~6GB），转换大世界容易卡顿或闪退？",
        answer = "请在【设置】中开启【防闪退模式（低运存优化）】，系统会自动向 JVM 注入串行 GC 与单线程并发限制参数，并适当调低 Java 虚拟机最大分配内存。"
    ),
    FaqItem(
        question = "提示 Original NBT is not available for this conversion 错误？",
        answer = "由于跨平台转换时，基岩版与 Java 版的 NBT 结构定义不一致，部分版本无法直接继承未转换的原始标签。请在转换设置中关闭【保留原始 NBT】即可顺利完成转换。"
    ),
    FaqItem(
        question = "网易版地图解密失败或者找不到 db 文件夹？",
        answer = "网易存档解密器依赖 LevelDB 的 CURRENT 指针文件与 MANIFEST 文件。请确保选中的是包含 db/ 文件夹的世界根目录，而非外部的应用备份父级目录。"
    )
)

@Composable
fun TutorialScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Official Wiki Connect Banner
        item {
            WikiPortalCard(
                onOpenMainWiki = { openUrl("https://chunkoid.top/docs/index.html") },
                onOpenInstructions = { openUrl("https://chunkoid.top/docs/index.html?doc=Instructions") },
                onOpenFaq = { openUrl("https://chunkoid.top/docs/index.html?doc=FAQ") },
                onOpenWebsite = { openUrl("https://chunkoid.top") }
            )
        }

        // 2. Section Header: Quick Start
        item {
            Text(
                text = "世界转换标准流程",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        item {
            WorkflowStepsCard()
        }

        // 3. Section Header: Official FAQ
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.QuestionAnswer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "官网常见问题排错 (FAQ)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        items(officialFaqList) { faq ->
            ExpandableFaqCard(faq = faq)
        }

        // 4. Community Support Card
        item {
            CommunitySupportCard(
                onJoinGroup = {
                    openUrl("https://qm.qq.com/cgi-bin/qm/qr?k=Chunkoid")
                }
            )
        }
    }
}

@Composable
private fun WikiPortalCard(
    onOpenMainWiki: () -> Unit,
    onOpenInstructions: () -> Unit,
    onOpenFaq: () -> Unit,
    onOpenWebsite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(ExpressiveShapes.small)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Chunkoid 官方在线 Wiki 文档站",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "chunkoid.top · 实时同步官方最新文档与操作手册",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onOpenMainWiki,
                modifier = Modifier.fillMaxWidth(),
                shape = ExpressiveShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("前往官方在线 Wiki 文档站 (Docs)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenInstructions,
                    modifier = Modifier.weight(1f),
                    shape = ExpressiveShapes.small
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("操作手册", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onOpenFaq,
                    modifier = Modifier.weight(1f),
                    shape = ExpressiveShapes.small
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("在线 FAQ", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun WorkflowStepsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StepItem(
                step = 1,
                title = "选择世界存档",
                description = "支持直接导入 .zip 压缩包、.mcworld 格式或已解压的目录，内置扫描系统会自动寻找 level.dat 和数据库。"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            StepItem(
                step = 2,
                title = "选择目标版本与平台",
                description = "支持 Java 1.8.8 ~ 1.21+ 与基岩版全系列互转，并可按需选择是否开启维度裁剪。"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            StepItem(
                step = 3,
                title = "开始转换与后台监控",
                description = "转换由嵌入式 OpenJDK 17 沙箱执行，通知栏与界面实时呈现进度百分比与执行日志。"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            StepItem(
                step = 4,
                title = "统一输出与管理",
                description = "转换完毕后文件存放于系统 Documents/chunkoid output 专区，支持一键导出到游戏。"
            )
        }
    }
}

@Composable
private fun StepItem(
    step: Int,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(ChipBadgeShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$step",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ExpandableFaqCard(faq: FaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ExpressiveShapes.medium)
            .clickable { expanded = !expanded },
        shape = ExpressiveShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = faq.answer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunitySupportCard(onJoinGroup: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "💬 官方交流反馈群",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "遇到未知错误或需特殊格式转换排错，欢迎加入官方交流群交流与反馈：群号 1103983368",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
