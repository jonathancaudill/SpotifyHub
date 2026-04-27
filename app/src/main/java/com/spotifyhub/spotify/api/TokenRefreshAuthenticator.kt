package com.spotifyhub.spotify.api

import com.spotifyhub.auth.SpotifyAuthRepository
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenRefreshAuthenticator(
    private val authRepository: SpotifyAuthRepository,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) {
            return null
        }

        val refreshedToken = authRepository.blockingRefreshAccessToken() ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $refreshedToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var current: Response? = response
        var count = 1
        while (current?.priorResponse != null) {
            count += 1
            current = current.priorResponse
        }
        return count
    }

    companion object {
        private const val MAX_AUTHENTICATION_ATTEMPTS = 2
    }
}

