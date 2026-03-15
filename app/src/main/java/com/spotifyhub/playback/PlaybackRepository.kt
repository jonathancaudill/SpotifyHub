package com.spotifyhub.playback

import com.spotifyhub.auth.SessionState
import com.spotifyhub.auth.SpotifyAuthRepository
import com.spotifyhub.playback.model.PlaybackSnapshot
import com.spotifyhub.spotify.api.SpotifyPlayerApi
import com.spotifyhub.spotify.mapper.PlaybackMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaybackRepository(
    private val appScope: CoroutineScope,
    private val authRepository: SpotifyAuthRepository,
    private val playerApi: SpotifyPlayerApi,
) {
    private val _playbackState = MutableStateFlow<PlaybackSnapshot?>(null)
    val playbackState: StateFlow<PlaybackSnapshot?> = _playbackState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var pollingJob: Job? = null

    init {
        appScope.launch {
            authRepository.sessionState.collect { state ->
                when (state) {
                    is SessionState.Ready -> startPolling()
                    else -> stopPolling()
                }
            }
        }
    }

    fun refreshNow() {
        appScope.launch { fetchPlayback() }
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) {
            return
        }
        pollingJob = appScope.launch {
            while (true) {
                fetchPlayback()
                delay(1_500L)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _playbackState.value = null
        _isRefreshing.value = false
    }

    private suspend fun fetchPlayback() {
        _isRefreshing.value = true
        runCatching {
            playerApi.getCurrentPlayback()
        }.onSuccess { dto ->
            _playbackState.value = PlaybackMapper.map(dto)
        }.onFailure {
            _playbackState.value = null
        }
        _isRefreshing.value = false
    }
}

