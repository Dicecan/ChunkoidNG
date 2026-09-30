import re

def replace_in_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        if old not in content:
            print(f"WARNING: '{old[:20]}...' not found!")
        content = content.replace(old, new)
        
    # Check for imports
    if 'import com.noches.chunkoidng.R' not in content:
        content = content.replace('import androidx.compose.ui.res.painterResource', 'import androidx.compose.ui.res.painterResource\nimport androidx.compose.ui.res.stringResource\nimport com.noches.chunkoidng.R')

    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

replacements = [
    ('val category: String = "常见问题"', 'val category: String = stringResource(R.string.tutorial_faq_category)'),
    ('question = "打开转换后的文件夹文件很少，游戏无法读取（没有 level.dat/db/region）？",', 'question = stringResource(R.string.tutorial_faq_q1),'),
    ('answer = "请重新转换一次，并在转换时观察实时日志流输出。通常是因为输入源的压缩包内存在多层文件夹嵌套，或源世界缺少核心数据库指针文件。建议直接将存档解压为文件夹后再选取导入。"', 'answer = stringResource(R.string.tutorial_faq_a1)'),
    ('question = "转换时出现 Termux environment not initialized 错误？",', 'question = stringResource(R.string.tutorial_faq_q2),'),
    ('answer = "这表示应用的 RootFS 运行环境未完全解压或被安全清理软件破坏。请到【设置】中心点击【重置并重新初始化沙箱环境】，或重新授予应用完整的存储权限。"', 'answer = stringResource(R.string.tutorial_faq_a2)'),
    ('question = "转换大存档时，切到后台或者息屏后软件突然中断退出？",', 'question = stringResource(R.string.tutorial_faq_q3),'),
    ('answer = "部分手机厂商系统（如 HyperOS、ColorOS、OriginOS、HarmonyOS）后台策略较激进。请在【设置】中开启【后台唤醒锁 (WakeLock)】，并将 Chunkoid 的电池策略设为【无限制/允许后台高耗电运行】，同时在多任务界面锁定软件卡片。"', 'answer = stringResource(R.string.tutorial_faq_a3)'),
    ('question = "手机运存较小（4GB~6GB），转换大世界容易卡顿或闪退？",', 'question = stringResource(R.string.tutorial_faq_q4),'),
    ('answer = "请在【设置】中开启【防闪退模式（低运存优化）】，系统会自动向 JVM 注入串行 GC 与单线程并发限制参数，并适当调低 Java 虚拟机最大分配内存。"', 'answer = stringResource(R.string.tutorial_faq_a4)'),
    ('question = "提示 Original NBT is not available for this conversion 错误？",', 'question = stringResource(R.string.tutorial_faq_q5),'),
    ('answer = "由于跨平台转换时，基岩版与 Java 版的 NBT 结构定义不一致，部分版本无法直接继承未转换的原始标签。请在转换设置中关闭【保留原始 NBT】即可顺利完成转换。"', 'answer = stringResource(R.string.tutorial_faq_a5)'),
    ('question = "网易版地图解密失败或者找不到 db 文件夹？",', 'question = stringResource(R.string.tutorial_faq_q6),'),
    ('answer = "网易存档解密器依赖 LevelDB 的 CURRENT 指针文件与 MANIFEST 文件。请确保选中的是包含 db/ 文件夹的世界根目录，而非外部的应用备份父级目录。"', 'answer = stringResource(R.string.tutorial_faq_a6)'),

    ('text = "世界转换标准流程",', 'text = stringResource(R.string.tutorial_flow_title),'),
    ('text = "官网常见问题排错 (FAQ)",', 'text = stringResource(R.string.tutorial_faq_title),'),
    ('text = "Chunkoid 官方在线 Wiki 文档站",', 'text = stringResource(R.string.tutorial_wiki_title),'),
    ('text = "chunkoid.top · 实时同步官方最新文档与操作手册",', 'text = stringResource(R.string.tutorial_wiki_desc),'),
    ('Text("前往官方在线 Wiki 文档站 (Docs)")', 'Text(stringResource(R.string.tutorial_wiki_button))'),
    ('Text("操作手册", fontSize = 12.sp)', 'Text(stringResource(R.string.tutorial_manual_tab), fontSize = 12.sp)'),
    ('Text("在线 FAQ", fontSize = 12.sp)', 'Text(stringResource(R.string.tutorial_faq_tab), fontSize = 12.sp)'),

    ('title = "选择世界存档",', 'title = stringResource(R.string.tutorial_step1_title),'),
    ('description = "支持直接导入 .zip 压缩包、.mcworld 格式或已解压的目录，内置扫描系统会自动寻找 level.dat 和数据库。"', 'description = stringResource(R.string.tutorial_step1_desc)'),
    ('title = "选择目标版本与平台",', 'title = stringResource(R.string.tutorial_step2_title),'),
    ('description = "支持 Java 1.8.8 ~ 1.21+ 与基岩版全系列互转，并可按需选择是否开启维度裁剪。"', 'description = stringResource(R.string.tutorial_step2_desc)'),
    ('title = "开始转换与后台监控",', 'title = stringResource(R.string.tutorial_step3_title),'),
    ('description = "转换由嵌入式 OpenJDK 17 沙箱执行，通知栏与界面实时呈现进度百分比与执行日志。"', 'description = stringResource(R.string.tutorial_step3_desc)'),
    ('title = "统一输出与管理",', 'title = stringResource(R.string.tutorial_step4_title),'),
    ('description = "转换完毕后文件存放于系统 Documents/chunkoid output 专区，支持一键导出到游戏。"', 'description = stringResource(R.string.tutorial_step4_desc)'),

    ('text = "💬 官方交流反馈群",', 'text = stringResource(R.string.tutorial_group_title),'),
    ('text = "遇到未知错误或需特殊格式转换排错，欢迎加入官方交流群交流与反馈：群号 1103983368",', 'text = stringResource(R.string.tutorial_group_desc),')
]
replace_in_file('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/java/com/noches/chunkoidng/ui/screens/tutorial/TutorialScreen.kt', replacements)

# Handle val category: String in composable? Wait, `val category` is in a ViewModel or remember block?
# Let's fix that. If `category` doesn't have composable context, we might need a workaround.
# But it's in a remember block or just a list? Let's check where `category` is used.
