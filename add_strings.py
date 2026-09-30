import xml.etree.ElementTree as ET
import sys

def parse_and_add(file_path, new_strings):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    insert_idx = content.rfind('</resources>')
    if insert_idx == -1:
        print(f'Error: </resources> not found in {file_path}')
        sys.exit(1)
        
    res = ''
    for k, v in new_strings.items():
        v = v.replace('\\', '\\\\').replace('\"', '\\\"').replace('\'', '\\\'')
        res += f'    <string name="{k}">{v}</string>\n'
        
    new_content = content[:insert_idx] + res + content[insert_idx:]
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print(f'Updated {file_path}')

strings_en = {
    'history_export_success': 'Rescue export successful!',
    'history_export_fail': 'Export failed: %1$s',
    'history_title': 'Conversion History',
    'history_back_desc': 'Back',
    'history_clear_desc': 'Clear History',
    'history_empty': 'No conversion history',
    'history_item_time': 'Time: %1$s  Duration: %2$ds',
    'history_detail_title': 'Archive Details',
    'history_detail_name': 'Name: %1$s',
    'history_detail_source': 'Source: %1$s',
    'history_detail_target': 'Target: %1$s',
    'history_detail_duration': 'Duration: %1$.1f s',
    'history_detail_export_uri_title': 'Export Location (URI):',
    'history_detail_not_exported': 'Export Location: Not exported yet',
    'history_close': 'Close',
    'history_exporting': 'Exporting...',
    'history_export_fallback': 'Rescue Export Archive',
    
    'pruner_pruned_version_suffix': '%1$s (Pruned)',
    'pruner_log_success': '[SUCCESS] Map pruned successfully! Time: %1$d s',
    'pruner_log_interrupted': 'Pruning interrupted: %1$s',
    'pruner_staging_unzipping': 'Unzipping and analyzing map...',
    'pruner_error_archive_load': 'Failed to load archive: %1$s',
    'pruner_staging_reading_world': 'Reading world data...',
    'pruner_error_dir_import': 'Failed to import directory: %1$s',
    'pruner_stage_starting_engine': 'Starting pruning engine...',
    'pruner_log_starting': '[SYSTEM] Starting world pruning and dimension trimming...',
    'pruner_log_cancelled': '[CANCEL] Pruning was cancelled by the user.',
    
    'tutorial_faq_category': 'FAQ',
    'tutorial_faq_q1': 'The converted folder has very few files and the game cannot read it (missing level.dat/db/region)?',
    'tutorial_faq_a1': 'Please convert again and observe the real-time log output. This usually happens because the input archive contains nested folders, or the source world is missing core database pointer files. It is recommended to unzip the archive into a folder before importing.',
    'tutorial_faq_q2': 'Getting "Termux environment not initialized" error during conversion?',
    'tutorial_faq_a2': "This means the app's RootFS environment was not fully extracted or was damaged by a cleaner app. Please go to Settings -> \"Reset RootFS\", or re-grant full storage permissions to the app.",
    'tutorial_faq_q3': 'The app suddenly stops or crashes in the background during long conversions?',
    'tutorial_faq_a3': 'Some systems (HyperOS, ColorOS, OriginOS, HarmonyOS) have aggressive background killing policies. Please enable "Wakelock" in Settings, set Chunkoid\'s battery usage to "Unrestricted", and lock the app in the recent tasks view.',
    'tutorial_faq_q4': 'The app lags or crashes when converting large worlds on low-RAM devices (4GB~6GB)?',
    'tutorial_faq_a4': 'Please enable "Anti-Crash Mode (Low RAM Optimization)" in Settings. This will inject serial GC and concurrency limits into the JVM, and lower the maximum Java heap allocation.',
    'tutorial_faq_q5': 'Getting "Original NBT is not available for this conversion" error?',
    'tutorial_faq_a5': 'Because Bedrock and Java editions have different NBT structures, some versions cannot preserve unmodified original tags during cross-platform conversion. Please disable "Keep Original NBT" in the conversion settings.',
    'tutorial_faq_q6': 'Netease edition map decryption fails or the db folder is missing?',
    'tutorial_faq_a6': 'The Netease decrypter relies on LevelDB\'s CURRENT and MANIFEST files. Make sure you select the world root folder containing the db/ folder, not the outer app backup folder.',
    'tutorial_flow_title': 'Standard Conversion Workflow',
    'tutorial_faq_title': 'Official FAQ & Troubleshooting',
    'tutorial_wiki_title': 'Chunkoid Official Wiki & Docs',
    'tutorial_wiki_desc': 'chunkoid.top · Real-time synchronization of the latest official documentation and manual',
    'tutorial_wiki_button': 'Go to Official Wiki Docs',
    'tutorial_manual_tab': 'User Manual',
    'tutorial_faq_tab': 'Online FAQ',
    'tutorial_step1_title': '1. Select World Archive',
    'tutorial_step1_desc': 'Supports importing .zip, .mcworld, or extracted folders directly. The built-in scanner automatically locates level.dat and the database.',
    'tutorial_step2_title': '2. Select Target Version & Platform',
    'tutorial_step2_desc': 'Supports mutual conversion between Java 1.8.8 ~ 1.21+ and all Bedrock series. You can also optionally enable dimension pruning.',
    'tutorial_step3_title': '3. Start Conversion & Monitoring',
    'tutorial_step3_desc': 'Conversion runs in the embedded OpenJDK 17 sandbox. Progress and real-time logs are shown in the app and notification bar.',
    'tutorial_step4_title': '4. Output & Management',
    'tutorial_step4_desc': 'Converted files are saved in the system\'s Documents/chunkoid output folder, supporting one-click export to the game.',
    'tutorial_group_title': '💬 Official Support Group',
    'tutorial_group_desc': 'If you encounter unknown errors or need help with special format conversions, welcome to join our official feedback group (QQ: 1103983368).'
}

