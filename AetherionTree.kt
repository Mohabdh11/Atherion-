package com.aetherion.noc.core.logging

import android.util.Log
import timber.log.Timber

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Production Logging Tree
// Developer: Mohammad Abdalftah Ibrahime
//
// In production builds:
//   • Debug/Info logs are suppressed entirely (zero sensitive data exposure)
//   • Warnings and Errors are sent to Android logcat only
//   • PII-containing patterns are scrubbed before logging
//
// NOTE: Firebase Crashlytics integration is disabled in this build.
//       To enable, add google-services.json and re-enable Firebase plugins.
// ═══════════════════════════════════════════════════════════════════════════

class AetherionTree : Timber.Tree() {

    // Patterns that must never appear in production logs
    private val piiPatterns = listOf(
        Regex("Bearer\\s+[A-Za-z0-9\\-._~+/]+=*"),
        Regex("password[\":\\s=]+[^\\s,}\"]+", RegexOption.IGNORE_CASE),
        Regex("([0-9]{1,3}\\.){3}[0-9]{1,3}"),
        Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    )

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.WARN
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val sanitized = sanitize(message)
        when (priority) {
            Log.WARN  -> Log.w(tag, sanitized)
            Log.ERROR -> Log.e(tag, sanitized, t)
        }
    }

    private fun sanitize(message: String): String {
        var result = message
        piiPatterns.forEach { pattern ->
            result = result.replace(pattern, "[REDACTED]")
        }
        return result
    }
}
