package com.noches.chunkoidng.maid.model

import androidx.annotation.StringRes
import com.noches.chunkoidng.R

enum class PerformanceMode(val id: String, @get:StringRes val titleRes: Int, @get:StringRes val descRes: Int) {
    TUNED("tuned", R.string.welcome_mode_tuned_title, R.string.welcome_mode_tuned_desc),
    STRICT("strict", R.string.welcome_mode_strict_title, R.string.welcome_mode_strict_desc),
    CUSTOM("custom", R.string.welcome_mode_custom_title, R.string.welcome_mode_custom_desc);

    companion object {
        fun fromId(id: String): PerformanceMode {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: TUNED
        }
    }
}
