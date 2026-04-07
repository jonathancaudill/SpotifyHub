package com.spotifyhub.app

import android.content.Context
import com.spotifyhub.artist.ArtistRepository
import com.spotifyhub.auth.EncryptedPrefsTokenStore
import com.spotifyhub.auth.SpotifyAuthRepository
import com.spotifyhub.auth.TokenStore
import com.spotifyhub.browse.BrowseRepository
import com.spotifyhub.library.LibraryRepository
import com.spotifyhub.playback.PlaybackRepository
import com.spotifyhub.rating.SheetsRepository
import com.spotifyhub.search.SearchRepository
import com.spotifyhub.spotify.api.SpotifyAccountsApi
import com.spotifyhub.spotify.api.SpotifyArtistApi
import com.spotifyhub.spotify.api.SpotifyBrowseApi
import com.spotifyhub.spotify.api.SpotifyEmbedApi
import com.spotifyhub.spotify.api.SpotifyLibraryApi
import com.spotifyhub.spotify.api.SpotifyNetworkModule
import com.spotifyhub.spotify.api.SpotifyPlayerApi
import com.spotifyhub.spotify.api.SpotifySearchApi
import com.spotifyhub.spotify.api.WikipediaApi
import com.spotifyhub.system.input.InputRouter
import com.spotifyhub.system.network.ConnectivityMonitor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.spotifyhub.ui.main.MainTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow

class AppGraph(private val appContext: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Shared flow so HomeViewModel can observe when the Home tab is (re-)selected. */
    val tabSelectedEvent = MutableSharedFlow<MainTab>(extraBufferCapacity = 1)

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
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

    val libraryApi: SpotifyLibraryApi by lazy {
        SpotifyNetworkModule.createLibraryApi(
            moshi = moshi,
            authRepository = authRepository,
        )
    }

    val browseApi: SpotifyBrowseApi by lazy {
        SpotifyNetworkModule.createBrowseApi(
            moshi = moshi,
            authRepository = authRepository,
        )
    }

    val searchApi: SpotifySearchApi by lazy {
        SpotifyNetworkModule.createSearchApi(
            moshi = moshi,
            authRepository = authRepository,
        )
    }

    val artistApi: SpotifyArtistApi by lazy {
        SpotifyNetworkModule.createArtistApi(
            moshi = moshi,
            authRepository = authRepository,
        )
    }

    val embedApi: SpotifyEmbedApi by lazy {
        SpotifyNetworkModule.createEmbedApi(moshi)
    }

    val wikipediaApi: WikipediaApi by lazy {
        SpotifyNetworkModule.createWikipediaApi(moshi)
    }

    val playbackRepository: PlaybackRepository by lazy {
        PlaybackRepository(
            appScope = applicationScope,
            sessionState = authRepository.sessionState,
            playerApi = playerApi,
            libraryApi = libraryApi,
        )
    }

    val browseRepository: BrowseRepository by lazy {
        BrowseRepository(
            browseApi = browseApi,
            libraryApi = libraryApi,
            searchApi = searchApi,
            embedApi = embedApi,
        )
    }

    val searchRepository: SearchRepository by lazy {
        SearchRepository(searchApi = searchApi)
    }

    val artistRepository: ArtistRepository by lazy {
        ArtistRepository(
            artistApi = artistApi,
            searchApi = searchApi,
            wikipediaApi = wikipediaApi,
        )
    }

    val sheetsRepository: SheetsRepository by lazy {
        SheetsRepository()
    }

    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(
            libraryApi = libraryApi,
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
