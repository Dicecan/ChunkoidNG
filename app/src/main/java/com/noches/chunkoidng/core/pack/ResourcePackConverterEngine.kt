package com.noches.chunkoidng.core.pack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ResourcePackConverterEngine {

    private const val MAX_ARCHIVE_ENTRIES = 100_000
    private const val MAX_ENTRY_UNCOMPRESSED_BYTES = 256L * 1024L * 1024L
    private const val MAX_ARCHIVE_UNCOMPRESSED_BYTES = 1L * 1024L * 1024L * 1024L
    private const val MAX_ENTRY_NAME_LENGTH = 512

    suspend fun scanPack(sourceDir: File): PackMetadata = withContext(Dispatchers.IO) {
        val packRoot = resolvePackRoot(sourceDir)
        val files = mutableListOf<File>()
        packRoot.walkTopDown().filter { it.isFile }.forEach { files.add(it) }

        val relativePaths = files.map { it.relativeTo(packRoot).path.replace('\\', '/') }
        val platform = PackManifestHandler.detectPlatform(relativePaths) ?: PackPlatform.JAVA

        var name = packRoot.name
        var description = ""
        var javaFormat: Int? = null
        var bedrockEngineVersion: BedrockEngineVersion? = null
        var iconFile: File? = null

        var textureCount = 0
        var blockTextureCount = 0
        var itemTextureCount = 0
        var langCount = 0
        var animationCount = 0
        var soundCount = 0

        for (rel in relativePaths) {
            val file = File(packRoot, rel)
            val lower = rel.lowercase()

            if (lower == "pack.png" || lower == "pack_icon.png") {
                iconFile = file
            }

            if (lower == "pack.mcmeta") {
                val (fmt, desc) = PackManifestHandler.parseJavaPackMcmeta(file.readText())
                javaFormat = fmt
                if (desc != null) description = desc
            }

            if (lower == "manifest.json") {
                val (n, d, v) = PackManifestHandler.parseBedrockManifest(file.readText())
                if (n != null) name = n
                if (d != null) description = d
                if (v != null) bedrockEngineVersion = v
            }

            if (lower.endsWith(".png") || lower.endsWith(".tga")) {
                textureCount++
                if (lower.contains("/block/") || lower.contains("/blocks/")) {
                    blockTextureCount++
                }
                if (lower.contains("/item/") || lower.contains("/items/")) {
                    itemTextureCount++
                }
            }

            if (lower.endsWith(".png.mcmeta") || lower.endsWith("flipbook_textures.json")) {
                animationCount++
            }

            if (lower.endsWith(".json") && lower.contains("/lang/") || lower.endsWith(".lang") && lower.contains("texts/")) {
                langCount++
            }

            if (lower.endsWith(".ogg") || lower.endsWith(".fsb")) {
                soundCount++
            }
        }

        PackMetadata(
            platform = platform,
            name = name,
            description = description,
            javaFormat = javaFormat,
            bedrockEngineVersion = bedrockEngineVersion,
            iconFile = iconFile,
            textureCount = textureCount,
            blockTextureCount = blockTextureCount,
            itemTextureCount = itemTextureCount,
            langCount = langCount,
            animationCount = animationCount,
            soundCount = soundCount
        )
    }

    suspend fun convertPack(
        sourceDir: File,
        targetDir: File,
        config: PackConversionConfig,
        onProgress: (PackConversionProgress) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        targetDir.mkdirs()

        val packRoot = resolvePackRoot(sourceDir)
        val allFiles = mutableListOf<File>()
        packRoot.walkTopDown().filter { it.isFile }.forEach { allFiles.add(it) }

        onProgress(PackConversionProgress("SCANNING", 0, allFiles.size, "Scanning assets: ${allFiles.size} files"))

        val processedBlockTextures = mutableSetOf<String>()
        val processedItemTextures = mutableSetOf<String>()
        val allTargetTextures = mutableSetOf<String>()
        val flipbookEntries = mutableListOf<FlipbookAnimationHandler.FlipbookEntry>()
        val detectedLocales = mutableListOf<String>()

        var packName = config.customPackName ?: packRoot.name
        var packDescription = config.customDescription ?: "Converted by ChunkoidNG"

        var currentIdx = 0
        val isJavaToBedrock = config.targetPlatform == PackPlatform.BEDROCK

        for (sourceFile in allFiles) {
            currentIdx++
            val relative = sourceFile.relativeTo(packRoot).path.replace('\\', '/')
            val relativeLower = relative.lowercase()
            val targetRel = if (isJavaToBedrock) {
                PackAssetMapper.mapJavaToBedrock(relative)
            } else {
                PackAssetMapper.mapBedrockToJava(relative, config.targetJavaFormat)
            }

            if (relativeLower == "pack.mcmeta" && isJavaToBedrock) {
                val (_, desc) = PackManifestHandler.parseJavaPackMcmeta(sourceFile.readText())
                if (desc != null && config.customDescription == null) packDescription = desc
                continue
            }

            if (relativeLower == "manifest.json" && !isJavaToBedrock) {
                val (name, desc, _) = PackManifestHandler.parseBedrockManifest(sourceFile.readText())
                if (name != null && config.customPackName == null) packName = name
                if (desc != null && config.customDescription == null) packDescription = desc
                continue
            }

            if (relativeLower.endsWith(".png.mcmeta") && isJavaToBedrock) {
                if (config.convertAnimations) {
                    val rawPngPath = relative.substring(0, relative.length - ".mcmeta".length)
                    val mappedPngPath = PackAssetMapper.mapJavaToBedrock(rawPngPath)
                    if (mappedPngPath != null) {
                        val entry = FlipbookAnimationHandler.parseJavaAnimationMcmeta(sourceFile.readText(), mappedPngPath)
                        if (entry != null) {
                            flipbookEntries.add(entry)
                        }
                    }
                }
                continue
            }

            if (relativeLower == "textures/flipbook_textures.json" && !isJavaToBedrock) {
                if (config.convertAnimations) {
                    val extractedMcmetas = FlipbookAnimationHandler.parseBedrockFlipbookJson(sourceFile.readText())
                    for ((mcmetaRel, content) in extractedMcmetas) {
                        val mappedJavaPath = PackAssetMapper.mapBedrockToJava(mcmetaRel, config.targetJavaFormat)
                        if (mappedJavaPath != null) {
                            val javaTarget = File(targetDir, mappedJavaPath)
                            javaTarget.parentFile?.mkdirs()
                            javaTarget.writeText(content)
                        }
                    }
                }
                continue
            }

            if (targetRel != null) {
                val destFile = File(targetDir, targetRel)
                destFile.parentFile?.mkdirs()

                if (isJavaToBedrock) {
                    when {
                        targetRel.lowercase().startsWith("texts/") && targetRel.lowercase().endsWith(".lang") -> {
                            if (config.convertLanguages) {
                                validateJson(sourceFile, "Java language file")
                                val langContent = PackLangHandler.convertJavaJsonToBedrockLang(sourceFile.readText())
                                destFile.writeText(langContent)
                                val localeName = targetRel.substringAfterLast('/').removeSuffix(".lang")
                                detectedLocales.add(localeName)
                            }
                        }
                        targetRel.lowercase() == "sounds/sound_definitions.json" -> {
                            if (config.convertSounds) {
                                validateJson(sourceFile, "Java sounds.json")
                                val bedrockSounds = PackSoundHandler.convertJavaSoundsToBedrock(sourceFile.readText())
                                if (bedrockSounds.isBlank()) {
                                    throw IllegalArgumentException("Failed to convert Java sounds.json")
                                }
                                destFile.writeText(bedrockSounds)
                            }
                        }
                        targetRel == "splashes.json" -> {
                            if (config.convertSplashes) {
                                val lines = sourceFile.readLines().filter { it.isNotBlank() }
                                val rootObj = JSONObject()
                                val array = JSONArray()
                                lines.forEach { array.put(it) }
                                rootObj.put("splashes", array)
                                destFile.writeText(rootObj.toString(2))
                            }
                        }
                        else -> {
                            sourceFile.copyTo(destFile, overwrite = true)
                        }
                    }

                    val targetLower = targetRel.lowercase()
                    if (targetLower.startsWith("textures/blocks/")) {
                        processedBlockTextures.add(targetRel)
                    }
                    if (targetLower.startsWith("textures/items/")) {
                        processedItemTextures.add(targetRel)
                    }
                    if (targetLower.endsWith(".png") || targetLower.endsWith(".tga")) {
                        allTargetTextures.add(targetRel)
                    }
                } else {
                    when {
                        targetRel.lowercase().startsWith("assets/minecraft/lang/") && targetRel.lowercase().endsWith(".json") -> {
                            if (config.convertLanguages) {
                                val jsonContent = PackLangHandler.convertBedrockLangToJavaJson(sourceFile.readText())
                                destFile.writeText(jsonContent)
                            }
                        }
                        targetRel.lowercase() == "assets/minecraft/sounds.json" -> {
                            if (config.convertSounds) {
                                validateJson(sourceFile, "Bedrock sound definitions")
                                val javaSounds = PackSoundHandler.convertBedrockSoundsToJava(sourceFile.readText())
                                if (javaSounds.isBlank()) {
                                    throw IllegalArgumentException("Failed to convert Bedrock sound definitions")
                                }
                                destFile.writeText(javaSounds)
                            }
                        }
                        targetRel == "assets/minecraft/texts/splashes.txt" -> {
                            if (config.convertSplashes) {
                                try {
                                    val obj = JSONObject(sourceFile.readText())
                                    val arr = obj.optJSONArray("splashes")
                                    val lines = mutableListOf<String>()
                                    if (arr != null) {
                                        for (i in 0 until arr.length()) lines.add(arr.getString(i))
                                    }
                                    destFile.writeText(lines.joinToString("\n"))
                                } catch (_: Exception) {
                                    sourceFile.copyTo(destFile, overwrite = true)
                                }
                            }
                        }
                        else -> {
                            sourceFile.copyTo(destFile, overwrite = true)
                        }
                    }
                }
            } else {
                val fallbackDest = File(targetDir, relative)
                fallbackDest.parentFile?.mkdirs()
                sourceFile.copyTo(fallbackDest, overwrite = true)
            }

            if (currentIdx % 20 == 0 || currentIdx == allFiles.size) {
                onProgress(PackConversionProgress("CONVERTING", currentIdx, allFiles.size, "Converted $currentIdx / ${allFiles.size}"))
            }
        }

        if (isJavaToBedrock) {
            val manifestContent = PackManifestHandler.generateBedrockManifest(
                name = packName,
                description = packDescription,
                engineVersion = config.targetBedrockVersion
            )
            File(targetDir, "manifest.json").writeText(manifestContent)

            if (config.generateAtlases) {
                if (processedBlockTextures.isNotEmpty()) {
                    TextureAtlasGenerator.generateTerrainTextureJson(
                        processedBlockTextures,
                        File(targetDir, "textures/terrain_texture.json")
                    )
                    TextureAtlasGenerator.generateBlocksJson(
                        processedBlockTextures,
                        File(targetDir, "blocks.json")
                    )
                }

                if (processedItemTextures.isNotEmpty()) {
                    TextureAtlasGenerator.generateItemTextureJson(
                        processedItemTextures,
                        File(targetDir, "textures/item_texture.json")
                    )
                }

                if (allTargetTextures.isNotEmpty()) {
                    TextureAtlasGenerator.generateTexturesListJson(
                        allTargetTextures,
                        File(targetDir, "textures/textures_list.json")
                    )
                }
            }

            if (config.convertAnimations && flipbookEntries.isNotEmpty()) {
                FlipbookAnimationHandler.writeFlipbookTexturesJson(
                    flipbookEntries,
                    File(targetDir, "textures/flipbook_textures.json")
                )
            }

            if (detectedLocales.isNotEmpty()) {
                PackLangHandler.generateLanguagesJson(
                    detectedLocales,
                    File(targetDir, "texts/languages.json")
                )
            }
        } else {
            val mcmetaContent = PackManifestHandler.generateJavaPackMcmeta(
                format = config.targetJavaFormat,
                description = packDescription
            )
            File(targetDir, "pack.mcmeta").writeText(mcmetaContent)
        }

        onProgress(PackConversionProgress("COMPLETE", allFiles.size, allFiles.size, "Conversion completed successfully"))
        targetDir
    }

    private fun resolvePackRoot(sourceDir: File): File {
        var root = sourceDir
        while (true) {
            val children = root.listFiles()
                ?.filterNot { it.name.equals("__MACOSX", ignoreCase = true) }
                ?: break
            if (children.size != 1 || !children[0].isDirectory) break

            val candidate = children[0]
            val paths = candidate.walkTopDown()
                .filter { it.isFile }
                .map { it.relativeTo(candidate).path.replace('\\', '/') }
                .toList()
            // Only flatten a wrapper when its contents look like a pack. This
            // preserves a legitimate pack containing only a textures folder.
            if (PackManifestHandler.detectPlatform(paths) == null) break
            root = candidate
        }
        return root
    }

    private fun validateJson(file: File, description: String) {
        try {
            JSONObject(file.readText())
        } catch (cause: Exception) {
            throw IllegalArgumentException("Invalid $description: ${file.name}", cause)
        }
    }

    suspend fun extractArchive(archiveFile: File, destinationDir: File) = withContext(Dispatchers.IO) {
        if (destinationDir.exists()) destinationDir.deleteRecursively()
        destinationDir.mkdirs()

        val destinationRoot = destinationDir.canonicalFile
        val seenEntries = HashSet<String>()
        var entryCount = 0
        var totalBytes = 0L
        var sawEntry = false

        ZipInputStream(FileInputStream(archiveFile).buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                sawEntry = true
                entryCount++
                if (entryCount > MAX_ARCHIVE_ENTRIES) {
                    throw IOException("Archive contains too many entries")
                }

                val entryName = entry.name.replace('\\', '/')
                if (entryName.isBlank() || entryName.length > MAX_ENTRY_NAME_LENGTH || entryName.contains('\u0000')) {
                    throw IOException("Invalid archive entry name")
                }
                val segments = entryName.split('/')
                if (segments.any { it == ".." } || entryName.startsWith('/') ||
                    Regex("^[A-Za-z]:/").containsMatchIn(entryName)) {
                    throw IOException("Archive entry escapes destination: ${entry.name}")
                }
                val relativeName = segments.filter { it.isNotEmpty() && it != "." }.joinToString("/")
                if (relativeName.isBlank()) {
                    zis.closeEntry()
                    entry = zis.nextEntry
                    continue
                }
                if (!seenEntries.add(relativeName.lowercase())) {
                    throw IOException("Duplicate archive entry: $entryName")
                }

                val targetFile = File(destinationRoot, relativeName).canonicalFile
                val rootPath = destinationRoot.path
                if (targetFile.path != rootPath && !targetFile.path.startsWith(rootPath + File.separator)) {
                    throw IOException("Archive entry escapes destination: ${entry.name}")
                }
                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    if (entry.size > MAX_ENTRY_UNCOMPRESSED_BYTES) {
                        throw IOException("Archive entry is too large: $entryName")
                    }
                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile).buffered().use { fos ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var entryBytes = 0L
                        while (true) {
                            val read = zis.read(buffer)
                            if (read < 0) break
                            entryBytes += read
                            totalBytes += read
                            if (entryBytes > MAX_ENTRY_UNCOMPRESSED_BYTES || totalBytes > MAX_ARCHIVE_UNCOMPRESSED_BYTES) {
                                throw IOException("Archive expands beyond the allowed size")
                            }
                            fos.write(buffer, 0, read)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        if (!sawEntry) {
            throw IOException("Archive is empty or invalid")
        }
    }

    suspend fun createArchive(sourceDir: File, targetArchiveFile: File) = withContext(Dispatchers.IO) {
        targetArchiveFile.parentFile?.mkdirs()
        ZipOutputStream(FileOutputStream(targetArchiveFile).buffered()).use { zos ->
            val files = sourceDir.walkTopDown().filter { it.isFile }.toList()
            for (file in files) {
                val relPath = file.relativeTo(sourceDir).path.replace('\\', '/')
                val zipEntry = ZipEntry(relPath)
                zos.putNextEntry(zipEntry)
                FileInputStream(file).buffered().use { fis ->
                    fis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
    }
}
