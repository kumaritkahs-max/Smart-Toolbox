package com.githubcontrol.utils

import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

object RelativeTime {
    fun ago(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        val past = try { Instant.parse(iso) } catch (e: DateTimeParseException) { return "" }
        return formatDuration(Duration.between(past, Instant.now()).seconds)
    }

    /** Same humanised format for a unix-millis timestamp (0 returns "never"). */
    fun format(epochMillis: Long): String {
        if (epochMillis <= 0L) return "never"
        val s = (System.currentTimeMillis() - epochMillis) / 1000L
        return if (s < 0) "in the future" else formatDuration(s)
    }

    private fun formatDuration(s: Long): String = when {
        s < 60 -> "just now"
        s < 3600 -> "${s / 60}m ago"
        s < 86400 -> "${s / 3600}h ago"
        s < 2592000 -> "${s / 86400}d ago"
        s < 31536000 -> "${s / 2592000}mo ago"
        else -> "${s / 31536000}y ago"
    }
}
