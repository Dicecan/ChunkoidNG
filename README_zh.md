# Chunkoid NG (Next-Generation) 简体中文说明

[English Documentation](README_en.md) | [返回主页](README.md) | [官方网站](https://chunkoid.top)

---

## 📢 项目背景与 EncoreTeam Recode 声明

**Chunkoid NG (Next-Generation)** 是由 **DICECAN (EncoreTeam)** 接手并主导的现代重构版本，其前身为独立开发者 **Dozener (DozenesStudio)** 原创开发的 **Chunkoid**。

* **项目传承**：原作者 Dozener 同学在个人业余时间独立探索，开发了首个无需电脑、完全基于 Android 端运行的 Minecraft 世界格式转换器。在 Dozener 开启大学生活之际，为避免工具生态停滞，正式将全套项目资产与知识产权移交至 DICECAN (EncoreTeam) 维护。
* **协议承诺**：项目核心遵循原有的 **GNU General Public License v3.0 (GPLv3)** 开源协议，保持完全免费、开源。
* **奠基人致谢**：在此重构版本及未来的每一次发布中，团队将永久保留原作者 Dozener 的署名与致谢。

---

## 💡 为什么需要 NG (Next-Generation) 重构？

原版 Chunkoid 奠定了移动端世界转换的坚实可行性基础，但受早期开发技术栈与架构所限，存在一些历史痛点：
1. **旧版 Android 视图卡顿**：多处使用传统 XML 布局与复杂 Fragment/Activity 架构。
2. **多线程调度脆弱**：底层进程与 Java 调用充斥着同步阻塞等待，容易在极端设备上产生 ANR。
3. **环境体积与碎片繁杂**：旧版 Linux RootFS 包含过多的冗余支持库（146 个碎文件），首次解压慢且占用大量内部存储。
4. **缺少可视化交互控制**：用户对底层 OpenJDK 17 的运行状态无法直观掌握。

> [!WARNING]
> ### 🚧 当前处于 Canary 早期开发预览阶段
> 本工程正处于从原版 Chunkoid 向现代架构的全面移植与重构阶段，**大部分核心业务功能仍在待开发/移植中**。当前仅实装了基础的 OpenJDK 17 沙箱运行环境与终端控制台。

---

## 🌟 NG (Next-Generation) 重构特性

* **Jetpack Compose (MD3E)**：全盘基于 Material Design 3 Expressive 规范构建。
* **极简 RootFS 沙箱**：大幅精简无用依赖，部署文件减少至 57 个，解压体积减半。
* **沙箱终端控制台**：内置环境自检与交互终端，支持调试 OpenJDK 17 与 Chunker CLI。

---

## 🚀 核心功能规划 (Canary 进度)

1. ✅ **沙箱终端控制台**：`已实装`（支持 OpenJDK 17 与 Shell 交互调试）
2. ⏳ **Minecraft 世界存档双向转换**：`待开发`（待从原版 Chunkoid 移植核心引擎）
3. ⏳ **世界版本升降级与维度裁剪**：`待开发`（待移植）
4. ⏳ **网易版加密存档提取解密**：`待开发`（待移植 LevelDB 异或算法）
5. ⏳ **NBT / LevelDB 结构查看与编辑**：`待开发`（待移植）
6. ⏳ **材质与资源包互转**：`待开发`（待移植）

---

## 🛠️ 构建与开发指南

### 环境要求
* **Android Studio**：Ladybug (2024.2.1) 或更高版本
* **JDK**：OpenJDK 17 或 JDK 21
* **Android SDK**：Min SDK 26 (Android 8.0)，Target SDK 34 (Android 14)
* **Gradle**：8.9+

### 编译 Release 版本
```bash
./gradlew assembleRelease
```
产物位于 `app/build/outputs/apk/release/app-release.apk`。

---

## 📜 开源协议

本项目采用 [GNU General Public License v3.0 (GPLv3)](LICENSE) 授权。

---

## 🤝 致谢与版权归属

* **原作者与奠基人**：Dozener (DozenesStudio)
* **当前维护团队**：DICECAN (EncoreTeam)
* **核心贡献者**：Ryan Steven、Dozener
* **核心依赖项目**：
  * [The Hive - Chunker](https://github.com/HiveGamesOSS/Chunker) (MIT)
  * [PowerNukkit - NBT-Manipulator](https://github.com/PowerNukkit/NBT-Manipulator) (MIT)
  * [HiveGamesOSS - leveldb-mcpe-java](https://github.com/HiveGamesOSS/leveldb-mcpe-java) (Apache-2.0 / BSD)
  * [Dicecan - NetEaseDecryptorSDK](https://github.com/Dicecan/NetEaseDecryptorSDK) (GPL-3.0)
