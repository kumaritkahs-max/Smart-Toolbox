package com.githubcontrol.data.auth

import com.githubcontrol.utils.Logger
import okhttp3.OkHttpClient
import okhttp3.Request

object TokenValidator {

    /** Detect the kind of token from its prefix. */
    fun classify(token: String): String = when {
        token.startsWith("github_pat_") -> "fine-grained"
        token.startsWith("ghp_") -> "classic"
        token.startsWith("gho_") -> "oauth"
        token.startsWith("ghu_") -> "user-to-server"
        token.startsWith("ghs_") -> "server-to-server"
        token.startsWith("ghr_") -> "refresh"
        else -> "unknown"
    }

    /**
     * Perform a live validation by calling /user. Returns a [TokenValidation]
     * record (suitable for archival) along with the parsed user details when
     * the request succeeded.
     */
    data class Result(
        val validation: TokenValidation,
        val login: String? = null,
        val avatarUrl: String? = null,
        val name: String? = null,
        val email: String? = null,
        val bio: String? = null,
        val publicRepos: Int = 0,
        val followers: Int = 0
    ) {
        val ok get() = validation.ok
        val scopes get() = validation.scopes
        val rateLimit get() = validation.rateLimit
        val error get() = validation.error
    }

    fun validate(token: String, client: OkHttpClient = OkHttpClient()): Result {
        val type = classify(token)
        return try {
            val req = Request.Builder()
                .url("https://api.github.com/user")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "GitHubControl-Android")
                .build()
            client.newCall(req).execute().use { resp ->
                val scopes = resp.header("X-OAuth-Scopes")
                    ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                val accepted = resp.header("X-Accepted-OAuth-Scopes")
                    ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                val rateRemaining = resp.header("X-RateLimit-Remaining")?.toIntOrNull()
                val rateMax = resp.header("X-RateLimit-Limit")?.toIntOrNull()
                val expiry = resp.header("GitHub-Authentication-Token-Expiration")
                Logger.i("TokenValidator", "validate ${type} → ${resp.code} scopes=${scopes.size} rl=$rateRemaining")
                if (!resp.isSuccessful) {
                    val v = TokenValidation(
                        ts = System.currentTimeMillis(),
                        ok = false, httpCode = resp.code,
                        scopes = scopes, acceptedScopes = accepted,
                        rateLimit = rateRemaining, rateMax = rateMax,
                        tokenType = type, tokenExpiry = expiry,
                        error = "HTTP ${resp.code} ${resp.message}"
                    )
                    return Result(v)
                }
                val body = resp.body?.string().orEmpty()
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val user = json.decodeFromString<com.githubcontrol.data.api.GhUser>(body)
                val v = TokenValidation(
                    ts = System.currentTimeMillis(),
                    ok = true, httpCode = resp.code,
                    scopes = scopes, acceptedScopes = accepted,
                    rateLimit = rateRemaining, rateMax = rateMax,
                    tokenType = type, tokenExpiry = expiry, error = null
                )
                Result(
                    validation = v,
                    login = user.login, avatarUrl = user.avatarUrl,
                    name = user.name, email = user.email, bio = user.bio,
                    publicRepos = user.publicRepos, followers = user.followers
                )
            }
        } catch (t: Throwable) {
            Logger.e("TokenValidator", "validate failed", t)
            Result(
                TokenValidation(
                    ts = System.currentTimeMillis(), ok = false, httpCode = null,
                    scopes = emptyList(), acceptedScopes = emptyList(),
                    rateLimit = null, rateMax = null,
                    tokenType = type, tokenExpiry = null,
                    error = t.message ?: "Unknown error"
                )
            )
        }
    }
}
