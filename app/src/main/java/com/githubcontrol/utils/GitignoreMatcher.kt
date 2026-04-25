package com.githubcontrol.utils

/** Lightweight .gitignore matcher (subset: glob, **, leading/trailing slashes, negation with !). */
class GitignoreMatcher(rules: List<String>) {
    private data class Rule(val pattern: Regex, val negate: Boolean, val dirOnly: Boolean)

    private val parsed: List<Rule> = rules
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { raw ->
            var p = raw
            val negate = p.startsWith("!")
            if (negate) p = p.removePrefix("!")
            val dirOnly = p.endsWith("/")
            if (dirOnly) p = p.removeSuffix("/")
            val rooted = p.startsWith("/")
            if (rooted) p = p.removePrefix("/")
            val regex = StringBuilder("^")
            if (!rooted) regex.append("(.*/)?")
            var i = 0
            while (i < p.length) {
                val c = p[i]
                when {
                    c == '*' && i + 1 < p.length && p[i + 1] == '*' -> {
                        regex.append(".*"); i += 2
                        if (i < p.length && p[i] == '/') i++
                    }
                    c == '*' -> { regex.append("[^/]*"); i++ }
                    c == '?' -> { regex.append("[^/]"); i++ }
                    c == '.' || c == '+' || c == '(' || c == ')' || c == '|' || c == '^' || c == '$' || c == '{' || c == '}' || c == '\\' -> {
                        regex.append("\\").append(c); i++
                    }
                    else -> { regex.append(c); i++ }
                }
            }
            regex.append("$")
            Rule(Regex(regex.toString()), negate, dirOnly)
        }

    fun isIgnored(path: String, isDir: Boolean): Boolean {
        var ignored = false
        for (r in parsed) {
            if (r.dirOnly && !isDir) continue
            if (r.pattern.matches(path)) ignored = !r.negate
        }
        return ignored
    }
}
