package com.spotifyhub.spotify.api

import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.Response

class RetryAfterInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var attempt = 0

        while (attempt < MAX_RETRIES && isRetryableError(response.code)) {
            val delayMs = calculateBackoffDelay(attempt)
            response.close()
            attempt++
            TimeUnit.MILLISECONDS.sleep(delayMs)
            response = chain.proceed(request)
        }

        return response
    }

    private fun isRetryableError(code: Int): Boolean {
        return code == 429 || code == 500 || code == 502 || code == 503 || code == 504
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        return when (attempt) {
            0 -> 500L
            1 -> 1_000L
            2 -> 2_000L
            3 -> 4_000L
            else -> 5_000L
        }
    }

    companion object {
        private const val MAX_RETRIES = 4
    }
}