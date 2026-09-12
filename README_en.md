# Chunkoid NG (Next-Generation) English Documentation

[简体中文 (Chinese)](README_zh.md) | [Home](README.md) | [Official Website](https://chunkoid.top)

---

## 📢 Project Origin & EncoreTeam Recode Statement

**Chunkoid NG (Next-Generation)** is a comprehensive, modern recode spearheaded by **DICECAN (EncoreTeam)**, succeeding the original **Chunkoid** application created by independent developer **Dozener (DozenesStudio)**.

* **Project Transition**: The original author, Dozener, explored and created the very first PC-free, Android-native Minecraft world conversion utility in his spare time. As Dozener embarked on his university journey, he graciously and unconditionally transferred the full project assets and IP to DICECAN (EncoreTeam) to ensure continuous maintenance and future development.
* **License Unchanged**: The project remains strictly open-source and free under the original **[GNU General Public License v3.0 (GPLv3)](LICENSE)**.
* **Founder Attribution**: We deeply respect Dozener's pioneering contributions; his attribution and acknowledgments will be **permanently honored** across all repositories, documentations, and application releases.

---

## 💡 What's New in NG (Next-Generation)?

While the original Chunkoid established the viability of mobile world conversion, Chunkoid NG delivers a generational leap forward:

1. **Jetpack Compose & MD3 Expressive (MD3E)**:
   * Entirely re-engineered with Jetpack Compose, discarding legacy XML layouts.
   * Dynamic Material You color schemes, non-linear fluid page animations, and adaptive staggered grid cards.
2. **Ultra-Slim RootFS 2.0**:
   * Pruned payload down from 146 fragmented files to just **57 essential binaries**.
   * Extraction speeds boosted by over 300% with nearly 50% storage savings, providing instant OpenJDK 17 aarch64 deployment.
3. **Interactive Developer Sandbox Console**:
   * Built-in full-screen terminal emulator enabling direct execution of Shell, OpenJDK 17, and Chunker CLI commands.
   * Native handling for Android 10+ `W^X` memory protection using dynamic linkers.
   * Free-form text selection, one-click clipboard copying, syntax color highlighting, and micro-batched 60fps streaming.
4. **Reactive Coroutine & Flow Architecture**:
   * All background operations, extraction tracking, and stdout streaming migrated to pure Kotlin `Flow<T>`, eliminating legacy thread blocking and ANR issues.

---

## 🚀 Key Features

* 🔄 **Bidirectional World Conversion**: Bedrock (BE/PE) ↔ Java Edition (JE) conversion supporting Minecraft versions 1.8.8 through 1.21+.
* 📦 **Version Upgrading / Downgrading & Dimension Pruning**: Seamless version translation and unneeded dimension cleanup.
* 🔓 **Encrypted NetEase World Decryption**: Built-in LevelDB pointer XOR streaming decryption engine.
* 🛠️ **NBT & LevelDB Visual Explorer**: Hierarchical tree-based inspection and manipulation of world state.
* 🎨 **Texture & Resource Pack Converter**: Cross-edition pack format translation.

---

## 🛠️ Build & Development

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1) or newer
* **JDK**: OpenJDK 17 or JDK 21
* **Android SDK**: Min SDK 26 (Android 8.0), Target SDK 34 (Android 14)
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
* **Core Contributors**: Ryan Steven, Dozener, Weiyin 1A
* **Open Source Dependencies**:
  * [The Hive - Chunker](https://github.com/HiveGamesOSS/Chunker) (MIT License)
  * [PowerNukkit - NBT-Manipulator](https://github.com/PowerNukkit/NBT-Manipulator) (MIT License)
  * [HiveGamesOSS - leveldb-mcpe-java](https://github.com/HiveGamesOSS/leveldb-mcpe-java) (Apache-2.0 / BSD)
  * [Dicecan - NetEaseDecryptorSDK](https://github.com/Dicecan/NetEaseDecryptorSDK) (GPL-3.0)
