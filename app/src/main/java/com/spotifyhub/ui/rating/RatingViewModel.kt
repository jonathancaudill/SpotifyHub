package com.spotifyhub.ui.rating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifyhub.playback.PlaybackRepository
import com.spotifyhub.playback.model.PlaybackItem
import com.spotifyhub.rating.SheetsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SubmissionState {
    Idle,
    Submitting,
    Success,
    Error,
}

data class RatingUiState(
    val currentItem: PlaybackItem?,
    val selectedRating: Float,
    val submissionState: SubmissionState,
    val errorMessage: String?,
    /** Non-null when this album already has a rating in the sheet. */
    val existingRating: Float?,
    /** True while we're checking the sheet for an existing rating. */
    val isLookingUp: Boolean,
) {
    /** The wheel and submit button should be disabled when a rating already exists. */
    val isLocked: Boolean get() = existingRating != null
}

class RatingViewModel(
    private val playbackRepository: PlaybackRepository,
    private val sheetsRepository: SheetsRepository,
) : ViewModel() {

    private val _selectedRating = MutableStateFlow(5.0f)
    private val _submissionState = MutableStateFlow(SubmissionState.Idle)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _existingRating = MutableStateFlow<Float?>(null)
    private val _isLookingUp = MutableStateFlow(false)

    /** Tracks which album we last looked up to avoid redundant queries. */
    private var lastLookedUpAlbumKey: String? = null
    private var lookupJob: Job? = null

    init {
        // Whenever the current album changes, look up its existing rating
        viewModelScope.launch {
            playbackRepository.playbackState
                .map { it?.item }
                .distinctUntilChanged { old, new -> albumKey(old) == albumKey(new) }
                .collect { item ->
                    onAlbumChanged(item)
                }
        }
    }

    val uiState: StateFlow<RatingUiState> = combine(
        playbackRepository.playbackState,
        _selectedRating,
        _submissionState,
        _errorMessage,
        _existingRating,
        _isLookingUp,
    ) { values ->
        val playback = values[0] as? com.spotifyhub.playback.model.PlaybackSnapshot
        val rating = values[1] as Float
        val submission = values[2] as SubmissionState
        val error = values[3] as? String
        val existing = values[4] as? Float
        val lookingUp = values[5] as Boolean

        RatingUiState(
            currentItem = playback?.item,
            selectedRating = rating,
            submissionState = submission,
            errorMessage = error,
            existingRating = existing,
            isLookingUp = lookingUp,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RatingUiState(
            currentItem = null,
            selectedRating = 5.0f,
            submissionState = SubmissionState.Idle,
            errorMessage = null,
            existingRating = null,
            isLookingUp = false,
        ),
    )

    fun setRating(value: Float) {
        if (_existingRating.value != null) return // locked
        _selectedRating.value = value.coerceIn(0f, 10f)
    }

    fun submitRating() {
        val item = uiState.value.currentItem ?: return
        if (_submissionState.value == SubmissionState.Submitting) return
        if (_existingRating.value != null) return // already rated

        viewModelScope.launch {
            _submissionState.value = SubmissionState.Submitting
            _errorMessage.value = null

            val result = sheetsRepository.submitRating(
                albumCover = item.artworkUrl,
                artistName = item.artist,
                title = item.album,
                releaseDate = item.releaseDate,
                rating = _selectedRating.value,
            )

            result.fold(
                onSuccess = {
                    _submissionState.value = SubmissionState.Success
                    // Lock the rating now that it's been submitted
                    _existingRating.value = _selectedRating.value
                    delay(2_000)
                    _submissionState.value = SubmissionState.Idle
                },
                onFailure = { throwable ->
                    _errorMessage.value = throwable.message ?: "Failed to submit rating"
                    _submissionState.value = SubmissionState.Error
                    delay(3_000)
                    _submissionState.value = SubmissionState.Idle
                    _errorMessage.value = null
                },
            )
        }
    }

    private fun onAlbumChanged(item: PlaybackItem?) {
        val key = albumKey(item)

        if (key == lastLookedUpAlbumKey) return
        lastLookedUpAlbumKey = key

        // Reset state for new album
        _existingRating.value = null
        _selectedRating.value = 5.0f
        _submissionState.value = SubmissionState.Idle
        _errorMessage.value = null

        if (item == null || item.artist.isBlank() || item.album.isBlank()) {
            _isLookingUp.value = false
            return
        }

        lookupJob?.cancel()
        lookupJob = viewModelScope.launch {
            _isLookingUp.value = true

            val result = sheetsRepository.lookupRating(
                artistName = item.artist,
                title = item.album,
            )

            result.fold(
                onSuccess = { existingRating ->
                    _existingRating.value = existingRating
                    if (existingRating != null) {
                        _selectedRating.value = existingRating
                    }
                },
                onFailure = {
                    // Lookup failed — just let the user rate normally
                    _existingRating.value = null
                },
            )

            _isLookingUp.value = false
        }
    }

    private fun albumKey(item: PlaybackItem?): String? {
        if (item == null) return null
        return "${item.artist}::${item.album}".lowercase()
    }
}
