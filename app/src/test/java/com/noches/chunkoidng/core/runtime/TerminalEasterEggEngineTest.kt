package com.noches.chunkoidng.core.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalEasterEggEngineTest {

    @Test
    fun easterEggCommandsRespondAndPassThroughUnknownCommands() {
        val creeper = TerminalEasterEggEngine.evaluate("creeper")
        assertNotNull(creeper)
        assertTrue(creeper!!.lines.any { it.contains("Creeper") })

        val herobrine = TerminalEasterEggEngine.evaluate("  HEROBRINE ")
        assertNotNull(herobrine)
        assertTrue(herobrine!!.lines.any { it.contains("Herobrine") })

        val teapot = TerminalEasterEggEngine.evaluate("coffee")
        assertNotNull(teapot)
        assertTrue(teapot!!.lines.any { it.contains("418") })

        val matrix = TerminalEasterEggEngine.evaluate("matrix")
        assertNotNull(matrix)
        assertTrue(matrix!!.toggleMatrixMode == true)

        val konami = TerminalEasterEggEngine.evaluate("uuddlrlrba")
        assertNotNull(konami)
        assertTrue(konami!!.unlockKonami)

        val clear = TerminalEasterEggEngine.evaluate("clear")
        assertNotNull(clear)
        assertTrue(clear!!.clearLogs)

        assertNull(TerminalEasterEggEngine.evaluate("java -version"))
        assertNull(TerminalEasterEggEngine.evaluate("ls -la"))
    }

    @Test
    fun helpListsCommandsAndEasterHints() {
        val help = TerminalEasterEggEngine.evaluate("help")
        assertNotNull(help)
        assertFalse(help!!.clearLogs)
        assertTrue(help.lines.any { it.contains("creeper") })
    }
}
