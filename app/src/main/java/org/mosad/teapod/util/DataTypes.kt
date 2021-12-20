package org.mosad.teapod.util

import java.util.Locale

class DataTypes {
    enum class MediaType(val str: String) {
        OTHER("other"),
        MOVIE("movie"), // TODO
        TVSHOW("series")
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
    val id: Int, // aod path id
    val title: String,
    val posterUrl: String,
    val idStr: String = "" // crunchyroll id
)

// TODO replace playlist: List<AoDEpisode> with a map?
data class AoDMedia(
    val aodId: Int,
    val type: DataTypes.MediaType,
    val title: String,
    val shortText: String,
    val posterURL: String,
    var year: Int,
    var age: Int,
    val similar: List<ItemMedia>,
    val playlist: List<AoDEpisode>,
) {
    fun getEpisodeById(mediaId: Int) = playlist.firstOrNull { it.mediaId == mediaId }
        ?: AoDEpisodeNone
}

data class AoDEpisode(
    val mediaId: Int,
    val title: String,
    val description: String,
    val shortDesc: String,
    val imageURL: String,
    val numberStr: String,
    val index: Int,
    var watched: Boolean,
    val watchedCallback: String,
    val streams: MutableList<Stream>,
){
    fun hasDub() = streams.any { it.language == Locale.GERMAN }

    /**
     * get the preferred stream
     * @return the preferred stream, if not present use the first stream
     */
    fun getPreferredStream(language: Locale) = streams.firstOrNull { it.language == language }
        ?: streams.first()
}

data class Stream(
    val url: String,
    val language : Locale
)

// TODO will be watched info (state and callback) -> remove description and number
data class AoDEpisodeInfo(
    val aodMediaId: Int,
    val shortDesc: String,
    var watched: Boolean,
    val watchedCallback: String,
)

val AoDMediaNone = AoDMedia(
    -1,
    DataTypes.MediaType.OTHER,
    "",
    "",
    "",
    -1,
    -1,
    listOf(),
    listOf()
)

val AoDEpisodeNone = AoDEpisode(
    -1,
    "",
    "",
    "",
    "",
    "",
    -1,
    false,
    "",
    mutableListOf()
)

/**
 * this class is used to represent the aod json API?
 */
data class AoDPlaylist(
    val list: List<Playlist>,
    val language: Locale
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
