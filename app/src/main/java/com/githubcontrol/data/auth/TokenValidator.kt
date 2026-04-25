package com.githubcontrol.data.auth

import okhttp3.OkHttpClient
import okhttp3.Request

object TokenValidator {
    data class Result(
        val ok: Boolean,
        val login: String? = null,
        val avatarUrl: String? = null,
        val name: String? = null,
        val email: String? = null,
        val scopes: List<String> = emptyList(),
        val rateLimit: Int? = null,
        val error: String? = null
    )

    fun validate(token: String, client: OkHttpClient = OkHttpClient()): Result {
        return try {
            val req = Request.Builder()
                .url("https://api.github.com/user")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "GitHubControl-Android")
                .build()
            client.newCall(req).execute().use { resp ->
                val scopes = resp.header("X-OAuth-Scopes")?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                val rateRemaining = resp.header("X-RateLimit-Remaining")?.toIntOrNull()
                if (!resp.isSuccessful) {
                    return Result(ok = false, error = "HTTP ${resp.code}", scopes = scopes, rateLimit = rateRemaining)
                }
                val body = resp.body?.string() ?: return Result(ok = false, error = "Empty response")
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val user = json.decodeFromString<com.githubcontrol.data.api.GhUser>(body)
                Result(
                    ok = true, login = user.login, avatarUrl = user.avatarUrl,
                    name = user.name, email = user.email, scopes = scopes, rateLimit = rateRemaining
                )
            }
        } catch (t: Throwable) {
            Result(ok = false, error = t.message ?: "Unknown error")
        }
    }
}
