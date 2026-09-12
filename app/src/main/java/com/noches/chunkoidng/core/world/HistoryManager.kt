package com.noches.chunkoidng.core.world

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class ConversionHistoryRecord(
    val id: String,
    val timestamp: Long,
    val worldName: String,
    val sourcePlatform: String,
    val targetPlatform: String,
    val durationMs: Long,
    val iconPath: String?,
    val exportedUri: String? = null
)

class HistoryManager(private val context: Context) {
    private val maxRecords = 100
    private val historyFile = File(context.filesDir, "history.json")
    private val iconDir = File(context.filesDir, "history_icons").apply { mkdirs() }

    @Synchronized
    fun addRecord(
        worldName: String,
        sourcePlatform: String,
        targetPlatform: String,
        durationMs: Long,
        icon: Bitmap?
    ): String {
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
        saveRecords(records.take(maxRecords))
        return id
    }

    @Synchronized
    fun updateExportLocation(id: String, uri: String) {
        val records = getRecords().toMutableList()
        val index = records.indexOfFirst { it.id == id }
        if (index != -1) {
            records[index] = records[index].copy(exportedUri = uri)
            saveRecords(records.take(maxRecords))
        }
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
                        iconPath = if (obj.has("iconPath")) obj.getString("iconPath") else null,
                        exportedUri = if (obj.has("exportedUri")) obj.getString("exportedUri") else null
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
                if (record.exportedUri != null) {
                    put("exportedUri", record.exportedUri)
                }
            }
            array.put(obj)
        }
        val tempFile = File(context.filesDir, "history.json.tmp")
        tempFile.bufferedWriter().use { writer ->
            writer.write(array.toString())
        }
        try {
            Files.move(
                tempFile.toPath(),
                historyFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            // Some providers/filesystems do not support atomic moves.
            if (!tempFile.renameTo(historyFile)) {
                tempFile.delete()
                throw IllegalStateException("无法保存转换历史")
            }
        }
    }

    @Synchronized
    fun clearHistory() {
        if (historyFile.exists()) historyFile.delete()
        if (iconDir.exists()) {
            iconDir.listFiles()?.forEach { it.delete() }
        }
    }
}

