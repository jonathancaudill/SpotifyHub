package com.spotifyhub.spotify.api

import com.spotifyhub.auth.SpotifyAuthRepository
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object SpotifyNetworkModule {
    fun createAccountsApi(moshi: Moshi): SpotifyAccountsApi {
        return Retrofit.Builder()
            .baseUrl("https://accounts.spotify.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(OkHttpClient.Builder().build())
            .build()
            .create(SpotifyAccountsApi::class.java)
    }

    fun createPlayerApi(
        moshi: Moshi,
        authRepository: SpotifyAuthRepository,
    ): SpotifyPlayerApi {
        return Retrofit.Builder()
            .baseUrl("https://api.spotify.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(AuthHeaderInterceptor(authRepository))
                    .addInterceptor(RetryAfterInterceptor())
                    .authenticator(TokenRefreshAuthenticator(authRepository))
                    .build(),
            )
            .build()
            .create(SpotifyPlayerApi::class.java)
    }
}

