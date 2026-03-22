package com.spotifyhub.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifyhub.library.LibraryRepository
import com.spotifyhub.library.model.TrackItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val title: String = "",
    val subtitle: String = "",
    val description: String? = null,
    val artworkUrl: String? = null,
    val uri: String = "",
    val tracks: List<TrackItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class DetailViewModel(
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadPlaylist(playlistId: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            runCatching {
                libraryRepository.getPlaylistDetail(playlistId)
            }.onSuccess { detail ->
                if (detail != null) {
                    _uiState.value = DetailUiState(
                        title = detail.name,
                        subtitle = detail.ownerName?.let { "by $it" } ?: "",
                        description = detail.description,
                        artworkUrl = detail.artworkUrl,
                        uri = detail.uri,
                        tracks = detail.tracks,
                        isLoading = false,
                    )
                } else {
                    _uiState.value = DetailUiState(isLoading = false, error = "Playlist not found")
                }
            }.onFailure { e ->
                _uiState.value = DetailUiState(isLoading = false, error = e.message)
            }
        }
    }

    fun loadAlbum(
        albumId: String,
        albumName: String,
        artistName: String?,
        artworkUrl: String?,
        albumUri: String,
    ) {
        viewModelScope.launch {
            _uiState.value = DetailUiState(
                title = albumName,
                subtitle = artistName ?: "",
                artworkUrl = artworkUrl,
                uri = albumUri,
                isLoading = true,
            )
            runCatching {
                libraryRepository.getAlbumDetail(
                    albumId = albumId,
                    albumName = albumName,
                    artistName = artistName,
                    artworkUrl = artworkUrl,
                    albumUri = albumUri,
                )
            }.onSuccess { detail ->
                _uiState.value = _uiState.value.copy(
                    tracks = detail.tracks,
                    isLoading = false,
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun playContext(trackOffset: Int? = null) {
        val uri = _uiState.value.uri
        if (uri.isBlank()) return
        viewModelScope.launch {
            runCatching { libraryRepository.playContext(uri, trackOffset) }
        }
    }

    fun playTrack(trackUri: String) {
        viewModelScope.launch {
            runCatching { libraryRepository.playTrack(trackUri) }
        }
    }

    fun playFromStart() {
        playContext(trackOffset = 0)
    }
}
