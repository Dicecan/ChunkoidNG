package com.noches.chunkoidng.maid.defense

import com.noches.chunkoidng.maid.audit.SecurityAuditor
import com.noches.chunkoidng.maid.model.ViolationType
import java.io.File

class PathSanitizer(private val securityAuditor: SecurityAuditor) {

    fun normalizeSeparators(path: String): String {
        return path.replace('\\', '/')
    }

    fun sanitizeFileName(name: String): String {
        val normalized = normalizeSeparators(name).substringAfterLast('/')
        val cleaned = normalized.replace(Regex("[\\\\/:*?\"<>|\r\n\t\\u0000]"), "_").trim()
        return if (cleaned.isEmpty() || cleaned == "." || cleaned == "..") "unnamed" else cleaned
    }

    fun resolveUnderBase(
        baseDir: File,
        entryPath: String,
        module: String = "Defense"
    ): File {
        if (entryPath.indexOf('\u0000') >= 0) {
            securityAuditor.recordBlock(
                type = ViolationType.ZIP_SLIP,
                module = module,
                message = "Blocked malicious path containing NUL byte",
                pathOrTarget = entryPath
            )
            throw SecurityException("Security violation: NUL byte in path: $entryPath")
        }
        val normalized = normalizeSeparators(entryPath)
        val targetFile = File(baseDir, normalized)
        val canonicalBase = baseDir.canonicalFile
        val canonicalTarget = targetFile.canonicalFile

        val baseCanonicalPath = canonicalBase.path
        val targetCanonicalPath = canonicalTarget.path

        val isInside = targetCanonicalPath == baseCanonicalPath ||
                targetCanonicalPath.startsWith(baseCanonicalPath + File.separator)

        if (!isInside) {
            securityAuditor.recordBlock(
                type = ViolationType.ZIP_SLIP,
                module = module,
                message = "Blocked Zip-Slip path traversal outside target directory",
                pathOrTarget = entryPath
            )
            throw SecurityException("Security violation: path traverses outside base directory: $entryPath")
        }

        return targetFile
    }
}
