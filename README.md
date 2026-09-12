<div align="center">
  <h1>Chunkoid NG (Next-Generation)</h1>
  <p><strong>EncoreTeam 对 Chunkoid 的全新现代化重构工程</strong></p>

  <p>
    <a href="README_zh.md"><strong>简体中文 (Chinese)</strong></a> | 
    <a href="README_en.md"><strong>English (英文)</strong></a> | 
    <a href="https://chunkoid.top"><strong>官方网站</strong></a>
  </p>

  <p>
    <img alt="Version" src="https://img.shields.io/badge/version-2.2.0--NG-2ea043">
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
> ### 🚧 当前处于 Canary 早期开发预览阶段
> 本工程正处于从原版 Chunkoid 向现代架构的全面移植与重构阶段，**大部分核心业务功能仍在待开发/移植中**。当前仅实装了基础的 OpenJDK 17 沙箱运行环境与终端控制台。

---

## 🌟 NG (Next-Generation) 重构特性

* 🎨 **Jetpack Compose (MD3E)**：全盘基于 Material Design 3 Expressive 规范构建。
* ⚡ **极简 RootFS 沙箱**：大幅精简无用依赖，部署文件减少至 57 个，解压体积减半。
* 🖥️ **沙箱终端控制台**：内置环境自检与交互终端，支持调试 OpenJDK 17 与 Chunker CLI。

---

## 🚀 核心功能规划 (Canary 进度)

* ✅ **沙箱终端控制台**：`已实装`（支持 OpenJDK 17 与 Shell 命令执行）
* ⏳ **世界存档双向互转**：`待开发`（待从原版 Chunkoid 移植核心引擎）
* ⏳ **版本升降级与维度裁剪**：`待开发`（待移植）
* ⏳ **网易版加密存档还原**：`待开发`（待移植 LevelDB 异或算法）
* ⏳ **专业 NBT / LevelDB 编辑**：`待开发`（待移植）
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
* **第三方开源组件**：
  * [The Hive - Chunker](https://github.com/HiveGamesOSS/Chunker) (MIT License)
  * [PowerNukkit - NBT-Manipulator](https://github.com/PowerNukkit/NBT-Manipulator) (MIT License)
  * [HiveGamesOSS - leveldb-mcpe-java](https://github.com/HiveGamesOSS/leveldb-mcpe-java) (Apache-2.0 / BSD)
  * [Dicecan - NetEaseDecryptorSDK](https://github.com/Dicecan/NetEaseDecryptorSDK) (GPL-3.0)
