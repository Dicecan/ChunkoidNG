package com.noches.chunkoidng.core.world

import androidx.annotation.StringRes
import com.noches.chunkoidng.R

enum class Platform(
    val displayName: String,
    val shortName: String,
    @StringRes val nameRes: Int
) {
    JAVA("Java", "JE", R.string.platform_java),
    BEDROCK("Bedrock", "BE", R.string.platform_bedrock);

    val isJava: Boolean get() = this == JAVA
    val isBedrock: Boolean get() = this == BEDROCK

    companion object {
        fun fromString(value: String): Platform {
            return when {
                value.contains("JAVA", ignoreCase = true) || value.contains("JE", ignoreCase = true) -> JAVA
                else -> BEDROCK
            }
        }
    }
}
