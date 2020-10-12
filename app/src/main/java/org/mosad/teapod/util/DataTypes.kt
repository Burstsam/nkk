package org.mosad.teapod.util

class DataTypes {
    enum class MediaType {
        OTHER,
        MOVIE,
        TVSHOW
    }
}

data class Media(val title: String, val link: String, val type: DataTypes.MediaType, val posterLink: String, val shortDesc : String, var episodes: List<Episode> = listOf()) {
    override fun toString(): String {
        return title
    }
}

data class Episode(val title: String = "", val streamUrl: String = "", val posterLink: String = "", var watched: Boolean = false)

data class TMDBResponse(val title: String = "", val overview: String = "", val posterUrl: String = "", val backdropUrl: String = "")
