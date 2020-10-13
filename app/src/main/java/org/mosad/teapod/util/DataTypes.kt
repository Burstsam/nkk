package org.mosad.teapod.util

class DataTypes {
    enum class MediaType {
        OTHER,
        MOVIE,
        TVSHOW
    }
}

data class Media(val title: String, val link: String, val type: DataTypes.MediaType, val posterLink: String, val shortDesc : String, var episodes: List<Episode> = listOf(), val info : Info = Info()) {
    override fun toString(): String {
        return title
    }
}

data class Info(var description: String = "", var year: Int = 0, var age: Int = 0, var episodesCount: Int = 0)

data class Episode(val title: String = "", val streamUrl: String = "", val posterLink: String = "", var description: String = "", var number: Int = 0, var watched: Boolean = false)

data class TMDBResponse(val id: Int = 0, val title: String = "", val overview: String = "", val posterUrl: String = "", val backdropUrl: String = "", var runtime: Int = 0)
