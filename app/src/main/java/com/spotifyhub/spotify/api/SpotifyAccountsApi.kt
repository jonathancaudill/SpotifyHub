package com.spotifyhub.spotify.api

import com.spotifyhub.spotify.dto.auth.TokenResponseDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface SpotifyAccountsApi {
    @FormUrlEncoded
    @POST("api/token")
    suspend fun exchangeToken(
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("code_verifier") codeVerifier: String,
    ): TokenResponseDto

    @FormUrlEncoded
    @POST("api/token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String,
        @Field("refresh_token") refreshToken: String,
    ): TokenResponseDto
}

