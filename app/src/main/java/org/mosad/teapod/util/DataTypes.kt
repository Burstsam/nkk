package org.mosad.teapod.util

class DataTypes {
    enum class MediaType {
        OTHER,
        MOVIE,
        TVSHOW
    }
}

/**
 * this class is used to represent the item media
 * it is uses in the ItemMediaAdapter (RecyclerView)
 */
data class ItemMedia(
    val id: Int,
    val title: String,
    val posterUrl: String
)


/**
 * TODO the episodes workflow could use a clean up/rework
 */
data class Media(
    val id: Int,
    val link: String,
    val type: DataTypes.MediaType,
    val info: Info = Info(),
    var episodes: List<Episode> = listOf()
)

data class Info(
    var title: String = "",
    var posterUrl: String = "",
    var shortDesc: String = "",
    var description: String = "",
    var year: Int = 0,
    var age: Int = 0,
    var episodesCount: Int = 0
)

data class Episode(
    val id: Int = 0,
    var title: String = "",
    var streamUrl: String = "",
    var posterUrl: String = "",
    var description: String = "",
    var shortDesc: String = "",
    var number: Int = 0,
    var watched: Boolean = false,
    var watchedCallback: String = ""
)

data class TMDBResponse(
    val id: Int = 0,
    val title: String = "",
    val overview: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    var runtime: Int = 0
)
