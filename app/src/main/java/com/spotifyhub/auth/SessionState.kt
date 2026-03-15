package com.spotifyhub.auth

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data object SigningIn : SessionState
    data class Ready(
        val accessToken: String,
        val expiresAtEpochSeconds: Long,
    ) : SessionState

    data class Error(val message: String) : SessionState
}

