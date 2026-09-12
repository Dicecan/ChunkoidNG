# Chunkoid NG (Next-Generation) English Documentation

[简体中文 (Chinese)](README_zh.md) | [Home](README.md) | [Official Website](https://chunkoid.top)

---

## 📢 Project Origin & EncoreTeam Recode Statement

**Chunkoid NG (Next-Generation)** is a comprehensive, modern recode spearheaded by **DICECAN (EncoreTeam)**, succeeding the original **Chunkoid** application created by independent developer **Dozener (DozenesStudio)**.

* **Project Transition**: The original author, Dozener, explored and created the very first PC-free, Android-native Minecraft world conversion utility in his spare time. As Dozener embarked on his university journey, he graciously and unconditionally transferred the full project assets and IP to DICECAN (EncoreTeam) to ensure continuous maintenance and future development.
* **License Unchanged**: The project remains strictly open-source and free under the original **[GNU General Public License v3.0 (GPLv3)](LICENSE)**.
* **Founder Attribution**: We deeply respect Dozener's pioneering contributions; his attribution and acknowledgments will be **permanently honored** across all repositories, documentations, and application releases.

---

> [!WARNING]
> ### 🚧 Early Canary Preview Phase
> This project is currently undergoing full active migration and refactoring from the legacy Chunkoid code. **Most core business features remain in development or pending migration.** Currently, only the foundational OpenJDK 17 sandbox environment and interactive terminal console are implemented.

---

## 🌟 NG (Next-Generation) Highlights

* **Jetpack Compose (MD3E)**: Built entirely on Material Design 3 Expressive standards.
* **Ultra-Slim RootFS**: Heavily pruned payload down to 57 binaries with 50% storage savings.
* **Interactive Sandbox Console**: Built-in environment self-check and terminal for debugging OpenJDK 17 and Chunker CLI.

---

## 🚀 Core Features Roadmap (Canary Status)

* ✅ **Interactive Sandbox Console**: `Implemented` (Supports OpenJDK 17 and Shell commands)
* ⏳ **Bidirectional World Conversion**: `In Development` (Pending core engine migration)
* ⏳ **Version Upgrading & Dimension Pruning**: `In Development`
* ⏳ **NetEase World Decryption**: `In Development` (Pending LevelDB XOR algorithm migration)
* ⏳ **NBT / LevelDB Visual Explorer**: `In Development`
* ⏳ **Resource Pack Converter**: `In Development`

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
