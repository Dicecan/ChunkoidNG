<div align="center">
  <h1>Chunkoid NG (Next-Generation)</h1>
  <p><strong>EncoreTeam's Modern Architectural Recode of Chunkoid</strong></p>

  <p>
    <a href="README_zh.md"><strong>简体中文 (Chinese)</strong></a> | 
    <a href="README_en.md"><strong>English (英文)</strong></a> | 
    <a href="https://chunkoid.top"><strong>Official Website</strong></a>
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

## 📢 Project Origin & EncoreTeam Recode Statement

**Chunkoid NG (Next-Generation)** is a comprehensive, modern recode spearheaded by **DICECAN (EncoreTeam)**, succeeding the original **Chunkoid** application created by independent developer **Dozener (DozenesStudio)**.

* **Project Transition**: The original author, Dozener, explored and created the very first PC-free, Android-native Minecraft world conversion utility in his spare time. As Dozener embarked on his university journey, he graciously and unconditionally transferred the full project assets and IP to DICECAN (EncoreTeam) to ensure continuous maintenance and future development.
* **License Unchanged**: The project remains strictly open-source and free under the original **[GNU General Public License v3.0 (GPLv3)](LICENSE)**.
* **Founder Attribution**: We deeply respect Dozener's pioneering contributions; his attribution and acknowledgments will be **permanently honored** across all repositories, documentations, and application releases.

---

> [!WARNING]
> ### 🚧 Early Canary 0.4 Preview Phase
> This project is currently undergoing active migration and refactoring from legacy Chunkoid. Foundational OpenJDK 17 sandbox runtime, interactive terminal, bidirectional Minecraft world conversion & export, smart version upgrade/downgrade mapping & risk alerts, standardized multi-language architecture (ZH/EN/JA), NetEase world decryption & passive encryption, standalone dimension pruning workbench, and full-featured NBT / LevelDB visual editor with smart semantic tagging, big data paging, and 2D chunk grid slimming have been fully implemented.

---

## 🌟 NG (Next-Generation) Highlights

* 📦 **Full-Featured NBT & LevelDB Visual Editor**:
  * **Comprehensive Format Support**: Supports single NBT files (`.dat`, `.nbt`, `level.dat`), MCA region files (`.mca`, `.mcr`), LevelDB databases (`db/`), and world archives (`.zip`, `.mcworld`) with rapid parsing and tree hierarchy exploration.
  * **Smart Semantic Analysis & Tags**: Built-in Minecraft semantic descriptor engine automatically identifies player attributes (health, hunger, XP, coordinates, inventory), entities/block entities (chest contents, furnace cook progress, sign texts), and world gamerules, presenting intuitive bilingual tags and safe value modification guidance to lower the learning curve.
  * **Big Data Paging & Anti-Lag Optimization**: Dynamic virtual paging and decimal/hex views for large `ByteArray`, `IntArray`, and `LongArray` structures (biome palettes, heightmaps), preventing UI freezing and OOM crashes.
  * **Rock-Solid Pure Java LevelDB Engine**: Built-in native ZLib compression bridge, eliminating Android JNI memory mapping (mmap) crashes to guarantee safe, crash-free loading and atomic transactions for Bedrock worlds.
* 🗺️ **2D Chunk Visualizer & Precise Slimming**:
  * **Interactive Chunk Grid Viewport**: Supports pan, pinch-to-zoom, tap, and rectangular multi-select box selection across world dimensions.
  * **InhabitedTime Analysis & 0-Tick Pruning**: Automatically calculates chunk player dwell time, pinpoints 0-tick untouched chunks, and enables safe one-tap batch pruning to dramatically reduce world storage.
* 🔄 **Full-Featured World Converter**:
  * **Bidirectional Lossless Conversion**: Seamlessly converts worlds between Java Edition (JE) and Bedrock Edition (BE), with custom target version selection.
  * **Smart Version Upgrade & Downgrade Mapping**: Cross-platform equivalent baseline mapping (JE 1.20 ↔ BE 1.20) with automatic recommendations and risk assessments for 1.18 negative height truncation and 1.13 block flattening.
  * **Foreground Service Persistence**: Long-running conversions are guarded by an Android foreground service with notification updates, preventing background kills and providing conversion history management with remedy export.
