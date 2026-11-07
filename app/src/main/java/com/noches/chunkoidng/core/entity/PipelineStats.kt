package com.noches.chunkoidng.core.entity

data class PipelineStats(
    val totalActors: Int,
    val skippedModded: Int,
    val chunksWritten: Int
) {
    val chunksIndexed: Int
        get() = chunksWritten
}
