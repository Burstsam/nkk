package org.mosad.teapod.parser.crunchyroll

import kotlinx.serialization.Serializable

/**
 * data classes for browse
 * TODO make class names more clear/possibly overlapping for now
 */
enum class SortBy(val str: String) {
    ALPHABETICAL("alphabetical"),
    NEWLY_ADDED("newly_added"),
    POPULARITY("popularity")
}

@Serializable
data class BrowseResult(val total: Int, val items: List<Item>)

@Serializable
data class Item(
    val id: String,
    val title: String,
    val type: String,
    val channel_id: String,
    val description: String,
    val images: Images
    // TODO metadata etc.
)

@Serializable
data class Images(val poster_tall: List<List<Poster>>, val poster_wide: List<List<Poster>>)
// crunchyroll why?

@Serializable
data class Poster(val height: Int, val width: Int, val source: String, val type: String)