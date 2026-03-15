package com.spotifyhub.auth

data class StoredToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
)

interface TokenStore {
    fun read(): StoredToken?
    fun write(token: StoredToken)
    fun clear()
}

