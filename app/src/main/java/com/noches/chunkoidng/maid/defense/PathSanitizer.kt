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
        return normalized.replace(Regex("[\\\\/:*?\"<>|\r\n\t]"), "_").trim()
    }

    fun resolveUnderBase(
        baseDir: File,
        entryPath: String,
        module: String = "Defense"
    ): File {
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
                message = "检测到Zip-Slip路径跨目录逃逸攻击，已严格拦截",
                pathOrTarget = entryPath
            )
            throw SecurityException("Security violation: path traverses outside base directory: $entryPath")
        }

        return targetFile
    }
}
