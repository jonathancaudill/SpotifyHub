package com.spotifyhub.search.model

import com.spotifyhub.browse.model.BrowseItem

enum class SearchSection {
    Tracks,
    Artists,
    Albums,
    Playlists,
}

data class SearchResults(
    val tracks: List<BrowseItem>,
    val albums: List<BrowseItem>,
    val artists: List<BrowseItem>,
    val playlists: List<BrowseItem>,
    val sectionOrder: List<SearchSection> = defaultSectionOrder(
        tracks = tracks,
        artists = artists,
        albums = albums,
        playlists = playlists,
    ),
) {
    val isEmpty: Boolean
        get() = tracks.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()
}

private fun defaultSectionOrder(
    tracks: List<BrowseItem>,
    artists: List<BrowseItem>,
    albums: List<BrowseItem>,
    playlists: List<BrowseItem>,
): List<SearchSection> {
    return buildList {
        if (tracks.isNotEmpty()) add(SearchSection.Tracks)
        if (artists.isNotEmpty()) add(SearchSection.Artists)
        if (albums.isNotEmpty()) add(SearchSection.Albums)
        if (playlists.isNotEmpty()) add(SearchSection.Playlists)
    }
}
