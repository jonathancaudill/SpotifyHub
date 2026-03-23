package com.spotifyhub.playback

import com.spotifyhub.auth.SessionState
import com.spotifyhub.auth.SpotifyAuthRepository
import com.spotifyhub.playback.model.PlaybackSnapshot
import com.spotifyhub.playback.model.RepeatMode
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    private var pollingJob: Job? = null
    private var lastKnownSavedItemUri: String? = null

    /** Epoch ms until which polling should not overwrite local optimistic state. */
    @Volatile
    private var optimisticUntil: Long = 0L

    /** Serializes API commands so they don't interleave, without blocking the UI. */
    private val commandMutex = Mutex()

    companion object {
        /** How long (ms) optimistic local state is protected from being overwritten by polling. */
        private const val OPTIMISTIC_GRACE_MS = 5_000L
    }

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
        optimisticUntil = 0L
        lastKnownSavedItemUri = null
    }

    private suspend fun fetchPlayback() {
        _isRefreshing.value = true
        val inGracePeriod = System.currentTimeMillis() < optimisticUntil
        runCatching {
            playerApi.getCurrentPlayback()
        }.onSuccess { dto ->
            val playback = PlaybackMapper.map(dto)
            if (!inGracePeriod) {
                _playbackState.value = playback
            }
            syncCurrentItemSavedState(playback?.item?.uri)
        }.onFailure {
            if (!inGracePeriod) {
                _playbackState.value = null
                _currentItemSaved.value = null
                lastKnownSavedItemUri = null
            }
        }
        _isRefreshing.value = false
    }

    // ── Optimistic update helpers ────────────────────────────────────────

    /** Apply an optimistic mutation to the current playback state and start the grace period. */
    private fun applyOptimistic(mutate: PlaybackSnapshot.() -> PlaybackSnapshot) {
        val current = _playbackState.value ?: return
        _playbackState.value = current.mutate()
        optimisticUntil = System.currentTimeMillis() + OPTIMISTIC_GRACE_MS
    }

    /** Fire the API command in the background, serialized by the mutex. */
    private fun fireCommand(command: suspend () -> Unit) {
        appScope.launch {
            commandMutex.withLock {
                runCatching { command() }
            }
        }
    }

    // ── Playback commands ────────────────────────────────────────────────

    fun togglePlayback() {
        val wasPlaying = _playbackState.value?.isPlaying == true
        applyOptimistic {
            copy(
                isPlaying = !wasPlaying,
                fetchedAtEpochMs = System.currentTimeMillis(),
                progressMs = if (isPlaying) {
                    // Was playing → pausing: freeze progress at current interpolated position
                    val elapsed = (System.currentTimeMillis() - fetchedAtEpochMs).coerceAtLeast(0L)
                    (progressMs + elapsed).coerceIn(0L, durationMs)
                } else {
                    // Was paused → playing: keep progressMs as-is, fetchedAtEpochMs resets interpolation
                    progressMs
                },
            )
        }
        fireCommand {
            if (wasPlaying) playerApi.pause() else playerApi.play()
        }
    }

    fun skipNext() {
        applyOptimistic {
            copy(progressMs = 0L, fetchedAtEpochMs = System.currentTimeMillis())
        }
        fireCommand { playerApi.skipNext() }
    }

    fun skipPrevious() {
        applyOptimistic {
            copy(progressMs = 0L, fetchedAtEpochMs = System.currentTimeMillis())
        }
        fireCommand { playerApi.skipPrevious() }
    }

    fun toggleSaveCurrentItem() {
        val itemUri = _playbackState.value?.item?.uri?.takeIf(String::isNotBlank) ?: return
        val shouldSave = _currentItemSaved.value != true
        // Optimistic update for saved state
        lastKnownSavedItemUri = itemUri
        _currentItemSaved.value = shouldSave
        fireCommand {
            if (shouldSave) {
                libraryApi.saveItems(uris = itemUri)
            } else {
                libraryApi.removeItems(uris = itemUri)
            }
        }
    }

    fun toggleShuffle() {
        val nextShuffleState = _playbackState.value?.isShuffleEnabled != true
        applyOptimistic { copy(isShuffleEnabled = nextShuffleState) }
        fireCommand { playerApi.setShuffle(enabled = nextShuffleState) }
    }

    fun cycleRepeatMode() {
        val nextRepeatMode = when (_playbackState.value?.repeatMode ?: RepeatMode.Off) {
            RepeatMode.Off -> RepeatMode.Context
            RepeatMode.Context -> RepeatMode.Track
            RepeatMode.Track -> RepeatMode.Off
        }
        applyOptimistic { copy(repeatMode = nextRepeatMode) }
        fireCommand { playerApi.setRepeatMode(repeatMode = nextRepeatMode.toApiValue()) }
    }

    fun seekTo(positionMs: Long) {
        applyOptimistic {
            copy(progressMs = positionMs.coerceIn(0L, durationMs), fetchedAtEpochMs = System.currentTimeMillis())
        }
        fireCommand { playerApi.seekTo(positionMs = positionMs) }
    }

    fun seekBy(deltaMs: Long) {
        val playback = _playbackState.value ?: return
        val durationMs = playback.durationMs
        if (durationMs <= 0L) return

        val currentPositionMs = currentPlaybackPosition(playback)
        val targetPositionMs = (currentPositionMs + deltaMs).coerceIn(0L, durationMs)
        if (targetPositionMs == currentPositionMs) return

        applyOptimistic {
            copy(progressMs = targetPositionMs, fetchedAtEpochMs = System.currentTimeMillis())
        }
        fireCommand { playerApi.seekTo(positionMs = targetPositionMs) }
    }

    fun adjustVolume(deltaPercent: Int) {
        val currentVolume = _playbackState.value?.device?.volumePercent ?: return
        val nextVolume = (currentVolume + deltaPercent).coerceIn(0, 100)
        if (nextVolume == currentVolume) return

        applyOptimistic {
            copy(device = device?.copy(volumePercent = nextVolume))
        }
        fireCommand { playerApi.setVolume(volumePercent = nextVolume) }
    }

    // ── Internal helpers ─────────────────────────────────────────────────

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

    private fun currentPlaybackPosition(playback: PlaybackSnapshot): Long {
        if (!playback.isPlaying) {
            return playback.progressMs.coerceIn(0L, playback.durationMs)
        }

        val elapsedMs = (System.currentTimeMillis() - playback.fetchedAtEpochMs).coerceAtLeast(0L)
        return (playback.progressMs + elapsedMs).coerceIn(0L, playback.durationMs)
    }

    private fun RepeatMode.toApiValue(): String {
        return when (this) {
            RepeatMode.Off -> "off"
            RepeatMode.Context -> "context"
            RepeatMode.Track -> "track"
        }
    }
}
