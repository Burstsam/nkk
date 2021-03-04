package org.mosad.teapod.util

import java.util.*
import kotlin.collections.ArrayList

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
    val episodes: ArrayList<Episode> = arrayListOf()
) {
    fun hasEpisode(id: Int) = episodes.any { it.id == id }
    fun getEpisodeById(id: Int) = episodes.first { it.id == id }
}

// TODO all val?
data class Info(
    var title: String = "",
    var posterUrl: String = "",
    var shortDesc: String = "",
    var description: String = "",
    var year: Int = 0,
    var age: Int = 0,
    var episodesCount: Int = 0,
    var similar: List<ItemMedia> = listOf()
)

/**
 * number = episode number (0..n)
 */
data class Episode(
    val id: Int = -1,
    val streams: MutableList<Stream> = mutableListOf(),
    val title: String = "",
    val posterUrl: String = "",
    val description: String = "",
    var shortDesc: String = "",
    val number: Int = 0,
    var watched: Boolean = false,
    var watchedCallback: String = ""
) {
    /**
     * get the preferred stream
     * @return the preferred stream, if not present use the first stream
     */
    fun getPreferredStream(language: Locale) =
        streams.firstOrNull { it.language == language } ?: streams.first()

    fun hasDub() = streams.any { it.language == Locale.GERMAN }
}

data class Stream(
    val url: String,
    val language : Locale
)

/**
 * this class is used for tmdb responses
 * TODO why is runtime var?
 */
data class TMDBResponse(
    val id: Int = 0,
    val title: String = "",
    val overview: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    var runtime: Int = 0
)

/**
 * this class is used to represent the aod json API?
 */
data class AoDObject(
    val playlist: List<Playlist>,
    val extLanguage: String
)

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
