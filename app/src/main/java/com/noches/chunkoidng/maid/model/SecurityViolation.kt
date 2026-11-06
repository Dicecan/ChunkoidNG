package com.noches.chunkoidng.maid.model

enum class ViolationSeverity {
    BLOCKED,
    GENTLE_TRUNCATED,
    WARNING
}

enum class ViolationType {
    ZIP_SLIP,
    ZIP_BOMB_TOTAL_SIZE,
    ZIP_BOMB_ENTRY_SIZE,
    ZIP_ENTRY_COUNT_LIMIT,
    FRAME_COUNT_LIMIT,
    NOTE_COUNT_LIMIT,
    MIDI_FILE_SIZE_LIMIT,
    MALFORMED_HEADER,
    BUFFER_OVERFLOW_ATTEMPT
}

data class SecurityViolation(
    val timestamp: Long = System.currentTimeMillis(),
    val severity: ViolationSeverity,
    val type: ViolationType,
    val module: String,
    val message: String,
    val pathOrTarget: String? = null,
    val requestedValue: Long? = null,
    val clampedValue: Long? = null
)
