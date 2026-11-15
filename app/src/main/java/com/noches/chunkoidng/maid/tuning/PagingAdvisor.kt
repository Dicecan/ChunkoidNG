package com.noches.chunkoidng.maid.tuning

import com.noches.chunkoidng.maid.audit.SecurityAuditor
import com.noches.chunkoidng.maid.model.PerformanceLimits
import com.noches.chunkoidng.maid.model.ViolationType

class PagingAdvisor(private val securityAuditor: SecurityAuditor) {

    fun getRecommendedNbtPageSize(limits: PerformanceLimits): Int {
        return limits.nbtArrayPageSize.coerceIn(20, 1000)
    }

    fun sanitizeAnimationFrames(
        requestedFrames: Long,
        maxFrames: Int,
        texturePath: String? = null
    ): Int {
        if (requestedFrames <= maxFrames) {
            return requestedFrames.toInt()
        }

        securityAuditor.recordGentleTruncate(
            type = ViolationType.FRAME_COUNT_LIMIT,
            module = "PackConverter",
            message = "Animation frame count exceeded limit; MAID™ gently clamped to safe cap",
            requestedValue = requestedFrames,
            clampedValue = maxFrames.toLong(),
            pathOrTarget = texturePath
        )
        return maxFrames
    }

    fun sanitizeMidiNotes(
        requestedNotes: Int,
        maxNotes: Int,
        songTitle: String? = null
    ): Int {
        if (requestedNotes <= maxNotes) {
            return requestedNotes
        }

        securityAuditor.recordGentleTruncate(
            type = ViolationType.NOTE_COUNT_LIMIT,
            module = "MidiConverter",
            message = "MIDI note count exceeded limit; MAID™ gently clamped to prevent OOM",
            requestedValue = requestedNotes.toLong(),
            clampedValue = maxNotes.toLong(),
            pathOrTarget = songTitle
        )
        return maxNotes
    }
}
