package com.githubcontrol.ai

import com.githubcontrol.upload.UploadJob

/** Heuristic commit-message generator (offline, deterministic). */
object CommitAi {
    fun suggest(job: UploadJob): String {
        if (job.files.isEmpty()) return "chore: empty commit"
        val byExt = job.files.groupingBy { it.targetPath.substringAfterLast('.', "").lowercase() }.eachCount()
        val top = byExt.entries.sortedByDescending { it.value }.take(3)
            .joinToString(", ") { "${it.value} ${if (it.key.isBlank()) "files" else ".${it.key}"}" }
        val rootDir = job.files.first().targetPath.substringBeforeLast('/', "")
        val scope = if (rootDir.isNotEmpty()) "($rootDir)" else ""
        val verb = when {
            job.files.size == 1 -> "add ${job.files.first().displayName}"
            job.files.size < 10 -> "add $top"
            else -> "import ${job.files.size} files ($top)"
        }
        return "feat$scope: $verb"
    }
}
