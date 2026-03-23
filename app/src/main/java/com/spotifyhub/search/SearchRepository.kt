package com.spotifyhub.search

import com.spotifyhub.browse.model.BrowseItem
import com.spotifyhub.browse.model.BrowseItemType
import com.spotifyhub.search.model.SearchResults
import com.spotifyhub.search.model.SearchSection
import com.spotifyhub.spotify.api.SpotifySearchApi
import com.spotifyhub.spotify.mapper.SearchMapper
import java.text.Normalizer

class SearchRepository(
    private val searchApi: SpotifySearchApi,
) {
    companion object {
        private const val AllTypes = "track,album,artist,playlist"
        private const val TrackType = "track"
        private const val AlbumTypes = "album,track"
        private const val ArtistTypes = "artist,album,track"
        private const val SearchLimit = 10
        private const val FallbackLimit = 8
        private const val ResultSectionLimit = 10

        private val SupportedFilters = setOf("artist", "album", "track", "year", "genre", "isrc", "upc")
        private val StopWords = setOf("a", "an", "the", "by", "from", "feat", "ft", "featuring", "song", "track", "album")
        private val FieldTokenPattern = Regex("(?i)^(artist|album|track|year|genre|isrc|upc):(.*)$")
        private val TokenPattern = Regex("\"[^\"]+\"|\\S+")
        private val ByPattern = Regex("(?i)^(.+?)\\s+by\\s+(.+)$")
        private val FromPattern = Regex("(?i)^(.+?)\\s+from\\s+(.+)$")
        private val SeparatorPattern = Regex("\\s+[\\-–—]\\s+")
        private val FeaturingPattern = Regex("(?i)\\b(feat|featuring|ft)\\.?\\s+.+$")
    }

    suspend fun search(query: String): SearchResults {
        val normalizedQuery = normalizeQuery(query)
        if (normalizedQuery.isBlank()) {
            return emptyResults()
        }

        val intent = parseSearchIntent(normalizedQuery)
        val primaryResults = performSearchRequests(buildPrimaryRequests(intent))
        val results = if (primaryResults.isEmpty) {
            mergeResults(primaryResults, resolveFallbackResults(intent))
        } else {
            primaryResults
        }
        return rerankResults(results, intent)
    }

    private suspend fun performSearch(
        query: String,
        types: String,
        limit: Int,
    ): SearchResults {
        val response = searchApi.search(
            query = query,
            type = types,
            limit = limit,
            market = "from_token",
            includeExternal = "audio",
        )
        return SearchMapper.map(response)
    }

    private suspend fun performSearchRequests(requests: List<SearchRequest>): SearchResults {
        var merged = emptyResults()
        for (request in requests.distinct()) {
            val requestResults = performSearch(
                query = request.query,
                types = request.types,
                limit = request.limit,
            )
            merged = mergeResults(merged, requestResults)
        }
        return merged
    }

    private suspend fun resolveFallbackResults(intent: SearchIntent): SearchResults {
        var merged = emptyResults()
        for (fallback in buildFallbackRequests(intent)) {
            val fallbackResults = performSearch(
                query = fallback.query,
                types = fallback.types,
                limit = fallback.limit,
            )
            merged = mergeResults(merged, fallbackResults)
            if (!merged.isEmpty) {
                break
            }
        }
        return merged
    }

    private fun buildPrimaryRequests(intent: SearchIntent): List<SearchRequest> {
        return buildList {
            add(
                SearchRequest(
                    query = intent.normalizedQuery,
                    types = AllTypes,
                    limit = SearchLimit,
                ),
            )

            when {
                intent.track != null -> {
                    add(
                        SearchRequest(
                            query = buildFieldFilterQuery(
                                residualQuery = intent.residualQuery,
                                track = intent.track,
                                artist = intent.artist,
                                album = intent.album,
                                year = intent.year,
                                genre = intent.genre,
                                isrc = intent.isrc,
                            ),
                            types = TrackType,
                            limit = SearchLimit,
                        ),
                    )
                }

                intent.album != null -> {
                    add(
                        SearchRequest(
                            query = buildFieldFilterQuery(
                                residualQuery = intent.residualQuery,
                                album = intent.album,
                                artist = intent.artist,
                                year = intent.year,
                                upc = intent.upc,
                            ),
                            types = AlbumTypes,
                            limit = SearchLimit,
                        ),
                    )
                }

                intent.artist != null -> {
                    add(
                        SearchRequest(
                            query = buildFieldFilterQuery(
                                residualQuery = intent.residualQuery,
                                artist = intent.artist,
                                genre = intent.genre,
                                year = intent.year,
                            ),
                            types = ArtistTypes,
                            limit = SearchLimit,
                        ),
                    )
                }

                intent.genre != null || intent.year != null -> {
                    add(
                        SearchRequest(
                            query = buildFieldFilterQuery(
                                residualQuery = intent.residualQuery,
                                genre = intent.genre,
                                year = intent.year,
                            ),
                            types = ArtistTypes,
                            limit = SearchLimit,
                        ),
                    )
                }
            }
        }.filter { it.query.isNotBlank() }
            .distinct()
    }

    private fun buildFallbackRequests(intent: SearchIntent): List<SearchRequest> {
        val strippedQuery = simplifySearchTerms(intent.normalizedQuery)
        val strippedCoreQuery = simplifySearchTerms(intent.coreQuery)
        val deFeaturedQuery = normalizeQuery(strippedCoreQuery.replace(FeaturingPattern, ""))
        val coreTokens = tokenizeForMatching(strippedCoreQuery)

        return buildList {
            if (strippedQuery.isNotBlank() && strippedQuery != intent.normalizedQuery) {
                add(SearchRequest(query = strippedQuery, types = AllTypes, limit = FallbackLimit))
            }

            if (intent.track != null) {
                add(
                    SearchRequest(
                        query = buildFieldFilterQuery(
                            residualQuery = intent.residualQuery.takeIf { it.isNotBlank() }?.let(::simplifySearchTerms),
                            track = simplifySearchTerms(intent.track),
                            artist = intent.artist?.let(::simplifySearchTerms),
                            album = intent.album?.let(::simplifySearchTerms),
                            year = intent.year,
                            genre = intent.genre?.let(::simplifySearchTerms),
                            isrc = intent.isrc,
                        ),
                        types = TrackType,
                        limit = FallbackLimit,
                    ),
                )
            } else if (intent.album != null) {
                add(
                    SearchRequest(
                        query = buildFieldFilterQuery(
                            residualQuery = intent.residualQuery.takeIf { it.isNotBlank() }?.let(::simplifySearchTerms),
                            album = simplifySearchTerms(intent.album),
                            artist = intent.artist?.let(::simplifySearchTerms),
                            year = intent.year,
                            upc = intent.upc,
                        ),
                        types = AlbumTypes,
                        limit = FallbackLimit,
                    ),
                )
            }

            if (deFeaturedQuery.isNotBlank() && deFeaturedQuery != strippedCoreQuery && deFeaturedQuery != intent.normalizedQuery) {
                add(SearchRequest(query = deFeaturedQuery, types = AllTypes, limit = FallbackLimit))
            }

            if (strippedCoreQuery.isNotBlank() && strippedCoreQuery != intent.normalizedQuery && strippedCoreQuery != strippedQuery) {
                add(SearchRequest(query = strippedCoreQuery, types = AllTypes, limit = FallbackLimit))
            }

            if (coreTokens.size > 1) {
                add(
                    SearchRequest(
                        query = coreTokens.take(4).joinToString(" "),
                        types = AllTypes,
                        limit = FallbackLimit,
                    ),
                )
            }
        }.filter { it.query.isNotBlank() && it.query != intent.normalizedQuery }
            .distinct()
    }

    private fun mergeResults(
        left: SearchResults,
        right: SearchResults,
    ): SearchResults {
        fun mergeItems(a: List<BrowseItem>, b: List<BrowseItem>): List<BrowseItem> {
            return (a + b).distinctBy { it.id }
        }
        return SearchResults(
            tracks = mergeItems(left.tracks, right.tracks),
            albums = mergeItems(left.albums, right.albums),
            artists = mergeItems(left.artists, right.artists),
            playlists = mergeItems(left.playlists, right.playlists),
        )
    }

    private fun rerankResults(
        results: SearchResults,
        intent: SearchIntent,
    ): SearchResults {
        val normalizedQuery = normalizeForMatching(intent.coreQuery.ifBlank { intent.normalizedQuery })
        val queryTokens = tokenizeForMatching(intent.coreQuery.ifBlank { intent.normalizedQuery })

        fun rank(items: List<BrowseItem>): List<BrowseItem> {
            return items
                .sortedWith(
                    compareByDescending<BrowseItem> {
                        relevanceScore(
                            item = it,
                            intent = intent,
                            normalizedQuery = normalizedQuery,
                            queryTokens = queryTokens,
                        )
                    }.thenBy { it.title.length }
                        .thenBy { it.title.lowercase() },
                )
                .take(ResultSectionLimit)
        }

        val rankedResults = SearchResults(
            tracks = rank(results.tracks),
            albums = rank(results.albums),
            artists = rank(results.artists),
            playlists = rank(results.playlists),
        )

        return rankedResults.copy(
            sectionOrder = buildSectionOrder(
                results = rankedResults,
                intent = intent,
                normalizedQuery = normalizedQuery,
                queryTokens = queryTokens,
            ),
        )
    }

    private fun relevanceScore(
        item: BrowseItem,
        intent: SearchIntent,
        normalizedQuery: String,
        queryTokens: List<String>,
    ): Int {
        val title = normalizeForMatching(item.title)
        val subtitle = normalizeForMatching(item.subtitle)
        val combined = listOf(title, subtitle)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        var score = 0
        score += phraseScore(title, normalizedQuery, exact = 320, prefix = 220, wordBoundary = 160, contains = 90)
        score += phraseScore(subtitle, normalizedQuery, exact = 120, prefix = 80, wordBoundary = 48, contains = 24)
        score += phraseScore(combined, normalizedQuery, exact = 420, prefix = 260, wordBoundary = 180, contains = 110)

        val matchedTokens = queryTokens.count { token -> combined.contains(token) }
        score += matchedTokens * 18
        if (matchedTokens == queryTokens.size && queryTokens.isNotEmpty()) {
            score += 90
        } else {
            score -= (queryTokens.size - matchedTokens) * 10
        }

        for (token in queryTokens) {
            if (token.isBlank()) continue
            score += phraseScore(title, token, exact = 64, prefix = 34, wordBoundary = 20, contains = 12)
            score += phraseScore(subtitle, token, exact = 26, prefix = 16, wordBoundary = 10, contains = 6)
        }

        intent.track?.let {
            score += phraseScore(title, normalizeForMatching(it), exact = 360, prefix = 220, wordBoundary = 150, contains = 100)
        }
        intent.artist?.let {
            score += phraseScore(subtitle, normalizeForMatching(it), exact = 220, prefix = 140, wordBoundary = 90, contains = 52)
        }
        intent.album?.let {
            val normalizedAlbum = normalizeForMatching(it)
            score += when (item.type) {
                BrowseItemType.Album -> phraseScore(title, normalizedAlbum, exact = 260, prefix = 180, wordBoundary = 120, contains = 78)
                BrowseItemType.Track -> phraseScore(subtitle, normalizedAlbum, exact = 140, prefix = 100, wordBoundary = 70, contains = 42)
                else -> phraseScore(combined, normalizedAlbum, exact = 120, prefix = 80, wordBoundary = 48, contains = 30)
            }
        }
        intent.genre?.let {
            score += phraseScore(subtitle, normalizeForMatching(it), exact = 120, prefix = 80, wordBoundary = 50, contains = 28)
        }

        return score
    }

    private fun normalizeQuery(query: String): String {
        return query.replace(Regex("\\s+"), " ").trim()
    }

    private fun normalizeForMatching(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun simplifySearchTerms(query: String): String {
        return normalizeQuery(
            Normalizer.normalize(query, Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .replace(Regex("[^\\p{L}\\p{N}\\s]"), " "),
        )
    }

    private fun tokenizeForMatching(text: String): List<String> {
        return normalizeForMatching(text)
            .split(Regex("\\s+"))
            .filter { token ->
                token.length >= 2 && token !in StopWords
            }
    }

    private fun parseSearchIntent(query: String): SearchIntent {
        val parsedFilters = extractFieldFilters(query)

        var track = parsedFilters.filters["track"]
        var artist = parsedFilters.filters["artist"]
        var album = parsedFilters.filters["album"]
        var year = parsedFilters.filters["year"]
        var genre = parsedFilters.filters["genre"]
        var isrc = parsedFilters.filters["isrc"]
        var upc = parsedFilters.filters["upc"]
        var residualQuery = parsedFilters.residualQuery

        if (parsedFilters.filters.isEmpty()) {
            val byMatch = ByPattern.matchEntire(residualQuery)
            val fromMatch = FromPattern.matchEntire(residualQuery)
            val separatedParts = SeparatorPattern.split(residualQuery).map(::normalizeQuery).filter { it.isNotBlank() }

            when {
                byMatch != null -> {
                    track = normalizeQuery(byMatch.groupValues[1])
                    artist = normalizeQuery(byMatch.groupValues[2])
                    residualQuery = ""
                }

                fromMatch != null -> {
                    track = normalizeQuery(fromMatch.groupValues[1])
                    album = normalizeQuery(fromMatch.groupValues[2])
                    residualQuery = ""
                }

                separatedParts.size == 2 -> {
                    artist = separatedParts[0]
                    track = separatedParts[1]
                    residualQuery = ""
                }
            }
        }

        val coreQuery = normalizeQuery(
            listOf(
                residualQuery,
                track,
                artist,
                album,
                genre,
                year,
                isrc,
                upc,
            ).filterNotNull()
                .filter { it.isNotBlank() }
                .joinToString(" "),
        ).ifBlank { query }

        return SearchIntent(
            normalizedQuery = query,
            residualQuery = residualQuery,
            coreQuery = coreQuery,
            track = track,
            artist = artist,
            album = album,
            year = year,
            genre = genre,
            isrc = isrc,
            upc = upc,
            hasExplicitFilters = parsedFilters.filters.isNotEmpty(),
        )
    }

    private fun extractFieldFilters(query: String): ParsedFieldFilters {
        val tokens = TokenPattern.findAll(query)
            .map { match -> match.value.trim().trim('"') }
            .toList()

        val filters = linkedMapOf<String, String>()
        val residualTokens = mutableListOf<String>()
        var index = 0

        while (index < tokens.size) {
            val token = tokens[index]
            val fieldMatch = FieldTokenPattern.matchEntire(token)

            if (fieldMatch == null || fieldMatch.groupValues[1].lowercase() !in SupportedFilters) {
                residualTokens += token
                index += 1
                continue
            }

            val field = fieldMatch.groupValues[1].lowercase()
            val collectedValue = mutableListOf<String>()
            val inlineValue = normalizeQuery(fieldMatch.groupValues[2])
            if (inlineValue.isNotBlank()) {
                collectedValue += inlineValue
            }

            index += 1
            while (index < tokens.size && FieldTokenPattern.matchEntire(tokens[index]) == null) {
                collectedValue += tokens[index]
                index += 1
            }

            val value = normalizeQuery(collectedValue.joinToString(" "))
            if (value.isNotBlank()) {
                filters[field] = value
            }
        }

        return ParsedFieldFilters(
            filters = filters,
            residualQuery = normalizeQuery(residualTokens.joinToString(" ")),
        )
    }

    private fun buildFieldFilterQuery(
        residualQuery: String? = null,
        track: String? = null,
        artist: String? = null,
        album: String? = null,
        year: String? = null,
        genre: String? = null,
        isrc: String? = null,
        upc: String? = null,
    ): String {
        return listOfNotNull(
            residualQuery?.takeIf { it.isNotBlank() },
            track?.takeIf { it.isNotBlank() }?.let { "track:$it" },
            album?.takeIf { it.isNotBlank() }?.let { "album:$it" },
            artist?.takeIf { it.isNotBlank() }?.let { "artist:$it" },
            genre?.takeIf { it.isNotBlank() }?.let { "genre:$it" },
            year?.takeIf { it.isNotBlank() }?.let { "year:$it" },
            isrc?.takeIf { it.isNotBlank() }?.let { "isrc:$it" },
            upc?.takeIf { it.isNotBlank() }?.let { "upc:$it" },
        ).joinToString(" ")
    }

    private fun phraseScore(
        text: String,
        phrase: String,
        exact: Int,
        prefix: Int,
        wordBoundary: Int,
        contains: Int,
    ): Int {
        if (text.isBlank() || phrase.isBlank()) return 0
        return when {
            text == phrase -> exact
            text.startsWith("$phrase ") -> prefix
            containsWholePhrase(text, phrase) -> wordBoundary
            text.contains(phrase) -> contains
            else -> 0
        }
    }

    private fun containsWholePhrase(text: String, phrase: String): Boolean {
        if (text.isBlank() || phrase.isBlank()) return false
        val pattern = Regex("(^|\\b)${Regex.escape(phrase)}(\\b|$)")
        return pattern.containsMatchIn(text)
    }

    private fun buildSectionOrder(
        results: SearchResults,
        intent: SearchIntent,
        normalizedQuery: String,
        queryTokens: List<String>,
    ): List<SearchSection> {
        return SearchSection.entries
            .mapNotNull { section ->
                val items = itemsForSection(results, section)
                if (items.isEmpty()) {
                    null
                } else {
                    section to sectionPriorityScore(
                        section = section,
                        item = items.first(),
                        intent = intent,
                        normalizedQuery = normalizedQuery,
                        queryTokens = queryTokens,
                    )
                }
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun itemsForSection(
        results: SearchResults,
        section: SearchSection,
    ): List<BrowseItem> {
        return when (section) {
            SearchSection.Tracks -> results.tracks
            SearchSection.Artists -> results.artists
            SearchSection.Albums -> results.albums
            SearchSection.Playlists -> results.playlists
        }
    }

    private fun sectionPriorityScore(
        section: SearchSection,
        item: BrowseItem,
        intent: SearchIntent,
        normalizedQuery: String,
        queryTokens: List<String>,
    ): Int {
        val title = normalizeForMatching(item.title)
        val subtitle = normalizeForMatching(item.subtitle)
        val baseScore = relevanceScore(
            item = item,
            intent = intent,
            normalizedQuery = normalizedQuery,
            queryTokens = queryTokens,
        )
        val titleTokenMatches = countTokenMatches(title, queryTokens)
        val subtitleTokenMatches = countTokenMatches(subtitle, queryTokens)
        val isLooseEntityQuery = intent.track == null &&
            intent.album == null &&
            intent.artist == null &&
            queryTokens.size <= 2

        return when (section) {
            SearchSection.Artists -> {
                baseScore +
                    phraseScore(title, normalizedQuery, exact = 260, prefix = 180, wordBoundary = 120, contains = 70) +
                    (titleTokenMatches * 42) +
                    if (isLooseEntityQuery) 140 else 0
            }

            SearchSection.Albums -> {
                baseScore +
                    phraseScore(title, normalizedQuery, exact = 210, prefix = 150, wordBoundary = 100, contains = 56) +
                    (titleTokenMatches * 28)
            }

            SearchSection.Tracks -> {
                baseScore +
                    phraseScore(title, normalizedQuery, exact = 240, prefix = 170, wordBoundary = 110, contains = 64) +
                    (titleTokenMatches * 34) -
                    if (isLooseEntityQuery && titleTokenMatches == 0 && subtitleTokenMatches > 0) 170 else 0
            }

            SearchSection.Playlists -> {
                baseScore +
                    phraseScore(title, normalizedQuery, exact = 180, prefix = 120, wordBoundary = 82, contains = 44) +
                    (titleTokenMatches * 22)
            }
        }
    }

    private fun countTokenMatches(
        text: String,
        queryTokens: List<String>,
    ): Int {
        return queryTokens.count { token -> text.contains(token) }
    }

    private fun emptyResults(): SearchResults {
        return SearchResults(
            tracks = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
            playlists = emptyList(),
        )
    }

    private data class SearchRequest(
        val query: String,
        val types: String,
        val limit: Int,
    )

    private data class ParsedFieldFilters(
        val filters: Map<String, String>,
        val residualQuery: String,
    )

    private data class SearchIntent(
        val normalizedQuery: String,
        val residualQuery: String,
        val coreQuery: String,
        val track: String?,
        val artist: String?,
        val album: String?,
        val year: String?,
        val genre: String?,
        val isrc: String?,
        val upc: String?,
        val hasExplicitFilters: Boolean,
    )
}
