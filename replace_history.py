import re

def replace_in_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

replacements = [
    ('Toast.makeText(context, "导出补救成功！", Toast.LENGTH_SHORT).show()', 'Toast.makeText(context, context.getString(R.string.history_export_success), Toast.LENGTH_SHORT).show()'),
    ('Toast.makeText(context, "导出失败: ${err.message}", Toast.LENGTH_SHORT).show()', 'Toast.makeText(context, context.getString(R.string.history_export_fail, err.message), Toast.LENGTH_SHORT).show()'),
    ('Text("转换记录", fontWeight = FontWeight.Bold)', 'Text(stringResource(R.string.history_title), fontWeight = FontWeight.Bold)'),
    ('Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")', 'Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.history_back_desc))'),
    ('Icon(Icons.Outlined.Delete, contentDescription = "清空记录")', 'Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.history_clear_desc))'),
    ('Text("暂无转换记录", color = MaterialTheme.colorScheme.onSurfaceVariant)', 'Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)'),
    ('text = "时间: ${sdf.format(Date(record.timestamp))}  耗时: ${record.durationMs / 1000}s",', 'text = stringResource(R.string.history_item_time, sdf.format(Date(record.timestamp)), record.durationMs / 1000),'),
    ('Text("存档详细信息", fontWeight = FontWeight.Bold)', 'Text(stringResource(R.string.history_detail_title), fontWeight = FontWeight.Bold)'),
    ('Text("名称: ${record.worldName}")', 'Text(stringResource(R.string.history_detail_name, record.worldName))'),
    ('Text("来源: ${record.sourcePlatform}")', 'Text(stringResource(R.string.history_detail_source, record.sourcePlatform))'),
    ('Text("目标: ${record.targetPlatform}")', 'Text(stringResource(R.string.history_detail_target, record.targetPlatform))'),
    ('Text("耗时: ${record.durationMs / 1000.0} 秒")', 'Text(stringResource(R.string.history_detail_duration, record.durationMs / 1000.0))'),
    ('Text("导出位置 (URI):", fontWeight = FontWeight.Bold)', 'Text(stringResource(R.string.history_detail_export_uri_title), fontWeight = FontWeight.Bold)'),
    ('Text("导出位置: 暂未导出", color = MaterialTheme.colorScheme.error)', 'Text(stringResource(R.string.history_detail_not_exported), color = MaterialTheme.colorScheme.error)'),
    ('Text("关闭")', 'Text(stringResource(R.string.history_close))'),
    ('Text(if (isExporting) "正在导出..." else "补救导出压缩包")', 'Text(stringResource(if (isExporting) R.string.history_exporting else R.string.history_export_fallback))')
]
replace_in_file('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/java/com/noches/chunkoidng/ui/screens/history/ConversionHistoryScreen.kt', replacements)

# Add R import if missing
with open('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/java/com/noches/chunkoidng/ui/screens/history/ConversionHistoryScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()
if 'import com.noches.chunkoidng.R' not in content:
    content = content.replace('import androidx.compose.ui.res.painterResource', 'import androidx.compose.ui.res.painterResource\nimport androidx.compose.ui.res.stringResource\nimport com.noches.chunkoidng.R')
with open('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/java/com/noches/chunkoidng/ui/screens/history/ConversionHistoryScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
