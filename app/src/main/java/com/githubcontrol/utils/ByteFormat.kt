package com.githubcontrol.utils

import java.text.DecimalFormat

object ByteFormat {
    fun human(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var b = bytes.toDouble() / 1024
        var i = 0
        while (b >= 1024 && i < units.size - 1) { b /= 1024; i++ }
        return DecimalFormat("#.#").format(b) + " " + units[i]
    }

    fun rate(bytesPerSec: Double): String {
        if (bytesPerSec <= 0) return "0 B/s"
        return human(bytesPerSec.toLong()) + "/s"
    }
}
