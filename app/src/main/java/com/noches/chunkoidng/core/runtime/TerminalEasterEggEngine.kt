package com.noches.chunkoidng.core.runtime

data class TerminalCommandResponse(
    val lines: List<String> = emptyList(),
    val clearLogs: Boolean = false,
    val toggleMatrixMode: Boolean? = null,
    val unlockKonami: Boolean = false
)

object TerminalEasterEggEngine {

    private val splashes = listOf(
        "100% pure Java, 0% MappedByteBuffer crashes!",
        "Now with fewer chunk borders!",
        "Tamed by MAID™ Defense Architecture!",
        "Converted worlds preserved with care!",
        "Note blocks tuned to 0.1s redstone ticks!",
        "Also check chunk (0, 0) for mysterious signs!",
        "Don't dig straight down in LevelDB!"
    )

    fun evaluate(rawCommand: String, randomIndex: Int = (System.nanoTime().toInt() and 0x7FFFFFFF)): TerminalCommandResponse? {
        val normalized = rawCommand.trim().lowercase().replace(Regex("\\s+"), " ")
        if (normalized.isEmpty()) return null

        return when (normalized) {
            "clear", "cls" -> TerminalCommandResponse(
                clearLogs = true,
                lines = listOf("[SYSTEM] Terminal buffer cleared.")
            )

            "help", "?" -> TerminalCommandResponse(
                lines = listOf(
                    "[SYSTEM] ChunkoidNG Sandbox Terminal — Built-in Commands:",
                    "  • java -jar cli.jar   Run Chunker CLI help",
                    "  • java -version       Verify bundled OpenJDK 17 runtime",
                    "  • ls / pwd / uname -a Inspect RootFS sandbox environment",
                    "  • clear / cls         Clear terminal buffer",
                    "  • help / easter       Show commands & secret terminal hints",
                    "[EASTER] Hidden triggers: creeper, herobrine, maid, splash, coffee, matrix, konami, /gamemode 1, 42, nyan"
                )
            )

            "easter", "eggs", "secret", "secrets" -> TerminalCommandResponse(
                lines = listOf(
                    "[EASTER] ChunkoidNG Easter Egg Scanner:",
                    "  Try typing: creeper | herobrine | chunkoid | maid | splash | fortune | coffee | teapot | matrix | neo | konami | /gamemode creative | steve | nyan | 42 | sudo make me a sandwich"
                )
            )

            "creeper", "aww man" -> TerminalCommandResponse(
                lines = listOf(
                    "[EASTER] A wild Creeper appeared in chunk (0, 0)!",
                    "    ██████████",
                    "    ██  ██  ██",
                    "    ████  ████",
                    "    ██      ██",
                    "    ██  ██  ██",
                    "  Tsssss... *BOOM!* Relax — MAID™ kept your world backup safe."
                )
            )

            "herobrine", "removed herobrine" -> TerminalCommandResponse(
                lines = listOf(
                    "[EASTER] Changelog Patch Notes:",
                    "  - Removed Herobrine",
                    "  ...wait. Why are the leaves disappearing in Overworld/region/r.0.0.mca?",
                    "  [HEROBRINE] Eyes glowing in the fog. Just kidding... probably."
                )
            )

            "chunkoid", "chunkoidng", "neofetch" -> TerminalCommandResponse(
                lines = listOf(
                    "[EASTER] ╔══════════════════════════════════╗",
                    "         ║   ChunkoidNG Portable Workshop   ║",
                    "         ╚══════════════════════════════════╝",
                    "  Runtime : OpenJDK 17 (aarch64 headless)",
                    "  Engine  : Chunker CLI + Native Entity/GameRule Pipelines",
                    "  Shield  : MAID™ Integrity & Defense Guard",
                    "  Motto   : Convert worlds cleanly, anywhere."
                )
            )

            "maid", "maid --status", "/maid" -> TerminalCommandResponse(
                lines = listOf(
                    "[EASTER] [MAID™] Standing by with spotless duster!",
                    "  • Zip-Slip Guard : ACTIVE",
                    "  • LevelDB SafeEnv: MMAP DISABLED (ART-Safe)",
                    "  • Memory Monitor : Watching heap watermarks gracefully."
                )
            )

            "coffee", "brew coffee", "teapot" -> TerminalCommandResponse(
                lines = listOf(
                    "[EASTER] HTTP 418 I'm a teapot",
                    "       ( (",
                    "        ) )",
                    "     ........",
                    "     |      |]",
                    "     \\      /",
                    "      `----'",
                    "  Refusing to brew coffee inside an Android Dalvik/ART kettle."
                )
            )

            "fortune", "splash", "mc splash" -> {
                val pick = splashes[randomIndex % splashes.size]
                TerminalCommandResponse(
                    lines = listOf("[EASTER] ★ Splash: \"$pick\"")
                )
            }

            "matrix", "neo", "follow the white rabbit" -> TerminalCommandResponse(
                toggleMatrixMode = true,
                lines = listOf(
                    "[EASTER] Wake up, Steve...",
                    "  The Overworld has you. Follow the Enderman.",
                    "  [MATRIX] Phosphor green theme toggled. Type 'matrix' again to toggle."
                )
            )

            "konami", "uuddlrlrba", "up up down down left right left right b a" -> TerminalCommandResponse(
                unlockKonami = true,
                lines = listOf(
                    "[EASTER] ↑ ↑ ↓ ↓ ← → ← → B A",
                    "  ★ +30 Lives unlocked! Creative Redstone Overclock badge activated."
                )
            )

            "/gamemode creative", "/gamemode 1", "gamemode 1", "gamemode creative" -> TerminalCommandResponse(
                lines = listOf("[EASTER] Set own game mode to Creative Mode (Terminal privileges unchanged — stay out of trouble!).")
            )

            "sudo make me a sandwich" -> TerminalCommandResponse(
                lines = listOf("[EASTER] Okay fine. 🥪 +1 Golden Sandwich crafted!")
            )

            "sudo rm -rf /", "rm -rf /" -> TerminalCommandResponse(
                lines = listOf("[EASTER] [MAID™ BLOCKED] Nice try, Chaos Agent! System & world directories remain intact.")
            )

            "steve", "villager", "hrmm" -> TerminalCommandResponse(
                lines = listOf("[EASTER] Villager: \"Hrmmm... Best I can do is 1x Emerald for 64x Converted Chunks.\"")
            )

            "nyan", "cat" -> TerminalCommandResponse(
                lines = listOf(
                    "[EASTER]  /\\_/\\",
                    "         ( o.o )  Meow! Reminder: never dig straight down.",
                    "          > ^ <"
                )
            )

            "42", "answer" -> TerminalCommandResponse(
                lines = listOf("[EASTER] 42 — Answer to the Ultimate Question of Chunks, Entities, and Everything.")
            )

            else -> null
        }
    }
}
