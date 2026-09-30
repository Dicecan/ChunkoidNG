const fs = require('fs');

function addToXml(path, newStrings) {
    let content = fs.readFileSync(path, 'utf8');
    const idx = content.lastIndexOf('</resources>');
    if (idx === -1) return;
    
    let res = '';
    for (const [k, v] of Object.entries(newStrings)) {
        res += `    <string name="${k}">${v}</string>\n`;
    }
    content = content.substring(0, idx) + res + content.substring(idx);
    fs.writeFileSync(path, content, 'utf8');
}

function replaceInFile(path, replacements) {
    let content = fs.readFileSync(path, 'utf8');
    for (const [oldStr, newStr] of replacements) {
        content = content.split(oldStr).join(newStr);
    }
    
    // Add import R if missing
    if (!content.includes('import com.noches.chunkoidng.R')) {
        // Try to insert it below the package
        content = content.replace(/^package (.*?)$/m, 'package $1\n\nimport com.noches.chunkoidng.R\nimport androidx.compose.ui.res.stringResource');
    }
    
    fs.writeFileSync(path, content, 'utf8');
}

const stringsEn = {
    'feature_conv_title': 'World Conversion',
    'feature_conv_subtitle': 'Bedrock (BE) ↔ Java (JE) two-way conversion\\nSupports 1.8.8+ to 1.21+',
    'feature_conv_badge': 'Core',
    'feature_conv_tag_1': 'Two-way',
    'feature_conv_tag_2': 'Lossless',
    'feature_conv_tag_3': 'Cross-version',
    'feature_decrypt_title': 'Netease Decryption & Encryption',
    'feature_decrypt_subtitle': 'LevelDB XOR stream decryption, supports passive encryption',
    'feature_decrypt_badge': 'Exclusive',
    'feature_decrypt_tag_1': 'Stream decrypt',
    'feature_decrypt_tag_2': 'Passive encrypt',
    'feature_decrypt_tag_3': 'Auto',
    'feature_prune_title': 'Dimension & Chunk Pruning',
    'feature_prune_subtitle': 'Smart pruning of unmodified and invalid chunks',
    'feature_prune_badge': 'Smart Prune',
    'feature_prune_tag_1': 'Builder preset',
    'feature_prune_tag_2': 'Spawn protect',
    'feature_prune_tag_3': 'Anti-crash',
    'feature_nbt_title': 'NBT / LevelDB Editor',
    'feature_nbt_subtitle': 'Tree view visualization, realtime CRUD for keys',
    'feature_nbt_badge': 'Pro Tool',
    'feature_nbt_tag_1': 'Tree fold',
    'feature_nbt_tag_2': 'Hex preview',
    'feature_nbt_tag_3': 'No unzip',
    'feature_res_title': 'Resource Pack Conversion',
    'feature_res_subtitle': 'Auto-convert language files, sounds and manifest',
    'feature_res_badge': 'Res Tool',
    'feature_res_tag_1': 'Texture map',
    'feature_res_tag_2': 'UUID gen',
    'feature_res_tag_3': 'Auto fix',
    'feature_cli_title': 'Sandbox Terminal CLI',
    'feature_cli_subtitle': 'Direct access to Linux sandbox & OpenJDK 17',
    'feature_cli_badge': 'Geek Mode',
    'feature_cli_tag_1': 'OpenJDK 17',
    'feature_cli_tag_2': 'Shell',
    'feature_cli_tag_3': 'Pinch zoom',
    
    'feature_screen_core_tools': 'Core Tool Library',
    'feature_screen_tool_count': '%1$d Tools',
    'feature_screen_tip': 'Click cards to enter the workspace',
    'feature_screen_status_title': 'Core Component Status',
    
    'cli_log_init': '[SYSTEM] Initializing terminal session...',
    'cli_log_release': '[SYSTEM] Releasing RootFS environment...',
    'cli_log_progress': '[SYSTEM] Extraction progress: %1$d%%',
    'cli_log_ready': '[SYSTEM] Environment ready. Testing Java engine...',
    'cli_log_test_done': '[SYSTEM] Test completed.',
    'cli_log_err': '[SYSTEM] Error: RootFS deployment failed!',
    
    'cli_status_extracting': 'Extracting %1$d%%',
    'cli_status_checking': 'Checking',
    'cli_self_test': 'Self Test',
    'cli_status_deploying': 'Deploying...',
    'cli_status_ready': 'Sandbox Ready',
    'cli_status_not_deploy': 'Not Deployed',
    'cli_engine_title': 'Chunker Engine',
    'cli_engine_testing': 'Testing connection...',
    'cli_engine_ready': 'Core Ready',
    'cli_engine_waiting': 'Waiting for mount',
    
    'decrypt_err_export': 'Failed to finish archive export',
    'decrypt_err_target_dir': 'Cannot create target directory',
    'decrypt_err_export_fail': 'Failed to export file: %1$s',
    'decrypt_platform_source': 'Netease Archive',
    'decrypt_platform_decrypted': 'Decrypted',
    'decrypt_platform_encrypted': 'Encrypted',
    'decrypt_err_unknown': 'Unknown export error',
    'decrypt_err_export_failed': 'Export failed: %1$s',
    'decrypt_err_input_stream': 'Cannot open input stream',
    'decrypt_err_create_file': 'Cannot create target file',
    'decrypt_err_output_stream': 'Cannot open output stream',
    
    'feature_card_enter': 'Enter'
};

