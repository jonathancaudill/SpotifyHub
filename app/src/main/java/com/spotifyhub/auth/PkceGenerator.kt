package com.spotifyhub.auth

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

data class PkcePair(
    val codeVerifier: String,
    val codeChallenge: String,
)

object PkceGenerator {
    fun generate(): PkcePair {
        val codeVerifier = ByteArray(64).also(SecureRandom()::nextBytes)
            .let(::base64Url)

        val sha256 = MessageDigest.getInstance("SHA-256")
        val codeChallenge = base64Url(sha256.digest(codeVerifier.toByteArray()))

        return PkcePair(
            codeVerifier = codeVerifier,
            codeChallenge = codeChallenge,
        )
    }

    private fun base64Url(bytes: ByteArray): String {
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        )
    }
}

