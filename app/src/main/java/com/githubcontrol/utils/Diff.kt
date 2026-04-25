package com.githubcontrol.utils

/** Parses a unified diff "patch" into structured hunks for side-by-side rendering. */
object Diff {
    data class Line(val type: Type, val oldNum: Int?, val newNum: Int?, val text: String)
    enum class Type { CONTEXT, ADD, REMOVE, HEADER }

    fun parse(patch: String?): List<Line> {
        if (patch.isNullOrEmpty()) return emptyList()
        val out = mutableListOf<Line>()
        var oldN = 0; var newN = 0
        for (raw in patch.split("\n")) {
            when {
                raw.startsWith("@@") -> {
                    out += Line(Type.HEADER, null, null, raw)
                    val m = Regex("@@ -(\\d+)(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@").find(raw)
                    if (m != null) { oldN = m.groupValues[1].toInt(); newN = m.groupValues[2].toInt() }
                }
                raw.startsWith("+") && !raw.startsWith("+++") -> { out += Line(Type.ADD, null, newN, raw.drop(1)); newN++ }
                raw.startsWith("-") && !raw.startsWith("---") -> { out += Line(Type.REMOVE, oldN, null, raw.drop(1)); oldN++ }
                else -> { out += Line(Type.CONTEXT, oldN, newN, raw); oldN++; newN++ }
            }
        }
        return out
    }

    fun ignoreWhitespace(lines: List<Line>): List<Line> = lines.filterNot {
        (it.type == Type.ADD || it.type == Type.REMOVE) && it.text.trim().isEmpty()
    }
}
