package com.noches.chunkoidng.core.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class JavaProcessManagerTest {

    @Test
    fun preAndroid10TriesDirectExecutionBeforeLinker() {
        val candidates = JavaProcessManager.buildLaunchCandidates(
            javaBinPath = "/data/user/0/app/files/rootfs/usr/lib/jvm/bin/java",
            jvmArgs = listOf("-Xmx1536m", "-jar", "cli.jar"),
            sdkInt = 27,
            has64BitAbi = true
        )

        assertEquals(2, candidates.size)
        assertEquals("/data/user/0/app/files/rootfs/usr/lib/jvm/bin/java", candidates[0].first())
        assertEquals("/system/bin/linker64", candidates[1].first())
        assertEquals("/data/user/0/app/files/rootfs/usr/lib/jvm/bin/java", candidates[1][1])
    }

    @Test
    fun android10AndAboveTriesLinkerBeforeDirectExecution() {
        val candidates = JavaProcessManager.buildLaunchCandidates(
            javaBinPath = "/data/user/0/app/files/rootfs/usr/lib/jvm/bin/java",
            jvmArgs = listOf("-Xmx2048m", "-jar", "cli.jar"),
            sdkInt = 34,
            has64BitAbi = true
        )

        assertEquals(2, candidates.size)
        assertEquals("/system/bin/linker64", candidates[0].first())
        assertEquals("/data/user/0/app/files/rootfs/usr/lib/jvm/bin/java", candidates[0][1])
        assertEquals("/data/user/0/app/files/rootfs/usr/lib/jvm/bin/java", candidates[1].first())
    }

    @Test
    fun uses32BitLinkerWhenNo64BitAbiPresent() {
        val candidates = JavaProcessManager.buildLaunchCandidates(
            javaBinPath = "/data/app/java",
            jvmArgs = emptyList(),
            sdkInt = 29,
            has64BitAbi = false
        )

        assertEquals("/system/bin/linker", candidates[0].first())
    }
}
