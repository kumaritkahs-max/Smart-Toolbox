package com.githubcontrol.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Lightweight in-memory ring-buffer logger that powers the in-app terminal log
 * panels. Designed to be safe on low-end phones: bounded, lock-free for reads,
 * and synchronised only for the small write path. Sensitive values
 * (PAT tokens, Authorization headers, secrets) are redacted before storage.
 */
object Logger {

    enum class Level(val tag: String) { D("DEBUG"), I("INFO"), W("WARN"), E("ERROR"), N("NET") }

    data class Entry(
        val id: Long,
        val ts: Long,
        val level: Level,
        val tag: String,
        val message: String
    ) {
        fun formatted(): String {
            val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(ts))
            return "$time  ${level.tag.padEnd(5)} [${tag.take(18).padEnd(18)}] $message"
        }
    }

    private const val CAPACITY = 1500
    private val ids = AtomicLong(1)
    private val buffer = ArrayDeque<Entry>(CAPACITY)

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    @Synchronized
    fun log(level: Level, tag: String, message: String) {
        val entry = Entry(ids.getAndIncrement(), System.currentTimeMillis(), level, tag, redact(message))
        if (buffer.size >= CAPACITY) buffer.removeFirst()
        buffer.addLast(entry)
        _entries.value = buffer.toList()
        // Mirror to logcat for adb / Android Studio diagnostics.
        when (level) {
            Level.E -> android.util.Log.e(tag, entry.message)
            Level.W -> android.util.Log.w(tag, entry.message)
            Level.N -> android.util.Log.d("net/$tag", entry.message)
            Level.I -> android.util.Log.i(tag, entry.message)
            Level.D -> android.util.Log.d(tag, entry.message)
        }
    }

    fun d(tag: String, message: String) = log(Level.D, tag, message)
    fun i(tag: String, message: String) = log(Level.I, tag, message)
    fun w(tag: String, message: String) = log(Level.W, tag, message)
    fun e(tag: String, message: String, t: Throwable? = null) =
        log(Level.E, tag, if (t == null) message else "$message :: ${t.javaClass.simpleName}: ${t.message}")
    fun net(tag: String, message: String) = log(Level.N, tag, message)

    @Synchronized
    fun clear() {
        buffer.clear()
        _entries.value = emptyList()
    }

    fun snapshot(filter: Level? = null): List<Entry> {
        val now = _entries.value
        return if (filter == null) now else now.filter { it.level == filter || filter == Level.D }
    }

    fun fullText(filter: Level? = null): String =
        snapshot(filter).joinToString("\n") { it.formatted() }

    /**
     * Redact obvious secrets. Recognises GitHub PAT prefixes and Authorization
     * headers so we never persist raw tokens to memory or the clipboard.
     */
    private val tokenRegex = Regex("(?i)(gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})")
    private val authHeaderRegex = Regex("(?i)(authorization:\\s*(?:bearer|token)\\s+)([^\\s\"']+)")

    private fun redact(input: String): String {
        var out = tokenRegex.replace(input) { m -> m.value.take(4) + "***" + m.value.takeLast(4) }
        out = authHeaderRegex.replace(out) { m -> m.groupValues[1] + "***redacted***" }
        return out
    }
}
