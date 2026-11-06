package com.noches.chunkoidng.maid.model

data class PerformanceLimits(
    val jvmMaxMemoryMb: Int,
    val parallelism: Int,
    val useSerialGc: Boolean,
    val minHeapFreeRatio: Int? = null,
    val maxHeapFreeRatio: Int? = null,
    val archiveMaxEntryBytes: Long,
    val archiveMaxTotalBytes: Long,
    val archiveMaxEntries: Int,
    val nbtArrayPageSize: Int,
    val packMaxFrames: Int,
    val midiMaxFileBytes: Long,
    val midiMaxNotes: Int
)
