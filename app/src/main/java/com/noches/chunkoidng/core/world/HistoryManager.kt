package com.noches.chunkoidng.core.world

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ConversionHistoryRecord(
    val id: String,
    val timestamp: Long,
    val worldName: String,
    val sourcePlatform: String,
    val targetPlatform: String,
    val durationMs: Long,
    val iconPath: String?
)

class HistoryManager(private val context: Context) {
    private val historyFile = File(context.filesDir, "history.json")
    private val iconDir = File(context.filesDir, "history_icons").apply { mkdirs() }

    fun addRecord(
        worldName: String,
        sourcePlatform: String,
        targetPlatform: String,
        durationMs: Long,
        icon: Bitmap?
    ) {
        val id = UUID.randomUUID().toString()
        var iconPath: String? = null
        if (icon != null) {
            val iconFile = File(iconDir, "$id.png")
            FileOutputStream(iconFile).use { out ->
                icon.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            iconPath = iconFile.absolutePath
        }

        val record = ConversionHistoryRecord(
            id = id,
            timestamp = System.currentTimeMillis(),
            worldName = worldName,
            sourcePlatform = sourcePlatform,
            targetPlatform = targetPlatform,
            durationMs = durationMs,
            iconPath = iconPath
        )

        val records = getRecords().toMutableList()
        records.add(0, record) // Add to top
        saveRecords(records)
    }

    fun getRecords(): List<ConversionHistoryRecord> {
        if (!historyFile.exists()) return emptyList()
        val list = mutableListOf<ConversionHistoryRecord>()
        try {
            val jsonText = historyFile.readText()
            val array = JSONArray(jsonText)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ConversionHistoryRecord(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("timestamp"),
                        worldName = obj.getString("worldName"),
                        sourcePlatform = obj.getString("sourcePlatform"),
                        targetPlatform = obj.getString("targetPlatform"),
                        durationMs = obj.getLong("durationMs"),
                        iconPath = if (obj.has("iconPath")) obj.getString("iconPath") else null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveRecords(records: List<ConversionHistoryRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            val obj = JSONObject().apply {
                put("id", record.id)
                put("timestamp", record.timestamp)
                put("worldName", record.worldName)
                put("sourcePlatform", record.sourcePlatform)
                put("targetPlatform", record.targetPlatform)
                put("durationMs", record.durationMs)
                if (record.iconPath != null) {
                    put("iconPath", record.iconPath)
                }
            }
            array.put(obj)
        }
        historyFile.writeText(array.toString())
    }

    fun clearHistory() {
        if (historyFile.exists()) historyFile.delete()
        if (iconDir.exists()) {
            iconDir.listFiles()?.forEach { it.delete() }
        }
    }
}