const stringsZh = {
    'feature_conv_title': '存档转换',
    'feature_conv_subtitle': '基岩版 (BE) ↔ Java 版 (JE) 双向转换\\n支持 1.8.8+ 至 1.21+',
    'feature_conv_badge': '核心',
    'feature_conv_tag_1': '双向转换',
    'feature_conv_tag_2': '无损',
    'feature_conv_tag_3': '跨版本',
    'feature_decrypt_title': '网易存档解密与加密',
    'feature_decrypt_subtitle': 'LevelDB 异或算法流式解密，并支持网易版被动加密',
    'feature_decrypt_badge': '独家黑科技',
    'feature_decrypt_tag_1': '流式解密',
    'feature_decrypt_tag_2': '被动加密',
    'feature_decrypt_tag_3': '全自动',
    'feature_prune_title': '维度与区块裁剪',
    'feature_prune_subtitle': '智能剔除未修改、无效的区块，精简地图体积',
    'feature_prune_badge': '智能瘦身',
    'feature_prune_tag_1': '建筑师预设',
    'feature_prune_tag_2': '出生点保护',
    'feature_prune_tag_3': '防闪退',
    'feature_nbt_title': 'NBT / LevelDB 编辑',
    'feature_nbt_subtitle': '层级树形可视化查看，实时增删改查 NBT 与 LevelDB 键值',
    'feature_nbt_badge': '专业工具',
    'feature_nbt_tag_1': '树形折叠',
    'feature_nbt_tag_2': 'Hex 预览',
    'feature_nbt_tag_3': '免解压',
    'feature_res_title': '材质包双向转换',
    'feature_res_subtitle': '自动转换双端语言文件、音效配置及 manifest',
    'feature_res_badge': '资源工具',
    'feature_res_tag_1': '贴图映射',
    'feature_res_tag_2': 'UUID 生成',
    'feature_res_tag_3': '自动修复',
    'feature_cli_title': '沙箱终端控制台',
    'feature_cli_subtitle': '直通底层 Linux 沙箱与 OpenJDK 17，支持自定义 CLI 命令',
    'feature_cli_badge': '极客模式',
    'feature_cli_tag_1': 'OpenJDK 17',
    'feature_cli_tag_2': 'Shell',
    'feature_cli_tag_3': '手势缩放',
    
    'feature_screen_core_tools': '核心功能库',
    'feature_screen_tool_count': '%1$d 项工具',
    'feature_screen_tip': '点击各卡片即可进入对应转换或编辑工作台',
    'feature_screen_status_title': '核心组件状态',
    
    'cli_log_init': '[SYSTEM] 初始化终端会话...',
    'cli_log_release': '[SYSTEM] 正在释放 RootFS 环境...',
    'cli_log_progress': '[SYSTEM] 解压进度: %1$d%%',
    'cli_log_ready': '[SYSTEM] 环境就绪。正在测试 Java 引擎...',
    'cli_log_test_done': '[SYSTEM] 测试完成。',
    'cli_log_err': '[SYSTEM] 错误: RootFS 部署失败！',
    
    'cli_status_extracting': '解压中 %1$d%%',
    'cli_status_checking': '检查中',
    'cli_self_test': '环境自检',
    'cli_status_deploying': '部署中...',
    'cli_status_ready': '沙箱已就绪',
    'cli_status_not_deploy': '未部署',
    'cli_engine_title': 'Chunker 引擎',
    'cli_engine_testing': '连线测试中...',
    'cli_engine_ready': '核心可用',
    'cli_engine_waiting': '等待挂载',
    
    'decrypt_err_export': '无法完成归档导出',
    'decrypt_err_target_dir': '无法创建目标目录',
    'decrypt_err_export_fail': '导出文件失败: %1$s',
    'decrypt_platform_source': '网易存档',
    'decrypt_platform_decrypted': '已解密',
    'decrypt_platform_encrypted': '已加密',
    'decrypt_err_unknown': '未知导出错误',
    'decrypt_err_export_failed': '导出失败: %1$s',
    'decrypt_err_input_stream': '无法打开输入流',
    'decrypt_err_create_file': '无法创建目标文件',
    'decrypt_err_output_stream': '无法打开输出流',
    
    'feature_card_enter': '进入功能'
};

