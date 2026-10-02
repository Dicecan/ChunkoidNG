<div align="center">
  <h1>Chunkoid NG (Next-Generation)</h1>
  <p><strong>EncoreTeam 对 Chunkoid 的全新现代化重构工程</strong></p>

  <p>
    <a href="README_zh.md"><strong>简体中文 (Chinese)</strong></a> | 
    <a href="README_en.md"><strong>English (英文)</strong></a> | 
    <a href="https://chunkoid.top"><strong>官方网站</strong></a>
  </p>

  <p>
    <img alt="Version" src="https://img.shields.io/badge/version-CANARY%200.4-2ea043">
    <a href="LICENSE"><img alt="License" src="https://img.shields.io/badge/license-GPLv3-E6B800"></a>
    <img alt="Kotlin" src="https://img.shields.io/badge/language-Kotlin-7F52FF">
    <img alt="UI" src="https://img.shields.io/badge/UI-Jetpack%20Compose%20MD3E-4285F4">
    <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84">
    <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-Bedrock%2FJava-62B47A">
  </p>
</div>

---

> [!IMPORTANT]
> ### 📢 项目背景与 EncoreTeam Recode 声明
> 
> **Chunkoid NG (Next-Generation)** 是 **EncoreTeam** 对 **Dozener (DozenesStudio)** 独立原创开发的 Android 端 Minecraft 世界转换工具 **Chunkoid** 的全面现代化重构版（Recode）。
> 
> * **项目交接与授权**：原作者 Dozener 同学在进入大学之际，为使项目持续保持活力与维护，正式无偿交接给 **DICECAN (EncoreTeam)** 继续演进。
> * **开源协议不变**：本项目完全沿用并严格遵守原版的 **[GNU General Public License v3.0 (GPLv3)](LICENSE)** 协议。
> * **永久保留署名与致谢**：在所有代码库、文档、发布版与软件关于页面中，永久保留原作者 Dozener 的署名与奠基人致谢！

---

> [!WARNING]
> ### 🚧 当前处于 Canary 0.4 开发预览阶段
> 本工程正处于从原版 Chunkoid 向现代架构的全面移植与重构阶段。当前已实装基础 OpenJDK 17 沙箱环境、终端控制台、Minecraft 世界存档双向转换与导出管理、智能版本升降级基础映射与兼容风险评估、规范化中/英/日多语言架构体系、网易版存档加解密与被动加密、独立维度裁剪与存档瘦身工作台，以及全能 NBT / LevelDB 可视化编辑器、智能数据语义标签分析、大数据分页编辑与 2D 区块栅格精准瘦身工具。

---

## 🌟 NG (Next-Generation) 核心特性

* 📦 **全能 NBT / LevelDB 可视化编辑器**：
  * **全格式解析兼容**：支持单文件 NBT (`.dat`, `.nbt`, `level.dat`)、MCA 区域文件 (`.mca`, `.mcr`)、LevelDB 数据库 (`db/` 目录) 以及整包存档 (`.zip`, `.mcworld`) 快速解析与结构树展开。
  * **智能语义分析与作用标签**：内置 Minecraft 语义描述引擎，自动识别玩家属性（生命、饥饿、经验、背包）、实体/方块实体（箱子内容、熔炉燃烧进度、告示牌文字）与世界核心规则，提供直观的中英双语作用标签与数值合法范围修改引导，大幅降低新手门槛。
  * **大数据分页与防卡顿优化**：针对大型 `ByteArray`, `IntArray`, `LongArray`（如生物群系网格、高度图、调色板）实现动态虚拟分页加载与十六进制/十进制展示，杜绝大数组展开时的 UI 卡顿与 OOM 内存崩溃。
  * **高稳定性纯 Java LevelDB 引擎**：补充原生 ZLib 压缩桥接，规避 Android JNI 内存映射 (mmap) 闪退痛点，确保大型基岩版世界安全秒开与原子化安全落盘。
* 🗺️ **2D 区块栅格可视化与精准瘦身**：
  * **流式区块网格视口**：支持自由平移缩放、点选与多选矩形框选，直观标定世界区块分布与维度位置。
  * **InhabitedTime 停滞分析与 0-Tick 清理**：自动分析区块玩家滞留时间，精准标记 0-tick 废弃区块，支持一键安全清理并大幅压缩存档体积。
* 🔄 **全功能世界转换器 (World Converter)**：
  * **双向无损互转**：支持 Java 版 (JE) 与基岩版 (BE) 世界双向转换，兼容自定义目标版本。
  * **版本升降级基础映射体系**：跨平台同代版本等价推荐（如 JE 1.20 ↔ BE 1.20），内置版本关系分析器，智能预警 1.18 负高度截断与 1.13 方块扁平化兼容风险。
  * **前台服务守护保活**：转换长任务搭载 Android 前台保活通知服务，后台运行防系统杀进程，提供转换历史管理与容灾补救导出。
