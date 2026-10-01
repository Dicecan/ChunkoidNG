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
> 本工程正处于从原版 Chunkoid 向现代架构的全面移植与重构阶段。当前已实装基础 OpenJDK 17 沙箱环境、终端控制台、Minecraft 世界存档双向转换与导出管理、智能版本升降级基础映射与兼容风险评估、规范化中/英/日多语言架构体系、网易版存档加解密与被动加密、独立维度裁剪与存档瘦身工作台，以及全能 NBT / LevelDB 可视化编辑器与 2D 区块栅格瘦身工具。

---

## 🌟 NG (Next-Generation) 重构特性

* 📦 **全能 NBT / LevelDB 可视化编辑器**：支持单文件 NBT (`.dat`, `.nbt`, `level.dat`)、MCA 区域文件 (`.mca`, `.mcr`)、LevelDB 数据库 (`db/` 目录) 与整包存档 (`.zip`, `.mcworld`) 解析；内置纯 Java LevelDB 驱动与 SafeEnv（规避 Android mmap 闪退）；提供完整 NBT 树编辑（增删改查、键名重命名、路径复制）与 Bedrock 2D 区块栅格交互视图（点选/框选区块、InhabitedTime 停滞时间计算与 0-tick 废区块智能瘦身）。
* 🗺️ **版本升降级基础映射系统**：引入 Minecraft 语义版本模型与跨平台基线映射表（JE ↔ BE），支持对端同版等价智能推荐；内置版本关系分析器与高风险预警（1.18 负高度截断、1.13 方块扁平化兼容提示）。
* 🌐 **规范化多语言体系 (i18n)**：全盘规范 Android 资源标准（默认英文兜底、简体中文、日本語），UI 全量动态读取，支持应用内免重启即时动态切换语言。
* 🎨 **Jetpack Compose (MD3E)**：全盘基于 Material Design 3 Expressive 规范构建，统一 Squircle 交互容器、全新版本筛选底栏与自适应防折行排版。
* ⚡ **极简 RootFS 沙箱**：大幅精简无用依赖，部署文件减少至 57 个，解压体积减半。
* 🖥️ **沙箱终端控制台**：内置环境自检与交互终端，支持调试 OpenJDK 17 与 Chunker CLI。
* 🌍 **全功能世界转换器**：支持 Java/基岩版双向转换、跨版本升降级、前台服务防杀保活、转换历史管理与自定义路径导出。
* ✂️ **独立存档瘦身与维度裁剪**：提供仅保留主世界、极速轻量化、全维度保留等预设策略，支持各维度独立裁剪与 `-k` 原始 NBT 保留。
* 🔐 **网易版存档还原与被动加密**：基于 LevelDB 异或算法流式解密，支持魔数完整性校验，并提供网易版被动加密与一键流转世界转换。
* 🔤 **全局字符编码规范**：全链路采用标准 UTF-8 字符集，根治多语言设置项与运行日志中的中文字符乱码隐患。

---

## 🚀 核心功能规划 (Canary 进度)

* ✅ **沙箱终端控制台**：`已实装`（支持 OpenJDK 17 与 Shell 命令执行）
* ✅ **世界存档双向互转**：`已实装`（支持 Java/基岩版互转、跨版本选择、前台服务转换、历史记录与补救导出）
* ✅ **独立维度裁剪与存档瘦身**：`已实装`（独立存档瘦身页面，提供全维度/仅主世界/极速轻量化预设、各维度自定义开关与 `-k` 原始 NBT 保留）
* ✅ **网易版存档还原与被动加密**：`已实装`（集成 LevelDB 异或流式解密、魔数校验与被动加密，支持无缝流转至世界转换器）
* ✅ **版本升降级映射与兼容风险评估**：`已实装 (Canary 0.3)`（语义版本模型、等价基线推荐、1.18/1.13 风险告警）
* ✅ **规范化多语言架构 (ZH / EN / JA)**：`已实装 (Canary 0.3)`（完整中英日文资源、应用内动态语言切换）
* ✅ **专业 NBT / LevelDB 编辑**：`已实装 (Canary 0.4)`（NBT 树结构交互、LevelDB 记录分类检索、2D 区块栅格可视化与精准瘦身）
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

* **原作者与奠基人**：Dozener (DozenesStudio)
* **当前维护与重构团队**：DICECAN (EncoreTeam)
* **核心贡献者**：Ryan Steven、Dozener
* **第三方开源组件与算法参考**：
  * [The Hive - Chunker](https://github.com/HiveGamesOSS/Chunker) (MIT License)
  * [HTMonkeyG - XOR-MC-Archive-Decrypt](https://github.com/HTMonkeyG/XOR-MC-Archive-Decrypt) (GPL-3.0)
  * [PowerNukkit - NBT-Manipulator](https://github.com/PowerNukkit/NBT-Manipulator) (MIT License)
  * [HiveGamesOSS - leveldb-mcpe-java](https://github.com/HiveGamesOSS/leveldb-mcpe-java) (Apache-2.0 / BSD)
  * [Dicecan - NetEaseDecryptorSDK](https://github.com/Dicecan/NetEaseDecryptorSDK) (GPL-3.0)