* ✂️ **Standalone Dimension Pruning & Slimming**:
  * Dedicated slimming workbench with Overworld-only, Speed, and Full-dimension presets, independent dimension toggles, and `-k` raw NBT retention rules.
* 🔐 **NetEase Decryption & Passive Encryption**:
  * Streaming LevelDB XOR algorithm with magic byte integrity verification, passive encryption, and direct pipeline into the world converter.
* 🌐 **Standardized i18n Architecture (ZH / EN / JA)**:
  * Complete English (default fallback), Simplified Chinese, and Japanese localized resources with instant in-app seamless language switching.
* 🎨 **Jetpack Compose Modern UX (MD3E)**:
  * Built entirely on Material Design 3 Expressive standards, featuring Squircle container interactions, system window inset adaptation, and collapsible cards to maximize screen real estate.
* ⚡ **Ultra-Slim RootFS & Sandbox Console**:
  * Heavily pruned payload down to 57 binaries with 50% storage savings; built-in terminal console for debugging OpenJDK 17 and Shell commands.

---

## 🚀 Core Features Roadmap (Canary Status)

* ✅ **Interactive Sandbox Console**: `Implemented` (Supports OpenJDK 17 and Shell commands)
* ✅ **Bidirectional World Conversion**: `Implemented` (Supports Java/Bedrock conversion, version targets, foreground service, history logs, and remedy export)
* ✅ **Dimension Pruning & World Slimming**: `Implemented` (Standalone workbench, Overworld-only/Speed presets, per-dimension toggles, and `-k` raw NBT retention)
* ✅ **NetEase Decryption & Passive Encryption**: `Implemented` (LevelDB XOR stream decryptor, magic byte check, passive encryption, and pipeline integration)
* ✅ **Version Mapping & Risk Assessment**: `Implemented (Canary 0.3)` (Semantic version models, baseline recommendation, 1.18/1.13 downgrade alerts)
* ✅ **Standardized i18n (ZH / EN / JA)**: `Implemented (Canary 0.3)` (Full English, Chinese, Japanese localizations with in-app switcher)
* ✅ **NBT / LevelDB Visual Explorer**: `Implemented (Canary 0.4)` (Tree manipulation, LevelDB category query, big data paging, full format support)
* ✅ **Minecraft Smart Semantic Analysis & Tags**: `Implemented (Canary 0.4)` (Player/entity/tile entity semantic tags, beginner-friendly value guides)
* ✅ **2D Chunk Visualizer & Precise Slimming**: `Implemented (Canary 0.4)` (Pinch/pan grid, InhabitedTime analysis, 0-tick chunk pruning)
* ⏳ **Resource Pack Converter**: `In Development` (Pending migration)

---

## 🛠️ Build & Development

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1) or newer
* **JDK**: OpenJDK 17 or JDK 21
* **Android SDK**: Min SDK 27 (Android 8.1), Target SDK 35 (Android 15)
* **Gradle**: 8.9+

### Building Release APK
```bash
./gradlew assembleRelease
```
The output APK will be generated at `app/build/outputs/apk/release/app-release.apk`.

---

## 📜 License

This project is licensed under the [GNU General Public License v3.0 (GPLv3)](LICENSE).

---

## 🤝 Credits & Acknowledgments

* **Original Author & Founder**: Dozener (DozenesStudio) (<dozener@outlook.com>)
* **Current Maintainer**: DICECAN (EncoreTeam)
* **Core Contributors**: Ryan Steven, Dozener
* **Open Source Dependencies & Algorithmic References**:
  * [The Hive - Chunker](https://github.com/HiveGamesOSS/Chunker) (MIT License)
  * [HTMonkeyG - XOR-MC-Archive-Decrypt](https://github.com/HTMonkeyG/XOR-MC-Archive-Decrypt) (GPL-3.0)
  * [PowerNukkit - NBT-Manipulator](https://github.com/PowerNukkit/NBT-Manipulator) (MIT License)
  * [iq80 - leveldb](https://github.com/dain/leveldb) (Apache-2.0)
  * [HiveGamesOSS - leveldb-mcpe-java](https://github.com/HiveGamesOSS/leveldb-mcpe-java) (Apache-2.0 / BSD)
  * [Dicecan - NetEaseDecryptorSDK](https://github.com/Dicecan/NetEaseDecryptorSDK) (GPL-3.0)