strings_zh = {
    'history_export_success': '导出补救成功！',
    'history_export_fail': '导出失败: %1$s',
    'history_title': '转换记录',
    'history_back_desc': '返回',
    'history_clear_desc': '清空记录',
    'history_empty': '暂无转换记录',
    'history_item_time': '时间: %1$s  耗时: %2$ds',
    'history_detail_title': '存档详细信息',
    'history_detail_name': '名称: %1$s',
    'history_detail_source': '来源: %1$s',
    'history_detail_target': '目标: %1$s',
    'history_detail_duration': '耗时: %1$.1f 秒',
    'history_detail_export_uri_title': '导出位置 (URI):',
    'history_detail_not_exported': '导出位置: 暂未导出',
    'history_close': '关闭',
    'history_exporting': '正在导出...',
    'history_export_fallback': '补救导出压缩包',
    
    'pruner_pruned_version_suffix': '%1$s (瘦身版)',
    'pruner_log_success': '[SUCCESS] 地图瘦身裁剪成功！耗时: %1$d s',
    'pruner_log_interrupted': '瘦身裁剪中断: %1$s',
    'pruner_staging_unzipping': '正在解压并分析地图...',
    'pruner_error_archive_load': '归档加载失败: %1$s',
    'pruner_staging_reading_world': '正在读取世界数据...',
    'pruner_error_dir_import': '目录导入失败: %1$s',
    'pruner_stage_starting_engine': '启动瘦身引擎...',
    'pruner_log_starting': '[SYSTEM] 开始执行存档瘦身与维度裁剪...',
    'pruner_log_cancelled': '[CANCEL] 瘦身已被用户主动取消。',

    'tutorial_faq_category': '常见问题',
    'tutorial_faq_q1': '打开转换后的文件夹文件很少，游戏无法读取（没有 level.dat/db/region）？',
    'tutorial_faq_a1': '请重新转换一次，并在转换时观察实时日志流输出。通常是因为输入源的压缩包内存在多层文件夹嵌套，或源世界缺少核心数据库指针文件。建议直接将存档解压为文件夹后再选取导入。',
    'tutorial_faq_q2': '转换时出现 Termux environment not initialized 错误？',
    'tutorial_faq_a2': '这表示应用的 RootFS 运行环境未完全解压或被安全清理软件破坏。请到【设置】中心点击【重置并重新初始化沙箱环境】，或重新授予应用完整的存储权限。',
    'tutorial_faq_q3': '转换大存档时，切到后台或者息屏后软件突然中断退出？',
    'tutorial_faq_a3': '部分手机厂商系统（如 HyperOS、ColorOS、OriginOS、HarmonyOS）后台策略较激进。请在【设置】中开启【后台唤醒锁 (WakeLock)】，并将 Chunkoid 的电池策略设为【无限制/允许后台高耗电运行】，同时在多任务界面锁定软件卡片。',
    'tutorial_faq_q4': '手机运存较小（4GB~6GB），转换大世界容易卡顿或闪退？',
    'tutorial_faq_a4': '请在【设置】中开启【防闪退模式（低运存优化）】，系统会自动向 JVM 注入串行 GC 与单线程并发限制参数，并适当调低 Java 虚拟机最大分配内存。',
    'tutorial_faq_q5': '提示 Original NBT is not available for this conversion 错误？',
    'tutorial_faq_a5': '由于跨平台转换时，基岩版与 Java 版的 NBT 结构定义不一致，部分版本无法直接继承未转换的原始标签。请在转换设置中关闭【保留原始 NBT】即可顺利完成转换。',
    'tutorial_faq_q6': '网易版地图解密失败或者找不到 db 文件夹？',
    'tutorial_faq_a6': '网易存档解密器依赖 LevelDB 的 CURRENT 指针文件与 MANIFEST 文件。请确保选中的是包含 db/ 文件夹的世界根目录，而非外部的应用备份父级目录。',
    'tutorial_flow_title': '世界转换标准流程',
    'tutorial_faq_title': '官网常见问题排错 (FAQ)',
    'tutorial_wiki_title': 'Chunkoid 官方在线 Wiki 文档站',
    'tutorial_wiki_desc': 'chunkoid.top · 实时同步官方最新文档与操作手册',
    'tutorial_wiki_button': '前往官方在线 Wiki 文档站 (Docs)',
    'tutorial_manual_tab': '操作手册',
    'tutorial_faq_tab': '在线 FAQ',
    'tutorial_step1_title': '选择世界存档',
    'tutorial_step1_desc': '支持直接导入 .zip 压缩包、.mcworld 格式或已解压的目录，内置扫描系统会自动寻找 level.dat 和数据库。',
    'tutorial_step2_title': '选择目标版本与平台',
    'tutorial_step2_desc': '支持 Java 1.8.8 ~ 1.21+ 与基岩版全系列互转，并可按需选择是否开启维度裁剪。',
    'tutorial_step3_title': '开始转换与后台监控',
    'tutorial_step3_desc': '转换由嵌入式 OpenJDK 17 沙箱执行，通知栏与界面实时呈现进度百分比与执行日志。',
    'tutorial_step4_title': '统一输出与管理',
    'tutorial_step4_desc': '转换完毕后文件存放于系统 Documents/chunkoid output 专区，支持一键导出到游戏。',
    'tutorial_group_title': '💬 官方交流反馈群',
    'tutorial_group_desc': '遇到未知错误或需特殊格式转换排错，欢迎加入官方交流群交流与反馈：群号 1103983368'
}

