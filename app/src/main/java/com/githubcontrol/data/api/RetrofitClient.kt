package com.githubcontrol.data.api

import com.githubcontrol.data.auth.AccountManager
import com.githubcontrol.utils.Logger
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitClient @Inject constructor(
    private val accountManager: AccountManager
) {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

    private val authInterceptor = Interceptor { chain ->
        val token = runBlocking { accountManager.activeToken() }
        val req = chain.request().newBuilder()
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")
            .addHeader("User-Agent", "GitHubControl-Android")
            .apply { if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token") }
            .build()
        chain.proceed(req)
    }

    private val rateLimitInterceptor = Interceptor { chain ->
        var resp = chain.proceed(chain.request())
        resp.header("X-RateLimit-Remaining")?.toIntOrNull()?.let { rem ->
            accountManager.updateRateRemaining(rem)
        }
        // If GitHub says we're rate-limited, look at how long we should wait
        // (X-RateLimit-Reset is epoch-seconds; secondary limits use Retry-After
        // in seconds). Auto-delay up to 30s and try again once.
        if (resp.code == 403 || resp.code == 429) {
            val retryAfter = resp.header("Retry-After")?.toLongOrNull()
            val resetEpoch = resp.header("X-RateLimit-Reset")?.toLongOrNull()
            val waitSec = retryAfter ?: resetEpoch?.let { it - System.currentTimeMillis() / 1000 } ?: 0L
            if (waitSec in 1..30) {
                Logger.w("HTTP", "rate-limited — waiting ${waitSec}s before one retry")
                resp.close()
                runCatching { Thread.sleep(waitSec * 1000) }
                resp = chain.proceed(chain.request())
                resp.header("X-RateLimit-Remaining")?.toIntOrNull()?.let { rem ->
                    accountManager.updateRateRemaining(rem)
                }
            }
        }
        // Cache the active token's scopes whenever the server reports them.
        resp.header("X-OAuth-Scopes")?.let { scopesHeader ->
            val scopes = scopesHeader.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (scopes.isNotEmpty()) {
                runBlocking {
                    accountManager.activeAccount()?.let { acc ->
                        if (acc.scopes != scopes) accountManager.updateScopes(acc.id, scopes)
                    }
                }
            }
        }
        resp
    }

    /**
     * Custom logging interceptor that pipes every HTTP exchange into the in-app
     * [Logger] so the terminal panels can show real network traffic. We never
     * log the Authorization header value here — Logger redacts it as a safety
     * net but we drop it explicitly anyway.
     */
    private val terminalLogInterceptor = Interceptor { chain ->
        val req = chain.request()
        val started = System.currentTimeMillis()
        Logger.net("HTTP", "→ ${req.method} ${req.url.encodedPath}${req.url.query?.let { "?$it" } ?: ""}")
        try {
            val resp = chain.proceed(req)
            val ms = System.currentTimeMillis() - started
            val rate = resp.header("X-RateLimit-Remaining")
            val rateStr = if (rate != null) "  rl=$rate" else ""
            Logger.net("HTTP", "← ${resp.code} ${req.method} ${req.url.encodedPath}  ${ms}ms$rateStr")
            resp
        } catch (t: Throwable) {
            Logger.e("HTTP", "✕ ${req.method} ${req.url.encodedPath} :: ${t.javaClass.simpleName}: ${t.message}")
            throw t
        }
    }

    private val client by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(rateLimitInterceptor)
            .addInterceptor(terminalLogInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    val api: GitHubApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubApi::class.java)
    }

    val rawClient: OkHttpClient get() = client
}
