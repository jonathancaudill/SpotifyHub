package com.spotifyhub.auth

import android.content.Context
import android.net.Uri
import com.spotifyhub.BuildConfig
import com.spotifyhub.spotify.api.SpotifyAccountsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import java.time.Instant

class SpotifyAuthRepository(
    private val appContext: Context,
    private val tokenStore: TokenStore,
    private val accountsApi: SpotifyAccountsApi,
) {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    @Volatile
    private var currentToken: StoredToken? = null

    suspend fun restoreSession() {
        val stored = tokenStore.read()
        if (stored == null) {
            currentToken = null
            _sessionState.value = SessionState.SignedOut
            return
        }

        currentToken = stored
        if (stored.expiresAtEpochSeconds <= Instant.now().epochSecond + 300) {
            val refreshed = refreshAccessTokenInternal(stored.refreshToken)
            if (!refreshed) {
                logout()
            }
            return
        }

        _sessionState.value = SessionState.Ready(
            accessToken = stored.accessToken,
            expiresAtEpochSeconds = stored.expiresAtEpochSeconds,
        )
    }

    suspend fun beginLogin(context: Context = appContext) {
        if (BuildConfig.SPOTIFY_CLIENT_ID.isBlank()) {
            _sessionState.value = SessionState.Error("SPOTIFY_CLIENT_ID is missing from Gradle properties.")
            return
        }

        _sessionState.value = SessionState.SigningIn
        val pkce = PkceGenerator.generate()
        val server = LoopbackAuthServer.bind()

        try {
            val authorizationUri = buildAuthorizationUri(
                clientId = BuildConfig.SPOTIFY_CLIENT_ID,
                redirectUri = server.redirectUri,
                codeChallenge = pkce.codeChallenge,
            )

            when (AuthLauncher.launch(context, authorizationUri)) {
                AuthLauncher.LaunchMode.CustomTabs,
                AuthLauncher.LaunchMode.BrowserIntent,
                -> Unit

                AuthLauncher.LaunchMode.Unavailable -> {
                    _sessionState.value = SessionState.Error("No browser is available to complete Spotify sign-in.")
                    return
                }
            }

            val callback = server.awaitCallback()
            val callbackUri = Uri.parse("http://127.0.0.1${callback.rawPath}")
            val authCode = callbackUri.getQueryParameter("code")
            val authError = callbackUri.getQueryParameter("error")

            if (authError != null) {
                _sessionState.value = SessionState.Error("Spotify authorization failed: $authError")
                return
            }

            if (authCode.isNullOrBlank()) {
                _sessionState.value = SessionState.Error("Spotify authorization did not return a code.")
                return
            }

            val tokenResponse = accountsApi.exchangeToken(
                clientId = BuildConfig.SPOTIFY_CLIENT_ID,
                grantType = "authorization_code",
                code = authCode,
                redirectUri = server.redirectUri,
                codeVerifier = pkce.codeVerifier,
            )
            storeToken(
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken ?: currentToken?.refreshToken.orEmpty(),
                expiresInSeconds = tokenResponse.expiresIn,
            )
        } catch (t: Throwable) {
            _sessionState.value = SessionState.Error(t.message ?: "Spotify sign-in failed.")
        } finally {
            server.close()
        }
    }

    suspend fun logout() {
        currentToken = null
        tokenStore.clear()
        _sessionState.value = SessionState.SignedOut
    }

    fun currentAccessToken(): String? = currentToken?.accessToken

    fun blockingRefreshAccessToken(): String? = runBlocking {
        val refreshToken = currentToken?.refreshToken ?: return@runBlocking null
        val success = refreshAccessTokenInternal(refreshToken)
        currentToken?.accessToken.takeIf { success }
    }

    private suspend fun refreshAccessTokenInternal(refreshToken: String): Boolean {
        return runCatching {
            val tokenResponse = accountsApi.refreshToken(
                clientId = BuildConfig.SPOTIFY_CLIENT_ID,
                grantType = "refresh_token",
                refreshToken = refreshToken,
            )
            storeToken(
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken ?: refreshToken,
                expiresInSeconds = tokenResponse.expiresIn,
            )
            true
        }.getOrElse {
            _sessionState.value = SessionState.Error(it.message ?: "Failed to refresh Spotify access token.")
            false
        }
    }

    private fun storeToken(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
    ) {
        val storedToken = StoredToken(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = Instant.now().epochSecond + expiresInSeconds,
        )
        currentToken = storedToken
        tokenStore.write(storedToken)
        _sessionState.value = SessionState.Ready(
            accessToken = storedToken.accessToken,
            expiresAtEpochSeconds = storedToken.expiresAtEpochSeconds,
        )
    }

    private fun buildAuthorizationUri(
        clientId: String,
        redirectUri: String,
        codeChallenge: String,
    ): Uri {
        return Uri.parse("https://accounts.spotify.com/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter(
                "scope",
                listOf(
                    "user-read-playback-state",
                    "user-read-currently-playing",
                    "user-modify-playback-state",
                    "user-library-read",
                    "user-library-modify",
                    "playlist-read-private",
                    "playlist-read-collaborative",
                    "user-read-recently-played",
                    "user-top-read",
                ).joinToString(" "),
            )
            .build()
    }
}

