package com.spotifyhub.spotify.api

import com.spotifyhub.auth.SpotifyAuthRepository
import okhttp3.Interceptor
import okhttp3.Response

class AuthHeaderInterceptor(
    private val authRepository: SpotifyAuthRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = authRepository.currentAccessToken()
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }
}

