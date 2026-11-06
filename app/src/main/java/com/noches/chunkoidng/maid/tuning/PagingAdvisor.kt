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
            message = "动画展开帧数超限，MAID™已温和截断至安全上限",
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
            message = "MIDI音符总数超标，MAID™已温和截断至安全上限以防内存溢出",
            requestedValue = requestedNotes.toLong(),
            clampedValue = maxNotes.toLong(),
            pathOrTarget = songTitle
        )
        return maxNotes
    }
}
