package com.noches.chunkoidng

import android.app.Application
import com.noches.chunkoidng.core.crash.CrashHandler

class ChunkoidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.init(this)
    }
}