const stringsJa = {
    'feature_conv_title': 'ワールド変換',
    'feature_conv_subtitle': '統合版 (BE) ↔ Java版 (JE) 双方向変換\\n1.8.8+ 〜 1.21+ 対応',
    'feature_conv_badge': 'コア',
    'feature_conv_tag_1': '双方向',
    'feature_conv_tag_2': '無劣化',
    'feature_conv_tag_3': 'クロスバージョン',
    'feature_decrypt_title': '網易版 復号と暗号化',
    'feature_decrypt_subtitle': 'LevelDB XOR ストリーム復号、受動的暗号化をサポート',
    'feature_decrypt_badge': '専用技術',
    'feature_decrypt_tag_1': 'ストリーム復号',
    'feature_decrypt_tag_2': '受動的暗号化',
    'feature_decrypt_tag_3': '自動',
    'feature_prune_title': 'ディメンションとチャンク軽量化',
    'feature_prune_subtitle': '未変更、無効なチャンクをスマートに削除',
    'feature_prune_badge': '軽量化',
    'feature_prune_tag_1': 'ビルダープリセット',
    'feature_prune_tag_2': 'スポーン保護',
    'feature_prune_tag_3': 'クラッシュ防止',
    'feature_nbt_title': 'NBT / LevelDB エディタ',
    'feature_nbt_subtitle': 'ツリー表示、リアルタイムでキー値を編集',
    'feature_nbt_badge': 'プロツール',
    'feature_nbt_tag_1': 'ツリー折りたたみ',
    'feature_nbt_tag_2': 'Hex プレビュー',
    'feature_nbt_tag_3': '解凍不要',
    'feature_res_title': 'リソースパック変換',
    'feature_res_subtitle': '言語ファイル、サウンド、マニフェストを自動変換',
    'feature_res_badge': 'リソース',
    'feature_res_tag_1': 'テクスチャマッピング',
    'feature_res_tag_2': 'UUID 生成',
    'feature_res_tag_3': '自動修復',
    'feature_cli_title': 'サンドボックスターミナル',
    'feature_cli_subtitle': 'Linux サンドボックスと OpenJDK 17 に直接アクセス',
    'feature_cli_badge': 'ギークモード',
    'feature_cli_tag_1': 'OpenJDK 17',
    'feature_cli_tag_2': 'Shell',
    'feature_cli_tag_3': 'ピンチズーム',
    
    'feature_screen_core_tools': 'コアツール',
    'feature_screen_tool_count': '%1$d 個のツール',
    'feature_screen_tip': 'カードをクリックしてワークスペースに入ります',
    'feature_screen_status_title': 'コンポーネントステータス',
    
    'cli_log_init': '[SYSTEM] ターミナルセッションを初期化しています...',
    'cli_log_release': '[SYSTEM] RootFS環境を解放しています...',
    'cli_log_progress': '[SYSTEM] 解凍進捗: %1$d%%',
    'cli_log_ready': '[SYSTEM] 環境準備完了。Javaエンジンをテスト中...',
    'cli_log_test_done': '[SYSTEM] テスト完了。',
    'cli_log_err': '[SYSTEM] エラー: RootFSの展開に失敗しました！',
    
    'cli_status_extracting': '解凍中 %1$d%%',
    'cli_status_checking': 'チェック中',
    'cli_self_test': 'セルフテスト',
    'cli_status_deploying': '展開中...',
    'cli_status_ready': 'サンドボックス準備完了',
    'cli_status_not_deploy': '未展開',
    'cli_engine_title': 'Chunker エンジン',
    'cli_engine_testing': '接続テスト中...',
    'cli_engine_ready': 'コア利用可能',
    'cli_engine_waiting': 'マウント待機中',
    
    'decrypt_err_export': 'アーカイブのエクスポートを完了できません',
    'decrypt_err_target_dir': 'ターゲットディレクトリを作成できません',
    'decrypt_err_export_fail': 'ファイルのエクスポートに失敗しました: %1$s',
    'decrypt_platform_source': '網易版アーカイブ',
    'decrypt_platform_decrypted': '復号済み',
    'decrypt_platform_encrypted': '暗号化済み',
    'decrypt_err_unknown': '不明なエクスポートエラー',
    'decrypt_err_export_failed': 'エクスポート失敗: %1$s',
    'decrypt_err_input_stream': '入力ストリームを開けません',
    'decrypt_err_create_file': 'ターゲットファイルを作成できません',
    'decrypt_err_output_stream': '出力ストリームを開けません',
    
    'feature_card_enter': '機能に入る'
};

