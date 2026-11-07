<div align="center">
  <h1>Chunkoid NG</h1>
  <p><strong>Android 端 Minecraft 世界与资源包工具箱</strong></p>

  <p>
    <a href="https://github.com/Dicecan/ChunkoidNG/releases"><img src="https://img.shields.io/github/v/release/Dicecan/ChunkoidNG?style=flat-square&color=2f7d63" alt="Release"></a>
    <img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Platform">
    <img src="https://img.shields.io/badge/Arch-arm64--v8a-blue?style=flat-square" alt="Arch">
    <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-orange?style=flat-square" alt="License"></a>
  </p>

  <p>
    <strong>简体中文</strong> | <a href="README_en.md">English</a>
  </p>
</div>

---

## 快速导航

- [项目背景](#项目背景)
- [主要功能](#主要功能)
- [运行环境要求](#运行环境要求)
- [构建说明](#构建说明)
- [开源协议与致谢](#开源协议与致谢)

---

## 项目背景

**Chunkoid NG (Next-Generation)** 是由 **EncoreTeam** 接手并基于现代化架构重构的 Android 端 Minecraft 世界与资源包工具。

- **传承与交接**：项目源自 **Dozener (DozenesStudio)** 独立开发的 Chunkoid。在其进入大学后，为保证项目持续维护，由 **DICECAN (EncoreTeam)** 接手继续开发。
- **开源协议**：本项目沿用并严格遵守 **[GNU General Public License v3.0 (GPLv3)](LICENSE)** 开源协议。
- **致谢奠基人**：在所有代码库、文档和软件关于界面中永久保留原作者 Dozener 的署名与致谢。

当前公开发布版本为 **Beta 0.7** 测试版（集成了全新的全量生物与实体智能迁移引擎及 MAID™ 女仆安全防御与资源治理底座）。

---

## 主要功能

- **世界存档双向转换**：支持 Java 版与基岩版（BE）世界存档互转，提供版本映射、跨版本兼容性提示以及前台常驻服务保活，避免大世界转换被系统后台清理。
- **全量生物与实体智能迁移（Beta 0.7 核心特性）**：独创全量生物与实体迁移流水线，支持 JE ↔ BE 双向及同平台跨版本迁移；完整保留村民交易表（`Offers.Recipes`）与附魔书，修复跨区块拴绳与载具挂接脱节，并自动过滤模组实体。
- **MAID™ 安全防御与资源治理（Beta 0.7 核心特性）**：全架构完整性与安全防御系统 **MAID™**（**M**anaged **A**rchitecture for **I**ntegrity & **D**efense，拟人化代称「女仆™」），统筹路径穿越（Zip-Slip）绝对阻断、归档解压多级配额、压缩炸弹熔断与二进制越界拦截。
- **资源超标温和截断（Gentle Truncation，Beta 0.7 核心特性）**：首创温和截断策略，对超大材质动画展开帧与海量 MIDI 音符自动修剪至设备安全承载上限，防止 OOM 内存溢出并记录审计日志。
- **女仆™ 巡检看板与实时审计（Beta 0.7 核心特性）**：在偏好设置中提供一键式女仆™看板弹窗，直观呈现设备硬件算力评分、ART 堆内存水位、生效运行配额与 100 条定长无锁环形审计事件流，支持一键导出完整诊断报告。
- **NBT 与 LevelDB 可视化编辑器**：支持解析 `.dat`、`.mca`、`level.dat`、LevelDB 数据库记录与整包存档；支持大数组分页浏览与数据字段语义标注。
- **2D 区块视图与存档瘦身**：网格化平移缩放查看区块分布，支持按维度（主世界/下界/末地）裁剪或清理未驻留废弃区块以压缩存档体积。
- **材质包双向转换**：支持 Java 版与基岩版材质包互转，自动处理模型、贴图映射及 MCMETA 动画纹理拼接。
- **MIDI 转红石音乐**：解析 MIDI 音轨并生成紧凑蛇形回路红石音符盒机械，支持直接导出超平坦世界或注入现有存档。
- **网易版存档支持**：基于 LevelDB 异或算法的存档解密与加密流转。
- **沙箱环境与硬件性能调优**：内置 OpenJDK 17 运行沙箱；提供自适应调优（低内存 / 均衡 / 激进 / 自定义），动态钳制 JVM 堆内存与并发数，防止大型世界解析时发生 OOM。
- **独立崩溃诊断**：独立 `:crash` 进程接管致命异常，自动抓取设备信息、安全审计记录与调用栈，支持日志安全导出与快速重启。

---

## 运行环境要求

- **操作系统**：Android 8.0（API 27）或更高版本
- **处理器架构**：仅支持 64 位 ARM（`arm64-v8a`）
- **推荐内存**：建议 4GB 及以上物理运存（大型 Java 世界转换建议 6GB+）
- **存储空间**：首次解压需预留约 100MB 空间用于内置 OpenJDK 运行时

---

## 构建说明

本项目使用现代 Android 技术栈：
- **Kotlin** 2.0+
- **Jetpack Compose** (Material Design 3)
- **NDK**：LevelDB JNI 绑定与原生优化

在终端中执行以下命令构建 Debug 安装包：

```bash
./gradlew assembleDebug
```

> **注意**：首次编译将自动下载并解压预编译的 aarch64 OpenJDK 与 Chunker CLI 二进制环境。

---

## 开源协议与致谢

- 本项目采用 **GNU General Public License v3.0** 开源，详情请参见 [LICENSE](LICENSE)。
- 转换核心基于 [Chunker CLI](https://github.com/HiveGames/Chunker)（The Hive，基于 MIT 协议开源）。
- 衷心感谢原作者 **Dozener** 为本项目做出的卓越奠基与无私开源精神。
