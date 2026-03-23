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

    private fun createAuthenticatedClient(authRepository: SpotifyAuthRepository): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor(authRepository))
            .addInterceptor(RetryAfterInterceptor())
            .authenticator(TokenRefreshAuthenticator(authRepository))
            .build()
    }

    private fun createAuthenticatedRetrofit(
        moshi: Moshi,
        authRepository: SpotifyAuthRepository,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.spotify.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(createAuthenticatedClient(authRepository))
            .build()
    }

    fun createPlayerApi(
        moshi: Moshi,
        authRepository: SpotifyAuthRepository,
    ): SpotifyPlayerApi {
        return createAuthenticatedRetrofit(moshi, authRepository)
            .create(SpotifyPlayerApi::class.java)
    }

    fun createLibraryApi(
        moshi: Moshi,
        authRepository: SpotifyAuthRepository,
    ): SpotifyLibraryApi {
        return createAuthenticatedRetrofit(moshi, authRepository)
            .create(SpotifyLibraryApi::class.java)
    }

    fun createBrowseApi(
        moshi: Moshi,
        authRepository: SpotifyAuthRepository,
    ): SpotifyBrowseApi {
        return createAuthenticatedRetrofit(moshi, authRepository)
            .create(SpotifyBrowseApi::class.java)
    }

    fun createSearchApi(
        moshi: Moshi,
        authRepository: SpotifyAuthRepository,
    ): SpotifySearchApi {
        return createAuthenticatedRetrofit(moshi, authRepository)
            .create(SpotifySearchApi::class.java)
    }

    fun createEmbedApi(moshi: Moshi): SpotifyEmbedApi {
        return Retrofit.Builder()
            .baseUrl("https://open.spotify.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(OkHttpClient.Builder().build())
            .build()
            .create(SpotifyEmbedApi::class.java)
    }
}
