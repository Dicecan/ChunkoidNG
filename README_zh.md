<div align="center">
  <h1>Chunkoid NG</h1>
  <p><strong>Android 端 Minecraft 世界与资源包工具箱</strong></p>

  <p>
    <a href="README_zh.md">简体中文</a> | 
    <a href="README_en.md">English</a> | 
    <a href="https://chunkoid.top">官方网站</a>
  </p>

  <p>
    <img alt="Version" src="https://img.shields.io/badge/version-Beta%200.6-2ea043">
    <a href="LICENSE"><img alt="License" src="https://img.shields.io/badge/license-GPLv3-E6B800"></a>
    <img alt="Kotlin" src="https://img.shields.io/badge/language-Kotlin-7F52FF">
    <img alt="UI" src="https://img.shields.io/badge/UI-Jetpack%20Compose%20MD3E-4285F4">
    <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84">
    <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-Bedrock%2FJava-62B47A">
  </p>
</div>

---

## 项目背景

**Chunkoid NG (Next-Generation)** 是由 **EncoreTeam** 接手并基于现代化架构重构的 Android 端 Minecraft 世界与资源包工具。

- **传承与交接**：项目源自 **Dozener (DozenesStudio)** 独立开发的 Chunkoid。在其进入大学后，为保证项目持续维护，由 **DICECAN (EncoreTeam)** 接手继续开发。
- **开源协议**：本项目沿用并严格遵守 **[GNU General Public License v3.0 (GPLv3)](LICENSE)** 开源协议。
- **致谢奠基人**：在所有代码库、文档和软件关于界面中永久保留原作者 Dozener 的署名与致谢。

当前项目已由早期预览阶段进入 **Beta 0.6** 测试阶段，各功能模块均已实装可用，重点转向稳定性提升与机型性能调优。

---

## 主要功能

- **世界存档双向转换**：支持 Java 版与基岩版（BE）世界存档互转，提供版本映射、跨版本兼容性提示以及前台常驻服务保活，避免大世界转换被系统后台清理。
- **NBT 与 LevelDB 可视化编辑器**：支持解析 `.dat`、`.mca`、`level.dat`、LevelDB 数据库记录与整包存档；支持大数组分页浏览与数据字段语义标注。
- **2D 区块视图与存档瘦身**：网格化平移缩放查看区块分布，支持按维度（主世界/下界/末地）裁剪或清理未驻留废弃区块以压缩存档体积。
- **材质包双向转换**：支持 Java 版与基岩版材质包互转，自动处理模型、贴图映射及 MCMETA 动画纹理拼接。
- **MIDI 转红石音乐**：解析 MIDI 音轨并生成紧凑蛇形回路红石音符盒机械，支持直接导出超平坦世界或注入现有存档。
- **网易版存档支持**：基于 LevelDB 异或算法的存档解密与加密流转。
- **沙箱环境与硬件性能调优**：内置 OpenJDK 17 运行沙箱；根据设备内存与处理器性能提供自适应调优（低内存 / 均衡 / 激进 / 自定义），防止大型世界解析时发生 OOM。
- **独立崩溃诊断**：独立 `:crash` 进程接管致命异常，自动抓取设备信息与调用栈，支持日志安全导出与快速重启。

---

## 编译与构建

### 开发环境
- **Android Studio**：Ladybug (2024.2.1) 或更新版本
- **JDK**：OpenJDK 17 或 JDK 21
- **Android SDK**：API 35（最低支持 Android 8.0 / API 26）

### 构建命令
```bash
# 编译 Debug APK
./gradlew assembleDebug

# 执行单元测试
./gradlew testDebugUnitTest
```

---

## 开源协议与致谢

- **开源协议**：[GNU General Public License v3.0](LICENSE)
- **原作者与奠基人**：[Dozener (DozenesStudio)](mailto:dozener@outlook.com)
- **维护与重构团队**：[EncoreTeam](https://chunkoid.top) (DICECAN)
- **核心组件与算法参考**：
  - [The Hive - Chunker](https://github.com/HiveGamesOSS/Chunker) (MIT)
  - [HTMonkeyG - XOR-MC-Archive-Decrypt](https://github.com/HTMonkeyG/XOR-MC-Archive-Decrypt) (GPL-3.0)
  - [PowerNukkit - NBT-Manipulator](https://github.com/PowerNukkit/NBT-Manipulator) (MIT)
  - [iq80 - leveldb](https://github.com/dain/leveldb) (Apache-2.0)
  - [HiveGamesOSS - leveldb-mcpe-java](https://github.com/HiveGamesOSS/leveldb-mcpe-java) (Apache-2.0 / BSD)
