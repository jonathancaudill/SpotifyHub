package com.spotifyhub.playback

import com.spotifyhub.auth.SessionState
import com.spotifyhub.auth.SpotifyAuthRepository
import com.spotifyhub.playback.model.PlaybackItem
import com.spotifyhub.playback.model.PlaybackSnapshot
import com.spotifyhub.playback.model.RepeatMode
import com.spotifyhub.spotify.api.SpotifyLibraryApi
import com.spotifyhub.spotify.api.SpotifyPlayerApi
import com.spotifyhub.spotify.mapper.PlaybackMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    private var pollingJob: Job? = null
    private var lastKnownSavedItemUri: String? = null

    /** Epoch ms until which polling should not overwrite local optimistic state. */
    @Volatile
    private var optimisticUntil: Long = 0L

    /** Serializes API commands so they don't interleave, without blocking the UI. */
    private val commandMutex = Mutex()

    // ── Adjacent track cache (refreshed every poll) ──────────────────────
    /** The next track in the queue, pre-fetched for instant skip-forward. */
    @Volatile
    private var cachedNextItem: PlaybackItem? = null

    /** The track that was playing before the current one, for instant skip-back. */
    @Volatile
    private var cachedPreviousItem: PlaybackItem? = null

    /** Guards skip transitions so stale polls cannot snap back to the track we just left. */
    @Volatile
    private var pendingTransportTransition: PendingTransportTransition? = null

    /** Prevents ad-hoc refreshes from piling on top of the polling loop. */
    private val fetchMutex = Mutex()

    companion object {
        /** How long (ms) optimistic local state is protected from being overwritten by polling. */
        private const val OPTIMISTIC_GRACE_MS = 5_000L
        private const val PLAY_PAUSE_OPTIMISTIC_GRACE_MS = 1_500L
        private const val POLL_INTERVAL_MS = 1_000L

        private val DEFAULT_RECONCILE_DELAYS_MS = longArrayOf(0L, 500L, 1_500L)
        private val TOGGLE_PLAYBACK_RECONCILE_DELAYS_MS = longArrayOf(0L, 350L, 1_000L, 2_500L)
        private val TRANSPORT_RECONCILE_DELAYS_MS = longArrayOf(0L, 400L, 1_200L, 2_500L)
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
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _playbackState.value = null
        _currentItemSaved.value = null
        optimisticUntil = 0L
        lastKnownSavedItemUri = null
        cachedNextItem = null
        cachedPreviousItem = null
        pendingTransportTransition = null
    }

    private suspend fun fetchPlayback() {
        fetchMutex.withLock {
            coroutineScope {
                val inGracePeriod = System.currentTimeMillis() < optimisticUntil

                // Fetch playback state and queue in parallel within this refresh call so
                // cancellation stops both requests instead of leaving detached app-scope work behind.
                val playbackDeferred = async { runCatching { playerApi.getCurrentPlayback() } }
                val queueDeferred = async { runCatching { playerApi.getQueue() } }

                playbackDeferred.await()
                    .onSuccess { dto ->
                        val playback = PlaybackMapper.map(dto)
                        val songChanged = playback?.item?.id != _playbackState.value?.item?.id
                        if (!shouldIgnorePlaybackUpdate(playback, inGracePeriod)) {
                            // Rotate the outgoing track into previousItem when the song changes.
                            if (songChanged) {
                                cachedPreviousItem = _playbackState.value?.item
                            }
                            _playbackState.value = playback
                            val transition = pendingTransportTransition
                            if (playback?.item?.id == transition?.targetItemId) {
                                pendingTransportTransition = null
                                optimisticUntil = 0L
                            } else if (!inGracePeriod) {
                                pendingTransportTransition = null
                            }
                        }
                        syncCurrentItemSavedState(playback?.item?.uri)
                    }
                    .onFailure {
                        if (!inGracePeriod) {
                            _playbackState.value = null
                            _currentItemSaved.value = null
                            lastKnownSavedItemUri = null
                        }
                    }

                // Update the adjacent-track cache from the queue response.
                queueDeferred.await()
                    .onSuccess { queueDto ->
                        val queuedNextItem = queueDto?.queue?.firstOrNull()?.let { PlaybackMapper.mapItem(it) }
                        if (!shouldIgnoreQueuedNextUpdate(queuedNextItem, inGracePeriod)) {
                            cachedNextItem = queuedNextItem
                        }
                    }
                    .onFailure {
                        // Queue fetch failed — stale cache is better than no cache; leave it as-is.
                    }
                }
            }
    }

    // ── Optimistic update helpers ────────────────────────────────────────

    /** Apply an optimistic mutation to the current playback state and start the grace period. */
    private fun applyOptimistic(
        graceMs: Long = OPTIMISTIC_GRACE_MS,
        mutate: PlaybackSnapshot.() -> PlaybackSnapshot,
    ) {
        val current = _playbackState.value ?: return
        _playbackState.value = current.mutate()
        optimisticUntil = System.currentTimeMillis() + graceMs
    }

    private fun shouldIgnorePlaybackUpdate(
        playback: PlaybackSnapshot?,
        inGracePeriod: Boolean,
    ): Boolean {
        if (!inGracePeriod) {
            return false
        }

        val transition = pendingTransportTransition ?: return playback?.item?.id == _playbackState.value?.item?.id
        val playbackItemId = playback?.item?.id
        return playbackItemId == transition.sourceItemId
    }

    private fun shouldIgnoreQueuedNextUpdate(
        queuedNextItem: PlaybackItem?,
        inGracePeriod: Boolean,
    ): Boolean {
        if (!inGracePeriod) {
            return false
        }

        return queuedNextItem?.id == _playbackState.value?.item?.id
    }

    /** Fire the API command in the background, serialized by the mutex. */
    private fun fireCommand(
        reconcileDelaysMs: LongArray = DEFAULT_RECONCILE_DELAYS_MS,
        command: suspend () -> Unit,
    ) {
        appScope.launch {
            val result = commandMutex.withLock {
                runCatching { command() }
            }
            result.onSuccess {
                schedulePlaybackRefreshes(reconcileDelaysMs)
            }.onFailure {
                optimisticUntil = 0L
                pendingTransportTransition = null
                schedulePlaybackRefreshes(longArrayOf(0L, 750L))
            }
        }
    }

    private fun schedulePlaybackRefreshes(delaysMs: LongArray) {
        delaysMs.toSet()
            .sorted()
            .forEach { delayMs ->
                appScope.launch {
                    if (delayMs > 0L) {
                        delay(delayMs)
                    }
                    fetchPlayback()
                }
            }
    }

    // ── Playback commands ────────────────────────────────────────────────

    fun togglePlayback() {
        val playback = _playbackState.value ?: return
        val wasPlaying = playback.isPlaying
        applyOptimistic(graceMs = PLAY_PAUSE_OPTIMISTIC_GRACE_MS) {
            copy(
                isPlaying = !wasPlaying,
                fetchedAtEpochMs = System.currentTimeMillis(),
                progressMs = if (isPlaying) {
                    (progressMs + (System.currentTimeMillis() - fetchedAtEpochMs).coerceAtLeast(0L))
                        .coerceIn(0L, durationMs)
                } else {
                    progressMs
                },
            )
        }
        fireCommand(reconcileDelaysMs = TOGGLE_PLAYBACK_RECONCILE_DELAYS_MS) {
            if (wasPlaying) playerApi.pause() else playerApi.play()
        }
    }

    fun skipNext() {
        val next = cachedNextItem
        val currentItem = _playbackState.value?.item
        applyOptimistic {
            copy(
                progressMs = 0L,
                fetchedAtEpochMs = System.currentTimeMillis(),
                item = next ?: item,
                durationMs = next?.durationMs ?: durationMs,
            )
        }
        if (next != null) {
            cachedPreviousItem = currentItem
            cachedNextItem = null // consumed — will be refreshed on next poll
        }
        pendingTransportTransition = PendingTransportTransition(
            sourceItemId = currentItem?.id,
            targetItemId = next?.id,
        )
        fireCommand(reconcileDelaysMs = TRANSPORT_RECONCILE_DELAYS_MS) { playerApi.skipNext() }
    }

    fun skipPrevious() {
        val prev = cachedPreviousItem
        val currentItem = _playbackState.value?.item
        applyOptimistic {
            copy(
                progressMs = 0L,
                fetchedAtEpochMs = System.currentTimeMillis(),
                item = prev ?: item,
                durationMs = prev?.durationMs ?: durationMs,
            )
        }
        if (prev != null) {
            cachedPreviousItem = null // consumed — will be refreshed on next poll
        }
        pendingTransportTransition = PendingTransportTransition(
            sourceItemId = currentItem?.id,
            targetItemId = prev?.id,
        )
        fireCommand(reconcileDelaysMs = TRANSPORT_RECONCILE_DELAYS_MS) { playerApi.skipPrevious() }
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

    private data class PendingTransportTransition(
        val sourceItemId: String?,
        val targetItemId: String?,
    )
}
