package com.spotifyhub.spotify.dto.auth

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TokenResponseDtoJsonTest {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `token response adapter parses spotify auth payload`() {
        val adapter = moshi.adapter(TokenResponseDto::class.java)
        val response = adapter.fromJson(
            """
            {
              "access_token": "access-token",
              "token_type": "Bearer",
              "expires_in": 3600,
              "refresh_token": "refresh-token",
              "scope": "user-read-private"
            }
            """.trimIndent(),
        )

        assertNotNull(response)
        assertEquals("access-token", response?.accessToken)
        assertEquals("Bearer", response?.tokenType)
        assertEquals(3600L, response?.expiresIn)
        assertEquals("refresh-token", response?.refreshToken)
        assertEquals("user-read-private", response?.scope)
    }
}
