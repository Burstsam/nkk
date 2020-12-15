package org.mosad.teapod.util

class DataTypes {
    enum class MediaType {
        OTHER,
        MOVIE,
        TVSHOW
    }

    enum class Theme(val str: String) {
        LIGHT("Light"),
        DARK("Dark")
    }

    enum class License(val short: String, val long: String) {
        APACHE2("AL 2.0", "Apache License Version 2.0"),
        MIT("MIT", "MIT License"),
        GPL3("GPL 3", "GNU General Public License Version 3"),
        BSD2("BSD 2", "BSD 2-Clause License")
    }
}

data class ThirdPartyComponent(
    val name: String,
    val year: String,
    val copyrightOwner: String,
    val link: String,
    val license: DataTypes.License
)

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
    var episodes: ArrayList<Episode> = arrayListOf()
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

/**
 * if secStreamOmU == true, then a secondary stream is present
 * number = episode number (0..n)
 */
data class Episode(
    val id: Int = 0,
    var title: String = "",
    var priStreamUrl: String = "",
    var secStreamUrl: String = "",
    var secStreamOmU: Boolean = false,
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

data class AoDObject(val playlist: List<Playlist>)

data class Playlist(
    val sources: List<Source>,
    val image: String,
    val title: String,
    val description: String,
    val mediaid: Int
)

data class Source(
    val file: String = ""
)