addToXml('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/res/values/strings.xml', stringsEn);
addToXml('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/res/values-zh-rCN/strings.xml', stringsZh);
addToXml('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/res/values-ja/strings.xml', stringsJa);

const featureItemReps = [
    ['title = "存档转换",', 'title = stringResource(R.string.feature_conv_title),'],
    ['subtitle = "基岩版 (BE) ↔ Java 版 (JE) 双向转换\\n支持 1.8.8+ 至 1.21+",', 'subtitle = stringResource(R.string.feature_conv_subtitle),'],
    ['badge = "核心",', 'badge = stringResource(R.string.feature_conv_badge),'],
    ['tags = listOf("双向转换", "无损", "跨版本"),', 'tags = listOf(stringResource(R.string.feature_conv_tag_1), stringResource(R.string.feature_conv_tag_2), stringResource(R.string.feature_conv_tag_3)),'],
    ['title = "网易存档解密与加密",', 'title = stringResource(R.string.feature_decrypt_title),'],
    ['subtitle = "LevelDB 异或算法流式解密，并支持网易版被动加密",', 'subtitle = stringResource(R.string.feature_decrypt_subtitle),'],
    ['badge = "独家黑科技",', 'badge = stringResource(R.string.feature_decrypt_badge),'],
    ['tags = listOf("流式解密", "被动加密", "全自动"),', 'tags = listOf(stringResource(R.string.feature_decrypt_tag_1), stringResource(R.string.feature_decrypt_tag_2), stringResource(R.string.feature_decrypt_tag_3)),'],
    ['title = "维度与区块裁剪",', 'title = stringResource(R.string.feature_prune_title),'],
    ['subtitle = "智能剔除未修改、无效的区块，精简地图体积",', 'subtitle = stringResource(R.string.feature_prune_subtitle),'],
    ['badge = "智能瘦身",', 'badge = stringResource(R.string.feature_prune_badge),'],
    ['tags = listOf("建筑师预设", "出生点保护", "防闪退"),', 'tags = listOf(stringResource(R.string.feature_prune_tag_1), stringResource(R.string.feature_prune_tag_2), stringResource(R.string.feature_prune_tag_3)),'],
    ['title = "NBT / LevelDB 编辑",', 'title = stringResource(R.string.feature_nbt_title),'],
    ['subtitle = "层级树形可视化查看，实时增删改查 NBT 与 LevelDB 键值",', 'subtitle = stringResource(R.string.feature_nbt_subtitle),'],
    ['badge = "专业工具",', 'badge = stringResource(R.string.feature_nbt_badge),'],
    ['tags = listOf("树形折叠", "Hex 预览", "免解压"),', 'tags = listOf(stringResource(R.string.feature_nbt_tag_1), stringResource(R.string.feature_nbt_tag_2), stringResource(R.string.feature_nbt_tag_3)),'],
    ['title = "材质包双向转换",', 'title = stringResource(R.string.feature_res_title),'],
    ['subtitle = "自动转换双端语言文件、音效配置及 manifest",', 'subtitle = stringResource(R.string.feature_res_subtitle),'],
    ['badge = "资源工具",', 'badge = stringResource(R.string.feature_res_badge),'],
    ['tags = listOf("贴图映射", "UUID 生成", "自动修复"),', 'tags = listOf(stringResource(R.string.feature_res_tag_1), stringResource(R.string.feature_res_tag_2), stringResource(R.string.feature_res_tag_3)),'],
    ['title = "沙箱终端控制台",', 'title = stringResource(R.string.feature_cli_title),'],
    ['subtitle = "直通底层 Linux 沙箱与 OpenJDK 17，支持自定义 CLI 命令",', 'subtitle = stringResource(R.string.feature_cli_subtitle),'],
    ['badge = "极客模式",', 'badge = stringResource(R.string.feature_cli_badge),'],
    ['tags = listOf("OpenJDK 17", "Shell", "手势缩放"),', 'tags = listOf(stringResource(R.string.feature_cli_tag_1), stringResource(R.string.feature_cli_tag_2), stringResource(R.string.feature_cli_tag_3)),']
];
replaceInFile('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/java/com/noches/chunkoidng/ui/screens/features/FeatureItem.kt', featureItemReps);

