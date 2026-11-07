<div align="center">
  <h1>Chunkoid NG</h1>
  <p><strong>Minecraft World and Resource Pack Toolkit for Android</strong></p>

  <p>
    <a href="https://github.com/Dicecan/ChunkoidNG/releases"><img src="https://img.shields.io/github/v/release/Dicecan/ChunkoidNG?style=flat-square&color=2f7d63" alt="Release"></a>
    <img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Platform">
    <img src="https://img.shields.io/badge/Arch-arm64--v8a-blue?style=flat-square" alt="Arch">
    <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-orange?style=flat-square" alt="License"></a>
  </p>

  <p>
    <a href="README_zh.md">简体中文</a> | <strong>English</strong>
  </p>
</div>

---

## Quick Navigation

- [Project Background](#project-background)
- [Features](#features)
- [System Requirements](#system-requirements)
- [Build Instructions](#build-instructions)
- [License and Acknowledgements](#license-and-acknowledgements)

---

## Project Background

**Chunkoid NG (Next-Generation)** is a modernized, rebuilt Android toolkit for Minecraft worlds and resource packs, maintained by **EncoreTeam**.

- **Origins and Transition**: The project originated from Chunkoid, independently created by **Dozener (DozenesStudio)**. Upon their transition to university, maintenance was handed over to **DICECAN (EncoreTeam)** to continue active development.
- **Open-Source License**: Strictly adheres to the original **[GNU General Public License v3.0 (GPLv3)](LICENSE)**.
- **Founder Attribution**: Founder attribution to Dozener is permanently retained across code repositories, documentation, and the application's About screen.

The latest public release is **Beta 0.7** (featuring the smart full-entity migration pipeline and MAID™ security & resource governance framework).

---

## Features

- **World Conversion**: Bidirectional conversion between Java Edition and Bedrock Edition (BE), with version mapping, cross-version compatibility warnings, and foreground service persistence.
- **Smart Bi-directional Entity Migration (Beta 0.7 Core Feature)**: Full entity migration pipeline supporting JE ↔ BE and same-platform cross-version conversion; preserves villager trades (`Offers.Recipes`) and enchanted books, resolves cross-chunk leashes and vehicle links, and automatically filters modded entities.
- **MAID™ Security & Resource Governance (Beta 0.7 Core Feature)**: Built-in **MAID™** (**M**anaged **A**rchitecture for **I**ntegrity & **D**efense) framework providing centralized Zip-Slip directory traversal blocking, multi-tier archive quotas, zip bomb circuit breaking, and binary magic number verification.
- **Gentle Truncation Strategy (Beta 0.7 Core Feature)**: Oversized resource pack animation frames and extreme MIDI note counts are safely clamped to device hardware thresholds without interrupting the workflow, preventing out-of-memory crashes while recording audit logs.
- **MAID™ Inspection Dashboard & Live Audit (Beta 0.7 Core Feature)**: A dedicated in-app dashboard accessible via Settings to monitor device compute score, ART heap watermarks, active quotas, and recent security events with one-click report export.
- **NBT & LevelDB Editor**: Parse and edit `.dat`, `.mca`, `level.dat`, LevelDB key-value entries, and full `.mcworld`/`.zip` archives; includes virtual paging for large arrays and semantic tags for common data fields.
- **2D Chunk Visualizer & Pruning**: Grid-based chunk inspection across dimensions (Overworld / Nether / The End), with unvisited 0-tick chunk pruning and dimension trimming to reduce world size.
- **Resource Pack Converter**: Bidirectional conversion between Java and Bedrock resource packs, resolving blockstates, models, and MCMETA texture strip stitching.
- **MIDI to Redstone Music**: Parse MIDI tracks to construct compact serpentine redstone noteblock contraptions, directly written into LevelDB flat worlds or injected into existing saves.
- **NetEase World Decryption**: XOR-based decryption and passive encryption for NetEase Bedrock saves.
- **Sandbox Runtime & Performance Tuning**: Embedded OpenJDK 17 runtime with automatic device profiling and selectable memory profiles (Low Memory / Balanced / Aggressive / Custom) to avoid OOM crashes during large world processing.
- **Isolated Crash Handler**: Crash diagnosis running in an isolated `:crash` process to capture hardware snapshots, audit logs, and export stack traces safely.

---

## System Requirements

- **Operating System**: Android 8.0 (API 27) or higher
- **CPU Architecture**: 64-bit ARM only (`arm64-v8a`)
- **Recommended Memory**: 4GB+ RAM (6GB+ recommended for large Java world conversions)
- **Storage**: Minimum 100MB free space for unpacking the embedded OpenJDK runtime on first launch

---

## Build Instructions

Built with modern Android tooling:
- **Kotlin** 2.0+
- **Jetpack Compose** (Material Design 3)
- **NDK**: LevelDB JNI bindings and native optimizations

Run the following command to assemble a Debug APK:

```bash
./gradlew assembleDebug
```

> **Note**: The first build will automatically download and unpack pre-built aarch64 OpenJDK and Chunker CLI binaries.

---

## License and Acknowledgements

- Licensed under the **GNU General Public License v3.0**, see [LICENSE](LICENSE).
- Core conversion engine powered by [Chunker CLI](https://github.com/HiveGames/Chunker) (The Hive, MIT License).
- Sincere thanks to founder **Dozener** for their exceptional foundation and open-source contributions.
