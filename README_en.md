<div align="center">
  <h1>Chunkoid NG</h1>
  <p><strong>Minecraft World and Resource Pack Toolkit for Android</strong></p>

  <p>
    <a href="README_zh.md">简体中文</a> | 
    <a href="README_en.md">English</a> | 
    <a href="https://chunkoid.top">Official Website</a>
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

## Overview

**Chunkoid NG (Next-Generation)** is an Android-native Minecraft world conversion and resource management utility, refactored and maintained by **EncoreTeam**.

- **Project Transition**: Originated from Chunkoid developed by **Dozener (DozenesStudio)**. To ensure sustained maintenance and evolution after Dozener entered university, the project was officially handed over to **DICECAN (EncoreTeam)**.
- **License**: The project continues under the **[GNU General Public License v3.0 (GPLv3)](LICENSE)**.
- **Founder Attribution**: Dozener's pioneer attribution and credits remain permanently honored across all documentations, repositories, and release builds.

The project has transitioned from early previews to **Beta 0.6**. Core feature modules are complete and operational, with ongoing development focused on stability and device-specific performance tuning.

---

## Features

- **World Conversion**: Bidirectional conversion between Java Edition and Bedrock Edition (BE), with version mapping, cross-version compatibility warnings, and foreground service persistence.
- **NBT & LevelDB Editor**: Parse and edit `.dat`, `.mca`, `level.dat`, LevelDB key-value entries, and full `.mcworld`/`.zip` archives; includes virtual paging for large arrays and semantic tags for common data fields.
- **2D Chunk Visualizer & Pruning**: Grid-based chunk inspection across dimensions (Overworld / Nether / The End), with unvisited 0-tick chunk pruning and dimension trimming to reduce world size.
- **Resource Pack Converter**: Bidirectional conversion between Java and Bedrock resource packs, resolving blockstates, models, and MCMETA texture strip stitching.
- **MIDI to Redstone Music**: Parse MIDI tracks to construct compact serpentine redstone noteblock contraptions, directly written into LevelDB flat worlds or injected into existing saves.
- **NetEase World Decryption**: XOR-based decryption and passive encryption for NetEase Bedrock saves.
- **Sandbox Runtime & Performance Tuning**: Embedded OpenJDK 17 runtime with automatic device profiling and selectable memory profiles (Low Memory / Balanced / Aggressive / Custom) to avoid OOM crashes during large world processing.
- **Isolated Crash Handler**: Crash diagnosis running in an isolated `:crash` process to capture hardware snapshots and export stack traces safely.

---

## Building

### Requirements
- **Android Studio**: Ladybug (2024.2.1) or newer
- **JDK**: OpenJDK 17 or JDK 21
- **Android SDK**: API 35 (Minimum: Android 8.0 / API 26)

### Build Commands
```bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew testDebugUnitTest
```

---

## License & Credits

- **License**: [GNU General Public License v3.0](LICENSE)
- **Original Author & Founder**: [Dozener (DozenesStudio)](mailto:dozener@outlook.com)
- **Maintainers**: [EncoreTeam](https://chunkoid.top) (DICECAN)
- **Key Libraries & References**:
  - [The Hive - Chunker](https://github.com/HiveGamesOSS/Chunker) (MIT)
  - [HTMonkeyG - XOR-MC-Archive-Decrypt](https://github.com/HTMonkeyG/XOR-MC-Archive-Decrypt) (GPL-3.0)
  - [PowerNukkit - NBT-Manipulator](https://github.com/PowerNukkit/NBT-Manipulator) (MIT)
  - [iq80 - leveldb](https://github.com/dain/leveldb) (Apache-2.0)
  - [HiveGamesOSS - leveldb-mcpe-java](https://github.com/HiveGamesOSS/leveldb-mcpe-java) (Apache-2.0 / BSD)
