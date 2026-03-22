package com.spotifyhub.browse.model

data class HomeData(
    val greeting: String,
    val displayName: String?,
    val quickAccess: List<BrowseItem>,
    val sections: List<HomeSection>,
)

data class HomeSection(
    val title: String,
    val items: List<BrowseItem>,
    val style: SectionStyle,
)

enum class SectionStyle {
    HorizontalCards,
    HorizontalCircle,
}

data class BrowseItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String?,
    val uri: String,
    val type: BrowseItemType,
    val contextUri: String? = null,
)

enum class BrowseItemType {
    Playlist,
    Album,
    Artist,
    Track,
}
