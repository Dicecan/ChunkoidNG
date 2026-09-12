package com.noches.chunkoidng.core.world

/**
 * Supported Minecraft edition platforms.
 */
enum class Platform(val displayName: String, val shortName: String) {
    JAVA("Java 版", "JE"),
    BEDROCK("基岩版", "BE");

    val isJava: Boolean get() = this == JAVA
    val isBedrock: Boolean get() = this == BEDROCK

    companion object {
        fun fromString(value: String): Platform {
            return when (value.uppercase()) {
                "JAVA", "JE", "JAVA_EDITION" -> JAVA
                else -> BEDROCK
            }
        }
    }
}
