package com.spotifyhub.app

import android.content.Context
import com.spotifyhub.auth.EncryptedPrefsTokenStore
import com.spotifyhub.auth.SpotifyAuthRepository
import com.spotifyhub.auth.TokenStore
import com.spotifyhub.playback.PlaybackRepository
import com.spotifyhub.spotify.api.SpotifyAccountsApi
import com.spotifyhub.spotify.api.SpotifyNetworkModule
import com.spotifyhub.spotify.api.SpotifyPlayerApi
import com.spotifyhub.system.input.InputRouter
import com.spotifyhub.system.network.ConnectivityMonitor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppGraph(private val appContext: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val tokenStore: TokenStore by lazy {
        EncryptedPrefsTokenStore(appContext)
    }

    val authRepository: SpotifyAuthRepository by lazy {
        SpotifyAuthRepository(
            appContext = appContext,
            tokenStore = tokenStore,
            accountsApi = accountsApi,
        )
    }

    val accountsApi: SpotifyAccountsApi by lazy {
        SpotifyNetworkModule.createAccountsApi(moshi)
    }

    val playerApi: SpotifyPlayerApi by lazy {
        SpotifyNetworkModule.createPlayerApi(
            moshi = moshi,
            authRepository = authRepository,
        )
    }

    val playbackRepository: PlaybackRepository by lazy {
        PlaybackRepository(
            appScope = applicationScope,
            authRepository = authRepository,
            playerApi = playerApi,
        )
    }

    val connectivityMonitor: ConnectivityMonitor by lazy {
        ConnectivityMonitor(appContext)
    }

    val inputRouter: InputRouter by lazy {
        InputRouter()
    }
}