strings_ja = {
    'history_export_success': 'レスキューエクスポートに成功しました！',
    'history_export_fail': 'エクスポート失敗: %1$s',
    'history_title': '変換履歴',
    'history_back_desc': '戻る',
    'history_clear_desc': '履歴をクリア',
    'history_empty': '変換履歴がありません',
    'history_item_time': '時間: %1$s  所要時間: %2$d秒',
    'history_detail_title': 'アーカイブ詳細情報',
    'history_detail_name': '名前: %1$s',
    'history_detail_source': 'ソース: %1$s',
    'history_detail_target': 'ターゲット: %1$s',
    'history_detail_duration': '所要時間: %1$.1f 秒',
    'history_detail_export_uri_title': 'エクスポート場所 (URI):',
    'history_detail_not_exported': 'エクスポート場所: 未エクスポート',
    'history_close': '閉じる',
    'history_exporting': 'エクスポート中...',
    'history_export_fallback': 'アーカイブをレスキューエクスポート',
    
    'pruner_pruned_version_suffix': '%1$s (軽量化版)',
    'pruner_log_success': '[SUCCESS] マップの軽量化に成功しました！ 所要時間: %1$d 秒',
    'pruner_log_interrupted': '軽量化が中断されました: %1$s',
    'pruner_staging_unzipping': 'マップを解凍して分析中...',
    'pruner_error_archive_load': 'アーカイブの読み込みに失敗しました: %1$s',
    'pruner_staging_reading_world': 'ワールドデータを読み取り中...',
    'pruner_error_dir_import': 'ディレクトリのインポートに失敗しました: %1$s',
    'pruner_stage_starting_engine': '軽量化エンジンを起動中...',
    'pruner_log_starting': '[SYSTEM] ワールド軽量化とディメンション削減を開始します...',
    'pruner_log_cancelled': '[CANCEL] 軽量化はユーザーによりキャンセルされました。',

    'tutorial_faq_category': 'よくある質問',
    'tutorial_faq_q1': '変換後のフォルダにファイルが少なく、ゲームで読み込めない（level.dat/db/regionがない）？',
    'tutorial_faq_a1': '再度変換を行い、リアルタイムのログ出力を確認してください。通常、入力元の圧縮ファイル内に複数階層のフォルダがあるか、コアデータベースファイルが不足しているためです。アーカイブをフォルダに解凍してからインポートすることをお勧めします。',
    'tutorial_faq_q2': '変換時に Termux environment not initialized エラーが発生する？',
    'tutorial_faq_a2': 'これはRootFS環境が完全に解凍されていないか、クリーナーアプリにより破損していることを意味します。設定から「RootFSをリセット」を実行するか、ストレージ権限を再度付与してください。',
    'tutorial_faq_q3': '大きなワールドの変換中に、バックグラウンドにしたり画面を消すとアプリが突然終了する？',
    'tutorial_faq_a3': '一部のシステム（HyperOS, ColorOS, OriginOS, HarmonyOS等）はバックグラウンドのプロセスを厳しく制限します。設定で「Wakelock」を有効にし、バッテリーを「制限なし」に設定した上で、タスク一覧でアプリをロックしてください。',
    'tutorial_faq_q4': '低メモリ（4GB~6GB）の端末で大きなワールドを変換するとカクついたりクラッシュする？',
    'tutorial_faq_a4': '設定で「クラッシュ防止モード（低RAM最適化）」を有効にしてください。JVMの並行処理を制限し、Javaの最大ヒープメモリ割り当てを適切に引き下げます。',
    'tutorial_faq_q5': '「Original NBT is not available for this conversion」というエラーが出る？',
    'tutorial_faq_a5': '統合版とJava版でNBT構造が異なるため、バージョンによっては元のタグをそのまま保持できません。変換設定で「元のNBTを保持」をオフにしてから再度変換してください。',
    'tutorial_faq_q6': '網易版（Netease）マップの復号に失敗するか、dbフォルダが見つからない？',
    'tutorial_faq_a6': '網易版復号ツールはLevelDBのCURRENTファイルとMANIFESTファイルに依存しています。アプリのバックアップ親フォルダではなく、db/ フォルダを含むワールドルートディレクトリを選択してください。',
    'tutorial_flow_title': '標準的な変換ワークフロー',
    'tutorial_faq_title': '公式よくある質問とトラブルシューティング (FAQ)',
    'tutorial_wiki_title': 'Chunkoid 公式オンラインWiki・ドキュメント',
    'tutorial_wiki_desc': 'chunkoid.top · 公式の最新ドキュメントとマニュアルをリアルタイム同期',
    'tutorial_wiki_button': '公式Wikiドキュメントへ',
    'tutorial_manual_tab': 'マニュアル',
    'tutorial_faq_tab': 'オンラインFAQ',
    'tutorial_step1_title': '1. ワールドアーカイブを選択',
    'tutorial_step1_desc': '.zip、.mcworld 形式、または解凍済みフォルダの直接インポートをサポートします。スキャナーが自動的に level.dat とデータベースを探します。',
    'tutorial_step2_title': '2. ターゲットのバージョンとプラットフォームを選択',
    'tutorial_step2_desc': 'Java 1.8.8 ~ 1.21+ とすべての統合版（Bedrock）シリーズの相互変換をサポートします。ディメンションの軽量化も選択可能です。',
    'tutorial_step3_title': '3. 変換開始とバックグラウンド監視',
    'tutorial_step3_desc': '変換は組み込みのOpenJDK 17サンドボックスで実行されます。進捗とリアルタイムログはアプリと通知バーに表示されます。',
    'tutorial_step4_title': '4. 出力と管理',
    'tutorial_step4_desc': '変換されたファイルはシステムの Documents/chunkoid output フォルダに保存され、ゲームへのワンクリックエクスポートをサポートします。',
    'tutorial_group_title': '💬 公式サポートグループ',
    'tutorial_group_desc': '未知のエラーが発生した場合や特別なフォーマット変換に関するトラブルシューティングが必要な場合は、公式フィードバックグループに参加してください（QQ: 1103983368）。'
}

parse_and_add('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/res/values/strings.xml', strings_en)
parse_and_add('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/res/values-zh-rCN/strings.xml', strings_zh)
parse_and_add('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/res/values-ja/strings.xml', strings_ja)
