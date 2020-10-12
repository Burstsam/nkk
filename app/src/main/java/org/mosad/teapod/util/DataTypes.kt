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

data class Episode(val title: String = "", val streamUrl: String = "", var watched: Boolean = false)