const featureScreenReps = [
    ['text = "核心功能库",', 'text = stringResource(R.string.feature_screen_core_tools),'],
    ['text = "${chunkoidFeatures.size} 项工具",', 'text = stringResource(R.string.feature_screen_tool_count, chunkoidFeatures.size),'],
    ['text = "点击各卡片即可进入对应转换或编辑工作台",', 'text = stringResource(R.string.feature_screen_tip),'],
    ['text = "核心组件状态",', 'text = stringResource(R.string.feature_screen_status_title),'],
    ['terminalLogs = listOf("[SYSTEM] 初始化终端会话...")', 'terminalLogs = listOf(context.getString(R.string.cli_log_init))'],
    ['terminalLogs = terminalLogs + "[SYSTEM] 正在释放 RootFS 环境..."', 'terminalLogs = terminalLogs + context.getString(R.string.cli_log_release)'],
    ['terminalLogs = terminalLogs + "[SYSTEM] 解压进度: $progress%"', 'terminalLogs = terminalLogs + context.getString(R.string.cli_log_progress, progress)'],
    ['terminalLogs = terminalLogs + "[SYSTEM] 环境就绪。正在测试 Java 引擎..."', 'terminalLogs = terminalLogs + context.getString(R.string.cli_log_ready)'],
    ['terminalLogs = terminalLogs + "[SYSTEM] 测试完成。"', 'terminalLogs = terminalLogs + context.getString(R.string.cli_log_test_done)'],
    ['terminalLogs = terminalLogs + "[SYSTEM] 错误: RootFS 部署失败！"', 'terminalLogs = terminalLogs + context.getString(R.string.cli_log_err)'],
    ['text = if (extractionProgress in 1..99) "解压中 $extractionProgress%" else "检查中",', 'text = if (extractionProgress in 1..99) stringResource(R.string.cli_status_extracting, extractionProgress) else stringResource(R.string.cli_status_checking),'],
    ['contentDescription = "环境自检",', 'contentDescription = stringResource(R.string.cli_self_test),'],
    ['text = "环境自检",', 'text = stringResource(R.string.cli_self_test),'],
    ['status = if (isChecking && !rootfsReady) "部署中..." else if (rootfsReady) "沙箱已就绪" else "未部署",', 'status = if (isChecking && !rootfsReady) stringResource(R.string.cli_status_deploying) else if (rootfsReady) stringResource(R.string.cli_status_ready) else stringResource(R.string.cli_status_not_deploy),'],
    ['title = "Chunker 引擎",', 'title = stringResource(R.string.cli_engine_title),'],
    ['status = if (isChecking && rootfsReady) "连线测试中..." else if (rootfsReady) "核心可用" else "等待挂载",', 'status = if (isChecking && rootfsReady) stringResource(R.string.cli_engine_testing) else if (rootfsReady) stringResource(R.string.cli_engine_ready) else stringResource(R.string.cli_engine_waiting),']
];
replaceInFile('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/java/com/noches/chunkoidng/ui/screens/features/FeaturesScreen.kt', featureScreenReps);

