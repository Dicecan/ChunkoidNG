package com.noches.chunkoidng

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class I18nConsistencyTest {

    private fun extractKeys(file: File): Set<String> {
        assertTrue("Resource file must exist: ${file.absolutePath}", file.exists())
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        val stringNodes = doc.getElementsByTagName("string")
        val keys = mutableSetOf<String>()
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val name = node.attributes.getNamedItem("name")?.nodeValue
            if (name != null) keys.add(name)
        }
        return keys
    }

    @Test
    fun testAllLocalesHaveIdenticalKeys() {
        val baseDir = File("src/main/res")
        val enFile = File(baseDir, "values/strings.xml")
        val zhFile = File(baseDir, "values-zh-rCN/strings.xml")
        val jaFile = File(baseDir, "values-ja/strings.xml")

        val enKeys = extractKeys(enFile)
        val zhKeys = extractKeys(zhFile)
        val jaKeys = extractKeys(jaFile)

        assertTrue("English keys should not be empty", enKeys.isNotEmpty())
        assertEquals("Simplified Chinese keys must match English keys", enKeys, zhKeys)
        assertEquals("Japanese keys must match English keys", enKeys, jaKeys)
    }
}
