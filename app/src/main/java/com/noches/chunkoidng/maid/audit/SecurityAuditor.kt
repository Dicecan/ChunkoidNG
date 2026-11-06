package com.noches.chunkoidng.maid.audit

import android.util.Log
import com.noches.chunkoidng.maid.model.SecurityViolation
import com.noches.chunkoidng.maid.model.ViolationSeverity
import com.noches.chunkoidng.maid.model.ViolationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque

class SecurityAuditor(private val capacity: Int = 100) {

    private val TAG = "MaidSecurityAuditor"
    private val lock = Any()
    private val buffer = ArrayDeque<SecurityViolation>(capacity)

    private val _violationsFlow = MutableStateFlow<List<SecurityViolation>>(emptyList())
    val violationsFlow: StateFlow<List<SecurityViolation>> = _violationsFlow.asStateFlow()

    fun record(violation: SecurityViolation) {
        synchronized(lock) {
            if (buffer.size >= capacity) {
                buffer.pollFirst()
            }
            buffer.addLast(violation)
            _violationsFlow.value = buffer.toList()
        }

        try {
            when (violation.severity) {
                ViolationSeverity.BLOCKED -> Log.w(TAG, "[BLOCKED] [${violation.module}] ${violation.type}: ${violation.message} (target: ${violation.pathOrTarget})")
                ViolationSeverity.GENTLE_TRUNCATED -> Log.i(TAG, "[TRUNCATED] [${violation.module}] ${violation.type}: ${violation.message} (requested=${violation.requestedValue} -> clamped=${violation.clampedValue})")
                ViolationSeverity.WARNING -> Log.d(TAG, "[WARNING] [${violation.module}] ${violation.type}: ${violation.message}")
            }
        } catch (_: Throwable) {
            println("[$TAG] [${violation.severity}] [${violation.module}] ${violation.type}: ${violation.message}")
        }
    }

    fun recordBlock(
        type: ViolationType,
        module: String,
        message: String,
        pathOrTarget: String? = null,
        requestedValue: Long? = null
    ) {
        record(
            SecurityViolation(
                severity = ViolationSeverity.BLOCKED,
                type = type,
                module = module,
                message = message,
                pathOrTarget = pathOrTarget,
                requestedValue = requestedValue
            )
        )
    }

    fun recordGentleTruncate(
        type: ViolationType,
        module: String,
        message: String,
        requestedValue: Long,
        clampedValue: Long,
        pathOrTarget: String? = null
    ) {
        record(
            SecurityViolation(
                severity = ViolationSeverity.GENTLE_TRUNCATED,
                type = type,
                module = module,
                message = message,
                pathOrTarget = pathOrTarget,
                requestedValue = requestedValue,
                clampedValue = clampedValue
            )
        )
    }

    fun getRecentViolations(): List<SecurityViolation> {
        synchronized(lock) {
            return buffer.toList().reversed()
        }
    }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            _violationsFlow.value = emptyList()
        }
    }
}
