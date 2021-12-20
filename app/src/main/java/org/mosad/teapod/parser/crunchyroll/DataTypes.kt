package org.mosad.teapod.parser.crunchyroll

import kotlinx.serialization.SerialName
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

val NoneItem = Item("", "", "", "", "", Images(listOf(), listOf()))
val NoneBrowseResult = BrowseResult(0, listOf())

@Serializable
data class Images(val poster_tall: List<List<Poster>>, val poster_wide: List<List<Poster>>)
// crunchyroll why?

@Serializable
data class Poster(val height: Int, val width: Int, val source: String, val type: String)

/**
 * Series return type
 */
@Serializable
data class Series(
    val id: String,
    val title: String,
    val description: String,
    val images: Images
)
val NoneSeries = Series("", "", "", Images(listOf(), listOf()))


/**
 * Seasons data type
 */
@Serializable
data class Seasons(val total: Int, val items: List<Season>)

@Serializable
data class Season(
    val id: String,
    val title: String,
    val series_id: String,
    val season_number: Int
)

val NoneSeasons = Seasons(0, listOf())

/**
 * Episodes data type
 */
@Serializable
data class Episodes(val total: Int, val items: List<Episode>)

@Serializable
data class Episode(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("series_id") val seriesId: String,
    @SerialName("season_title") val seasonTitle: String,
    @SerialName("season_id") val seasonId: String,
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("episode") val episode: String,
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("description") val description: String,
    @SerialName("next_episode_id") val nextEpisodeId: String = "", // use default value since the field is optional
    @SerialName("next_episode_title") val nextEpisodeTitle: String = "", // use default value since the field is optional
    @SerialName("is_subbed") val isSubbed: Boolean,
    @SerialName("is_dubbed") val isDubbed: Boolean,
    @SerialName("images") val images: Thumbnail,
    @SerialName("duration_ms") val durationMs: Int,
    @SerialName("playback") val playback: String,
)

@Serializable
data class Thumbnail(
    @SerialName("thumbnail") val thumbnail: List<List<Poster>>
)

val NoneEpisodes = Episodes(0, listOf())
val NoneEpisode = Episode(
    id = "",
    title = "",
    seriesId = "",
    seasonId = "",
    seasonTitle = "",
    seasonNumber = 0,
    episode = "",
    episodeNumber = 0,
    description = "",
    nextEpisodeId = "",
    nextEpisodeTitle = "",
    isSubbed = false,
    isDubbed = false,
    images = Thumbnail(listOf()),
    durationMs = 0,
    playback = ""
)

/**
 * Playback/stream data type
 */
@Serializable
data class Playback(
    @SerialName("audio_locale") val audioLocale: String,
    @SerialName("subtitles") val subtitles: Map<String, Subtitle>,
    @SerialName("streams") val streams: Streams,
)

@Serializable
data class Subtitle(
    @SerialName("locale") val locale: String,
    @SerialName("url") val url: String,
    @SerialName("format") val format: String,
)

@Serializable
data class Streams(
    @SerialName("adaptive_dash") val adaptive_dash: Map<String, Stream>,
    @SerialName("adaptive_hls") val adaptive_hls: Map<String, Stream>,
    @SerialName("download_hls") val download_hls: Map<String, Stream>,
    @SerialName("drm_adaptive_dash") val drm_adaptive_dash: Map<String, Stream>,
    @SerialName("drm_adaptive_hls") val drm_adaptive_hls: Map<String, Stream>,
    @SerialName("drm_download_hls") val drm_download_hls: Map<String, Stream>,
    @SerialName("trailer_dash") val trailer_dash: Map<String, Stream>,
    @SerialName("trailer_hls") val trailer_hls: Map<String, Stream>,
    @SerialName("vo_adaptive_dash") val vo_adaptive_dash: Map<String, Stream>,
    @SerialName("vo_adaptive_hls") val vo_adaptive_hls: Map<String, Stream>,
    @SerialName("vo_drm_adaptive_dash") val vo_drm_adaptive_dash: Map<String, Stream>,
    @SerialName("vo_drm_adaptive_hls") val vo_drm_adaptive_hls: Map<String, Stream>,
)

@Serializable
data class Stream(
    @SerialName("hardsub_locale") val hardsubLocale: String,
    @SerialName("url") val url: String,
    @SerialName("vcodec") val vcodec: String,
)

val NonePlayback = Playback(
    "",
    mapOf(),
    Streams(
        mapOf(), mapOf(), mapOf(), mapOf(), mapOf(), mapOf(),
        mapOf(), mapOf(), mapOf(), mapOf(), mapOf(), mapOf(),
    )
)
