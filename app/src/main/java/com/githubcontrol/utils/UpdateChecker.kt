package com.githubcontrol.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Tiny self-update checker that compares this build's version to the latest
 * GitHub Release of [BuildInfo.GITHUB_OWNER]/[BuildInfo.GITHUB_REPO]. Returns
 * a [Result] with the comparison so the UI can show "You're up to date" or
 * "Update available — vX.Y.Z".
 */
object UpdateChecker {
    @Serializable
    data class GhRelease(
        val tag_name: String? = null,
        val name: String? = null,
        val html_url: String? = null,
        val body: String? = null,
        val published_at: String? = null,
        val prerelease: Boolean = false
    )

    data class Result(
        val current: String,
        val latest: String?,
        val newer: Boolean,
        val htmlUrl: String?,
        val notes: String?,
        val publishedAt: String?
    )

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val client = OkHttpClient()

    suspend fun check(): kotlin.Result<Result> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://api.github.com/repos/${BuildInfo.GITHUB_OWNER}/${BuildInfo.GITHUB_REPO}/releases/latest"
            val req = Request.Builder().url(url)
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("X-GitHub-Api-Version", "2022-11-28")
                .addHeader("User-Agent", "GitHubControl-Android")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@runCatching Result(BuildInfo.VERSION_NAME, null, false, null,
                        "No releases published yet (${resp.code}).", null)
                }
                val body = resp.body?.string() ?: error("empty body")
                val rel = json.decodeFromString<GhRelease>(body)
                val latest = rel.tag_name?.removePrefix("v")?.trim()
                val newer = latest != null && compareVersions(latest, BuildInfo.VERSION_NAME) > 0
                Result(BuildInfo.VERSION_NAME, latest, newer, rel.html_url, rel.body, rel.published_at)
            }
        }
    }

    private fun compareVersions(a: String, b: String): Int {
        val pa = a.split(".", "-").mapNotNull { it.toIntOrNull() }
        val pb = b.split(".", "-").mapNotNull { it.toIntOrNull() }
        val len = maxOf(pa.size, pb.size)
        for (i in 0 until len) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }
}
