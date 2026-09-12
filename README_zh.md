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

**Chunkoid NG 带来了彻底的降维革新：**
* **全面拥抱 Jetpack Compose**：采用最前沿的 Material Design 3 Expressive (MD3E) 设计规范，瀑布流卡片自适应，丝滑非线性动效。
* **极限精简的 RootFS 2.0**：文件数量从 146 骤减到 **57 个**，解压时间提速 300%，存储占用大幅减少。
* **极客沉浸式沙箱控制台 (Console)**：内置全功能交互终端，支持在移动端直接输入 Shell/Java 命令，完美适配 Android 10+ W^X 权限与 linker 动态加载，支持选词复制与微批节流防掉帧。
* **响应式 Flow 驱动**：从解压进度、日志收集到转换状态监控，全部重写为纯非阻塞的 `Flow<T>`。

---

## 🚀 核心功能列表

1. **Minecraft 世界存档双向转换**：
   * 支持基岩版（Bedrock / PE）与 Java 版（JE）双向互转。
   * 支持从 1.8.8 至 1.21+ 全系主流游戏版本。
2. **世界版本升降级**：
   * 支持高版本 Java 存档降级适配旧客户端，或老版本基岩存档升级。
3. **网易版加密存档提取解密**：
   * 搭载自主实现的 LevelDB 指针异或算法，自动探测并还原加密存档。
4. **NBT / LevelDB 结构查看与编辑**：
   * 树状层级查看世界核心数据标签。
5. **材质与资源包互转**：
   * JE 材质包与 BE 材质包格式双向转换。

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
* **核心贡献者**：Ryan Steven、Dozener、Weiyin 1A
* **核心依赖项目**：
  * [The Hive - Chunker](https://github.com/HiveGamesOSS/Chunker) (MIT)
  * [PowerNukkit - NBT-Manipulator](https://github.com/PowerNukkit/NBT-Manipulator) (MIT)
  * [HiveGamesOSS - leveldb-mcpe-java](https://github.com/HiveGamesOSS/leveldb-mcpe-java) (Apache-2.0 / BSD)
  * [Dicecan - NetEaseDecryptorSDK](https://github.com/Dicecan/NetEaseDecryptorSDK) (GPL-3.0)
