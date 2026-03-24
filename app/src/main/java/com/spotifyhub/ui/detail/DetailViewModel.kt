package com.spotifyhub.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifyhub.artist.ArtistRepository
import com.spotifyhub.artist.model.ArtistBio
import com.spotifyhub.artist.model.ArtistDetail
import com.spotifyhub.artist.model.ArtistPlaylistSummary
import com.spotifyhub.artist.model.ArtistReleaseSummary
import com.spotifyhub.browse.model.BrowseItem
import com.spotifyhub.library.LibraryRepository
import com.spotifyhub.library.model.TrackItem
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface DetailContent {
    data class Playlist(
        val title: String,
        val subtitle: String,
        val description: String?,
        val artworkUrl: String?,
        val uri: String,
        val tracks: List<TrackItem>,
    ) : DetailContent

    data class Album(
        val title: String,
        val subtitle: String,
        val artworkUrl: String?,
        val uri: String,
        val tracks: List<TrackItem>,
    ) : DetailContent

    data class Artist(
        val detail: ArtistDetail,
    ) : DetailContent
}

data class DetailUiState(
    val content: DetailContent? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

class DetailViewModel(
    private val libraryRepository: LibraryRepository,
    private val artistRepository: ArtistRepository,
) : ViewModel() {
    private val playlistUnavailableMessage =
        "Track list unavailable from Spotify's API for this playlist. Tap Play to start it on your active device."

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadPlaylist(playlistId: String, fallbackItem: BrowseItem? = null) {
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            runCatching {
                libraryRepository.getPlaylistDetail(playlistId)
            }.onSuccess { detail ->
                if (detail != null) {
                    _uiState.value = DetailUiState(
                        content = DetailContent.Playlist(
                            title = detail.name,
                            subtitle = detail.ownerName?.let { "by $it" } ?: "",
                            description = detail.description,
                            artworkUrl = detail.artworkUrl,
                            uri = detail.uri,
                            tracks = detail.tracks,
                        ),
                        isLoading = false,
                    )
                } else if (fallbackItem != null) {
                    _uiState.value = DetailUiState(
                        content = buildFallbackPlaylistContent(fallbackItem),
                        isLoading = false,
                    )
                } else {
                    _uiState.value = DetailUiState(isLoading = false, error = "Playlist not found")
                }
            }.onFailure { error ->
                val fallback = fallbackItem
                if (fallback != null && shouldUsePlaylistFallback(error, fallback)) {
                    _uiState.value = DetailUiState(
                        content = buildFallbackPlaylistContent(fallback),
                        isLoading = false,
                    )
                } else {
                    _uiState.value = DetailUiState(isLoading = false, error = error.message)
                }
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
            _uiState.value = DetailUiState(isLoading = true)
            runCatching {
                libraryRepository.getAlbumDetail(
                    albumId = albumId,
                    albumName = albumName,
                    artistName = artistName,
                    artworkUrl = artworkUrl,
                    albumUri = albumUri,
                )
            }.onSuccess { detail ->
                _uiState.value = DetailUiState(
                    content = DetailContent.Album(
                        title = detail.name,
                        subtitle = detail.artistName.orEmpty(),
                        artworkUrl = detail.artworkUrl,
                        uri = detail.uri,
                        tracks = detail.tracks,
                    ),
                    isLoading = false,
                )
            }.onFailure { error ->
                _uiState.value = DetailUiState(isLoading = false, error = error.message)
            }
        }
    }

    fun loadArtist(artistId: String, fallbackItem: BrowseItem? = null) {
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            val fallbackArtistName = fallbackItem?.title.orEmpty()
            val bioDeferred = fallbackArtistName.takeIf { it.isNotBlank() }?.let { name ->
                async { artistRepository.getArtistBio(name) }
            }

            runCatching {
                artistRepository.getArtistDetail(artistId, fallbackItem)
            }.onSuccess { detail ->
                _uiState.value = DetailUiState(
                    content = DetailContent.Artist(detail = detail),
                    isLoading = false,
                )

                val bio = bioDeferred?.await()
                if (bio != null) {
                    applyArtistBio(artistId, bio)
                } else if (fallbackArtistName.isBlank()) {
                    val lateBio = artistRepository.getArtistBio(detail.name)
                    if (lateBio != null) {
                        applyArtistBio(artistId, lateBio)
                    }
                }
            }.onFailure { error ->
                _uiState.value = DetailUiState(isLoading = false, error = error.message)
            }
        }
    }

    fun openArtistRelease(release: ArtistReleaseSummary) {
        loadAlbum(
            albumId = release.id,
            albumName = release.title,
            artistName = release.subtitle,
            artworkUrl = release.artworkUrl,
            albumUri = release.uri,
        )
    }

    fun openArtistPlaylist(playlist: ArtistPlaylistSummary) {
        loadPlaylist(
            playlistId = playlist.id,
            fallbackItem = BrowseItem(
                id = playlist.id,
                title = playlist.title,
                subtitle = playlist.subtitle,
                artworkUrl = playlist.artworkUrl,
                uri = playlist.uri,
                type = com.spotifyhub.browse.model.BrowseItemType.Playlist,
            ),
        )
    }

    fun playContext(trackOffset: Int? = null) {
        val uri = when (val content = _uiState.value.content) {
            is DetailContent.Album -> content.uri
            is DetailContent.Playlist -> content.uri
            is DetailContent.Artist -> content.detail.uri
            null -> ""
        }
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

    private fun applyArtistBio(artistId: String, bio: ArtistBio) {
        val currentContent = _uiState.value.content as? DetailContent.Artist ?: return
        if (currentContent.detail.id != artistId) {
            return
        }
        _uiState.value = _uiState.value.copy(
            content = currentContent.copy(
                detail = currentContent.detail.copy(bio = bio),
            ),
        )
    }

    private fun shouldUsePlaylistFallback(error: Throwable, fallbackItem: BrowseItem): Boolean {
        if (fallbackItem.uri.isBlank()) {
            return false
        }
        val httpError = error as? HttpException ?: return false
        return httpError.code() == 403 || httpError.code() == 404
    }

    private fun buildFallbackPlaylistContent(item: BrowseItem): DetailContent.Playlist {
        return DetailContent.Playlist(
            title = item.title,
            subtitle = item.subtitle,
            description = playlistUnavailableMessage,
            artworkUrl = item.artworkUrl,
            uri = item.uri,
            tracks = emptyList(),
        )
    }
}
