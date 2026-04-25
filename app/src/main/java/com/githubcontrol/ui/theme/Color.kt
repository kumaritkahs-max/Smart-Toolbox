package com.githubcontrol.ui.theme

import androidx.compose.ui.graphics.Color

// GitHub-inspired tokens
val GhInk = Color(0xFF0D1117)
val GhInkAmoled = Color(0xFF000000)
val GhSurface = Color(0xFF161B22)
val GhSurface2 = Color(0xFF1F2630)
val GhBorder = Color(0xFF30363D)
val GhMuted = Color(0xFF8B949E)
val GhText = Color(0xFFE6EDF3)
val GhAccent = Color(0xFF2F81F7)
val GhSuccess = Color(0xFF3FB950)
val GhWarn = Color(0xFFD29922)
val GhDanger = Color(0xFFF85149)
val GhPurple = Color(0xFFD2A8FF)

val GhInkLight = Color(0xFFF6F8FA)
val GhSurfaceLight = Color(0xFFFFFFFF)
val GhBorderLight = Color(0xFFD0D7DE)
val GhMutedLight = Color(0xFF656D76)
val GhTextLight = Color(0xFF1F2328)

/** Picker-friendly accent palette. */
object AccentPalette {
    data class Swatch(val key: String, val label: String, val color: Color)

    val all: List<Swatch> = listOf(
        Swatch("blue",   "Blue",    Color(0xFF2F81F7)),
        Swatch("purple", "Purple",  Color(0xFF8957E5)),
        Swatch("pink",   "Pink",    Color(0xFFEC4899)),
        Swatch("red",    "Red",     Color(0xFFE5534B)),
        Swatch("orange", "Orange",  Color(0xFFFB8500)),
        Swatch("yellow", "Yellow",  Color(0xFFEAC54F)),
        Swatch("green",  "Green",   Color(0xFF3FB950)),
        Swatch("teal",   "Teal",    Color(0xFF14B8A6)),
        Swatch("cyan",   "Cyan",    Color(0xFF06B6D4)),
        Swatch("indigo", "Indigo",  Color(0xFF6366F1)),
        Swatch("slate",  "Slate",   Color(0xFF64748B))
    )

    fun byKey(key: String): Color = all.firstOrNull { it.key == key }?.color ?: all.first().color
}

/** Terminal/code-block palette. */
object TerminalPalette {
    data class Theme(val key: String, val label: String, val bg: Color, val fg: Color)

    val all: List<Theme> = listOf(
        Theme("github-dark", "GitHub dark", Color(0xFF0D1117), Color(0xFFE6EDF3)),
        Theme("github-light","GitHub light",Color(0xFFF6F8FA), Color(0xFF1F2328)),
        Theme("dracula",     "Dracula",     Color(0xFF282A36), Color(0xFFF8F8F2)),
        Theme("solar-dark",  "Solarized dark", Color(0xFF002B36), Color(0xFF93A1A1)),
        Theme("solar-light", "Solarized light", Color(0xFFFDF6E3), Color(0xFF586E75)),
        Theme("monokai",     "Monokai",     Color(0xFF272822), Color(0xFFF8F8F2)),
        Theme("nord",        "Nord",        Color(0xFF2E3440), Color(0xFFD8DEE9)),
        Theme("matrix",      "Matrix",      Color(0xFF000000), Color(0xFF00FF66))
    )

    fun byKey(key: String): Theme = all.firstOrNull { it.key == key } ?: all.first()
}