* ✂️ **独立存档瘦身与维度裁剪 (Dimension Pruner)**：
  * 提供「仅保留主世界」、「极速轻量化」、「全维度保留」等灵活预设，支持主世界、下界、末地独立开关与 `-k` 原始 NBT 保留机制。
* 🔐 **网易版存档还原与被动加密 (NetEase Cryptor)**：
  * 基于 LevelDB 异或算法流式解密，支持魔数完整性校验，并提供网易版存档被动加密与一键流转世界转换。
* 🌐 **规范化多语言体系 (i18n)**：
  * 完整中/英/日（ZH / EN / JA）多语言资源体系，全链路 UTF-8 编码，支持应用内免重启即时动态切换。
* 🎨 **Jetpack Compose 现代化交互 (MD3E)**：
  * 全盘基于 Material Design 3 Expressive 规范构建，适配 Squircle 容器交互、动态自适应排版、顶部状态栏安全边距适配与卡片平滑折叠，最大化操作视野。
* ⚡ **极简 RootFS 沙箱与终端控制台**：
  * 大幅精简无用依赖，部署文件精简至 57 个，体积减半；内置终端控制台支持 OpenJDK 17 与 Shell 调试。

---

## 🚀 核心功能规划 (Canary 进度)

* ✅ **沙箱终端控制台**：`已实装`（支持 OpenJDK 17 与 Shell 命令执行）
* ✅ **世界存档双向互转**：`已实装`（支持 Java/基岩版互转、跨版本选择、前台服务转换、历史记录与补救导出）
* ✅ **独立维度裁剪与存档瘦身**：`已实装`（独立存档瘦身页面，提供全维度/仅主世界/极速轻量化预设、各维度自定义开关与 `-k` 原始 NBT 保留）
* ✅ **网易版存档还原与被动加密**：`已实装`（集成 LevelDB 异或流式解密、魔数校验与被动加密，支持无缝流转至世界转换器）
* ✅ **版本升降级映射与兼容风险评估**：`已实装 (Canary 0.3)`（语义版本模型、等价基线推荐、1.18/1.13 风险告警）
* ✅ **规范化多语言架构 (ZH / EN / JA)**：`已实装 (Canary 0.3)`（完整中英日文资源、应用内动态语言切换）
* ✅ **专业 NBT / LevelDB 可视化编辑器**：`已实装 (Canary 0.4)`（全格式解析、NBT 树结构增删改查、大数组分页编辑、LevelDB 记录分类检索）
* ✅ **Minecraft 智能语义分析与作用标签**：`已实装 (Canary 0.4)`（玩家/实体/方块实体智能语义识别、新手门槛优化、安全修改引导）
* ✅ **2D 区块栅格可视化与精准瘦身**：`已实装 (Canary 0.4)`（点选/框选区块、InhabitedTime 停滞分析、0-tick 废区块一键清理）
* ⏳ **材质资源包互转**：`待开发`（待移植）

---

## 🛠️ 技术栈

* **开发语言**：Kotlin 2.0+
* **UI 框架**：Jetpack Compose (Material Design 3 Expressive)
* **异步与流**：Kotlinx Coroutines & Flow
* **沙箱运行时**：OpenJDK 17 (aarch64 Linux RootFS)
* **核心转换引擎**：[chunker-cli](https://github.com/HiveGamesOSS/Chunker) (The Hive - MIT License)

---

## 📜 开源协议

本项目采用 **[GNU General Public License v3.0](LICENSE)** 协议开源。

---

## 🤝 致谢与贡献

* **原作者与奠基人**：Dozener (DozenesStudio) (<dozener@outlook.com>)
* **当前维护与重构团队**：DICECAN (EncoreTeam)
* **核心贡献者**：Ryan Steven、Dozener
* **第三方开源组件与算法参考**：
  * [The Hive - Chunker](https://github.com/HiveGamesOSS/Chunker) (MIT License)
  * [HTMonkeyG - XOR-MC-Archive-Decrypt](https://github.com/HTMonkeyG/XOR-MC-Archive-Decrypt) (GPL-3.0)
  * [PowerNukkit - NBT-Manipulator](https://github.com/PowerNukkit/NBT-Manipulator) (MIT License)
  * [iq80 - leveldb](https://github.com/dain/leveldb) (Apache-2.0)
  * [HiveGamesOSS - leveldb-mcpe-java](https://github.com/HiveGamesOSS/leveldb-mcpe-java) (Apache-2.0 / BSD)
  * [Dicecan - NetEaseDecryptorSDK](https://github.com/Dicecan/NetEaseDecryptorSDK) (GPL-3.0)
