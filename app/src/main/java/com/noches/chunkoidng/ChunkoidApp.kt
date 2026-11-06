package com.noches.chunkoidng

import android.app.Application
import com.noches.chunkoidng.core.crash.CrashHandler
import com.noches.chunkoidng.maid.Maid

class ChunkoidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Maid.init(this)
        CrashHandler.init(this)
    }
}
