package com.spotifyhub.search.model

import com.spotifyhub.browse.model.BrowseItem

data class SearchResults(
    val tracks: List<BrowseItem>,
    val albums: List<BrowseItem>,
    val artists: List<BrowseItem>,
    val playlists: List<BrowseItem>,
) {
    val isEmpty: Boolean
        get() = tracks.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()
}
