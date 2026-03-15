package com.spotifyhub.playback

import com.spotifyhub.auth.SessionState
import com.spotifyhub.auth.SpotifyAuthRepository
import com.spotifyhub.playback.model.PlaybackSnapshot
import com.spotifyhub.spotify.api.SpotifyLibraryApi
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
    private val libraryApi: SpotifyLibraryApi,
) {
    private val _playbackState = MutableStateFlow<PlaybackSnapshot?>(null)
    val playbackState: StateFlow<PlaybackSnapshot?> = _playbackState.asStateFlow()

    private val _currentItemSaved = MutableStateFlow<Boolean?>(null)
    val currentItemSaved: StateFlow<Boolean?> = _currentItemSaved.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isSendingCommand = MutableStateFlow(false)
    val isSendingCommand: StateFlow<Boolean> = _isSendingCommand.asStateFlow()

    private var pollingJob: Job? = null
    private var lastKnownSavedItemUri: String? = null

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
        _currentItemSaved.value = null
        _isRefreshing.value = false
        _isSendingCommand.value = false
        lastKnownSavedItemUri = null
    }

    private suspend fun fetchPlayback() {
        _isRefreshing.value = true
        runCatching {
            playerApi.getCurrentPlayback()
        }.onSuccess { dto ->
            val playback = PlaybackMapper.map(dto)
            _playbackState.value = playback
            syncCurrentItemSavedState(playback?.item?.uri)
        }.onFailure {
            _playbackState.value = null
            _currentItemSaved.value = null
            lastKnownSavedItemUri = null
        }
        _isRefreshing.value = false
    }

    fun togglePlayback() {
        appScope.launch {
            executeCommand(command = {
                if (_playbackState.value?.isPlaying == true) {
                    playerApi.pause()
                } else {
                    playerApi.play()
                }
            })
        }
    }

    fun skipNext() {
        appScope.launch {
            executeCommand(command = { playerApi.skipNext() })
        }
    }

    fun skipPrevious() {
        appScope.launch {
            executeCommand(command = { playerApi.skipPrevious() })
        }
    }

    fun toggleSaveCurrentItem() {
        appScope.launch {
            val itemUri = _playbackState.value?.item?.uri?.takeIf(String::isNotBlank) ?: return@launch
            val shouldSave = _currentItemSaved.value != true
            executeCommand(
                command = {
                    if (shouldSave) {
                        libraryApi.saveItems(uris = itemUri)
                    } else {
                        libraryApi.removeItems(uris = itemUri)
                    }
                },
                onSuccess = {
                    lastKnownSavedItemUri = itemUri
                    _currentItemSaved.value = shouldSave
                },
            )
        }
    }

    private suspend fun syncCurrentItemSavedState(itemUri: String?) {
        val normalizedUri = itemUri?.takeIf(String::isNotBlank)
        if (normalizedUri == null) {
            lastKnownSavedItemUri = null
            _currentItemSaved.value = null
            return
        }

        if (lastKnownSavedItemUri == normalizedUri && _currentItemSaved.value != null) {
            return
        }

        runCatching {
            libraryApi.containsSavedItems(uris = normalizedUri).firstOrNull() ?: false
        }.onSuccess { isSaved ->
            lastKnownSavedItemUri = normalizedUri
            _currentItemSaved.value = isSaved
        }.onFailure {
            if (lastKnownSavedItemUri != normalizedUri) {
                _currentItemSaved.value = null
            }
        }
    }

    private suspend fun executeCommand(
        command: suspend () -> Unit,
        onSuccess: (() -> Unit)? = null,
    ) {
        if (_isSendingCommand.value) {
            return
        }

        _isSendingCommand.value = true
        runCatching { command() }
            .onSuccess { onSuccess?.invoke() }
        delay(200L)
        fetchPlayback()
        _isSendingCommand.value = false
    }
}
