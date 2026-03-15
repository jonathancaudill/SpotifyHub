package com.spotifyhub.spotify.api

import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.Response

class RetryAfterInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code != 429) {
            return response
        }

        val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull() ?: return response
        response.close()
        TimeUnit.SECONDS.sleep(retryAfterSeconds.coerceAtMost(5))
        return chain.proceed(chain.request())
    }
}

