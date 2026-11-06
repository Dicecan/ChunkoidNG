package com.noches.chunkoidng.maid.model

import androidx.annotation.StringRes
import com.noches.chunkoidng.R

enum class DeviceTier(@get:StringRes val titleRes: Int) {
    FLAGSHIP(R.string.welcome_tier_flagship),
    HIGH_END(R.string.welcome_tier_high_end),
    MID_RANGE(R.string.welcome_tier_mid_range),
    ENTRY_LEVEL(R.string.welcome_tier_entry)
}
