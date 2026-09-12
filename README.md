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

## 🌟 NG (Next-Generation) 全新重构特性

* 🎨 **Material Design 3 Expressive (MD3E) 界面**：抛弃旧版传统 XML 布局，采用最新 Jetpack Compose 进行全盘重写，带来丝滑非线性页面转场、动态色彩与极客瀑布流卡片。
* ⚡ **深度精简 RootFS 沙箱**：沙箱文件数由原版的 146 个锐减至 **57 个**，解压提速 300%，冷启动体积降低近半，秒级部署 OpenJDK 17 aarch64 运行环境。
* 🖥️ **沉浸式极客终端控制台 (Console)**：
  * 支持在手机端直接交互执行 Shell、OpenJDK 17 与 Chunker CLI 指令。
  * 自动适配 Android 10+ `W^X` 权限限制与动态链接器调用。
  * 全选词自由复制、一键复制全部输出、高亮语法配色与微批处理流畅防掉帧。
* 🧵 **纯响应式异步流驱动**：底层进程与日志全盘采用 **Kotlin Coroutines + Flow** 重写，彻底根除旧版线程卡死与主线程阻塞问题。

---

## 🚀 核心功能

* 🔄 **世界存档双向互转**：基岩版 (BE) ↔ Java 版 (JE) 无损互转，支持 Minecraft 1.8.8+ 至 1.21+。
* 📦 **版本升降级与维度裁剪**：支持旧世界升级、高版本降级与无效维度数据修剪。
* 🔓 **网易版加密存档还原**：内置独家 LevelDB 指针异或流式解密算法。
* 🛠️ **专业 NBT / LevelDB 查看**：层级树形可视化查看与键值编辑。
* 🎨 **材质资源包互转**：JE 与 BE 材质包格式双向转换。

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
* **核心贡献者**：Ryan Steven、Dozener、Weiyin 1A
* **第三方开源组件**：
  * [The Hive - Chunker](https://github.com/HiveGamesOSS/Chunker) (MIT License)
  * [PowerNukkit - NBT-Manipulator](https://github.com/PowerNukkit/NBT-Manipulator) (MIT License)
  * [HiveGamesOSS - leveldb-mcpe-java](https://github.com/HiveGamesOSS/leveldb-mcpe-java) (Apache-2.0 / BSD)
  * [Dicecan - NetEaseDecryptorSDK](https://github.com/Dicecan/NetEaseDecryptorSDK) (GPL-3.0)
