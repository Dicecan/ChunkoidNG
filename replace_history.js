const fs = require('fs');

function replaceInFile(path, replacements) {
    let content = fs.readFileSync(path, 'utf8');
    
    for (const [oldStr, newStr] of replacements) {
        if (!content.includes(oldStr)) {
            console.warn(`WARNING: '${oldStr.substring(0, 30)}...' not found!`);
        }
        content = content.split(oldStr).join(newStr);
    }
    
    if (!content.includes('import com.noches.chunkoidng.R')) {
        content = content.replace('import androidx.compose.ui.Alignment', 'import androidx.compose.ui.res.stringResource\nimport com.noches.chunkoidng.R\nimport androidx.compose.ui.Alignment');
    }
    
    fs.writeFileSync(path, content, 'utf8');
    console.log(`Updated ${path}`);
}

const replacements = [
    ['Toast.makeText(context, "导出补救成功！", Toast.LENGTH_SHORT).show()', 'Toast.makeText(context, context.getString(R.string.history_export_success), Toast.LENGTH_SHORT).show()'],
    ['Toast.makeText(context, "导出失败: ${err.message}", Toast.LENGTH_SHORT).show()', 'Toast.makeText(context, context.getString(R.string.history_export_fail, err.message), Toast.LENGTH_SHORT).show()'],
    ['Text("转换记录", fontWeight = FontWeight.Bold)', 'Text(stringResource(R.string.history_title), fontWeight = FontWeight.Bold)'],
    ['Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")', 'Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.history_back_desc))'],
    ['Icon(Icons.Outlined.Delete, contentDescription = "清空记录")', 'Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.history_clear_desc))'],
    ['Text("暂无转换记录", color = MaterialTheme.colorScheme.onSurfaceVariant)', 'Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)'],
    ['text = "时间: ${sdf.format(Date(record.timestamp))}  耗时: ${record.durationMs / 1000}s",', 'text = stringResource(R.string.history_item_time, sdf.format(Date(record.timestamp)), record.durationMs / 1000),'],
    ['Text("存档详细信息", fontWeight = FontWeight.Bold)', 'Text(stringResource(R.string.history_detail_title), fontWeight = FontWeight.Bold)'],
    ['Text("名称: ${record.worldName}")', 'Text(stringResource(R.string.history_detail_name, record.worldName))'],
    ['Text("来源: ${record.sourcePlatform}")', 'Text(stringResource(R.string.history_detail_source, record.sourcePlatform))'],
    ['Text("目标: ${record.targetPlatform}")', 'Text(stringResource(R.string.history_detail_target, record.targetPlatform))'],
    ['Text("耗时: ${record.durationMs / 1000.0} 秒")', 'Text(stringResource(R.string.history_detail_duration, record.durationMs / 1000.0))'],
    ['Text("导出位置 (URI):", fontWeight = FontWeight.Bold)', 'Text(stringResource(R.string.history_detail_export_uri_title), fontWeight = FontWeight.Bold)'],
    ['Text("导出位置: 暂未导出", color = MaterialTheme.colorScheme.error)', 'Text(stringResource(R.string.history_detail_not_exported), color = MaterialTheme.colorScheme.error)'],
    ['Text("关闭")', 'Text(stringResource(R.string.history_close))'],
    ['Text(if (isExporting) "正在导出..." else "补救导出压缩包")', 'Text(stringResource(if (isExporting) R.string.history_exporting else R.string.history_export_fallback))']
];

replaceInFile('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/java/com/noches/chunkoidng/ui/screens/history/ConversionHistoryScreen.kt', replacements);
