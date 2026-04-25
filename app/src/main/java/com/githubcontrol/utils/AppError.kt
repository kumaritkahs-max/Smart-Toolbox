package com.githubcontrol.utils

import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Friendly error envelope used by the Retry-aware UI components ([ErrorBanner]
 * and the new [com.githubcontrol.ui.screens.health.HealthScreen]). Every
 * network call should funnel its failures through [AppError.from] so the user
 * sees a sentence they can act on instead of a stack trace.
 */
sealed class AppError(open val message: String, open val cause: Throwable? = null) {
    object NoNetwork : AppError("You're offline — check Wi-Fi or mobile data and try again.")
    object Timeout : AppError("The request timed out. The network may be slow — please retry.")
    data class AuthExpired(val detail: String? = null) :
        AppError("Your sign-in is no longer accepted by GitHub. Re-authenticate in Settings → Accounts.")
    data class Forbidden(val detail: String? = null) :
        AppError("GitHub denied that request. Your token may be missing a scope. ${detail.orEmpty()}")
    data class NotFound(val detail: String? = null) :
        AppError("That resource doesn't exist (or isn't visible to your token).")
    data class Conflict(val detail: String? = null) :
        AppError("Conflict on the server (often: someone pushed first). Pull, then retry.")
    data class RateLimited(val resetSeconds: Long?) : AppError(
        "GitHub rate-limit reached." + (resetSeconds?.let { " Try again in ${it}s." } ?: "")
    )
    data class PushRejected(val detail: String) :
        AppError("Push rejected — the remote has new commits. Pull, merge, then push again.")
    data class HttpError(val code: Int, val detail: String?) :
        AppError("Request failed (HTTP $code)${detail?.let { ": $it" }.orEmpty()}")
    data class Unknown(val raw: String, override val cause: Throwable? = null) :
        AppError("Something failed: $raw", cause)

    companion object {
        fun from(t: Throwable): AppError = when (t) {
            is UnknownHostException -> NoNetwork
            is SocketTimeoutException -> Timeout
            is IOException -> NoNetwork
            is HttpException -> fromHttp(t.code(), t.response()?.errorBody()?.string())
            else -> Unknown(t.message ?: t.javaClass.simpleName, t)
        }

        fun fromResponse(resp: Response<*>): AppError =
            fromHttp(resp.code(), resp.errorBody()?.string())

        fun fromHttp(code: Int, body: String?): AppError = when (code) {
            401 -> AuthExpired(body)
            403 -> if (body?.contains("rate limit", ignoreCase = true) == true ||
                       body?.contains("secondary", ignoreCase = true) == true)
                RateLimited(null)
            else Forbidden(body)
            404 -> NotFound(body)
            409, 422 -> if (body?.contains("non-fast-forward", ignoreCase = true) == true ||
                            body?.contains("rejected", ignoreCase = true) == true)
                PushRejected(body) else Conflict(body)
            429 -> RateLimited(null)
            in 500..599 -> HttpError(code, "GitHub is having a problem (server error).")
            else -> HttpError(code, body)
        }
    }
}

/** Retry the suspending block up to [times] with exponential backoff. */
suspend fun <T> retry(
    times: Int = 3,
    initialDelayMs: Long = 800L,
    factor: Double = 2.0,
    block: suspend (attempt: Int) -> T
): T {
    var delay = initialDelayMs
    var lastError: Throwable? = null
    repeat(times) { attempt ->
        try {
            return block(attempt + 1)
        } catch (t: Throwable) {
            lastError = t
            Logger.w("Retry", "attempt ${attempt + 1}/$times failed: ${t.message}")
            kotlinx.coroutines.delay(delay)
            delay = (delay * factor).toLong().coerceAtMost(15_000L)
        }
    }
    throw lastError ?: IllegalStateException("retry exhausted")
}