const cryptVmReps = [
    ['check(docFile.renameTo("$safeName$extension")) { "无法完成归档导出" }', 'check(docFile.renameTo("$safeName$extension")) { getApplication<Application>().getString(R.string.decrypt_err_export) }'],
    ['Result.failure(Exception("无法创建目标目录"))', 'Result.failure(Exception(getApplication<Application>().getString(R.string.decrypt_err_target_dir)))'],
    ['Result.failure(Exception("导出文件失败: ${failures.take(3).joinToString()}"))', 'Result.failure(Exception(getApplication<Application>().getString(R.string.decrypt_err_export_fail, failures.take(3).joinToString())))'],
    ['sourcePlatform = "网易存档",', 'sourcePlatform = getApplication<Application>().getString(R.string.decrypt_platform_source),'],
    ['targetPlatform = if (state.mode == CryptMode.DECRYPT) "已解密" else "已加密",', 'targetPlatform = if (state.mode == CryptMode.DECRYPT) getApplication<Application>().getString(R.string.decrypt_platform_decrypted) else getApplication<Application>().getString(R.string.decrypt_platform_encrypted),'],
    ['Exception("未知导出错误")', 'Exception(getApplication<Application>().getString(R.string.decrypt_err_unknown))'],
    ['errorMessage = "导出失败: ${err.message}"', 'errorMessage = getApplication<Application>().getString(R.string.decrypt_err_export_failed, err.message)'],
    ['throw java.io.IOException("无法打开输入流")', 'throw java.io.IOException(getApplication<Application>().getString(R.string.decrypt_err_input_stream))'],
    ['throw java.io.IOException("无法创建目标文件")', 'throw java.io.IOException(getApplication<Application>().getString(R.string.decrypt_err_create_file))'],
    ['throw java.io.IOException("无法打开输出流")', 'throw java.io.IOException(getApplication<Application>().getString(R.string.decrypt_err_output_stream))']
];
replaceInFile('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/java/com/noches/chunkoidng/ui/screens/decryptor/NetEaseCryptViewModel.kt', cryptVmReps);

const featureCardReps = [
    ['contentDescription = "进入功能",', 'contentDescription = stringResource(R.string.feature_card_enter),']
];
replaceInFile('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/java/com/noches/chunkoidng/ui/screens/features/FeatureCard.kt', featureCardReps);
