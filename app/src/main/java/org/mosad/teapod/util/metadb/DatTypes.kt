package org.mosad.teapod.util.metadb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// class representing the media list json object
@Serializable
data class MediaList(
    @SerialName("media") val media: List<String>
)

// abstract class used for meta data objects (tv, movie)
abstract class Meta {
    abstract val id: Int
    abstract val tmdbId: Int
    abstract val crSeriesId: String
}

// class representing the movie json object
@Serializable
data class MovieMeta(
    @SerialName("id") override val id: Int,
    @SerialName("tmdb_id") override val tmdbId: Int,
    @SerialName("cr_series_id") override val crSeriesId: String,
): Meta()

// class representing the tv show json object
@Serializable
data class TVShowMeta(
    @SerialName("id") override val id: Int,
    @SerialName("tmdb_id") override val tmdbId: Int,
    @SerialName("cr_series_id") override val crSeriesId: String,
    @SerialName("seasons") val seasons: List<SeasonMeta>,
): Meta()

// class used in TVShowMeta, part of the tv show json object
@Serializable
data class SeasonMeta(
    @SerialName("id") val id: Int,
    @SerialName("tmdb_season_id") val tmdbSeasonId: Int,
    @SerialName("tmdb_season_number") val tmdbSeasonNumber: Int,
    @SerialName("cr_season_ids") val crSeasonIds: List<String>,
    @SerialName("episodes") val episodes: List<EpisodeMeta>,
)

// class used in TVShowMeta, part of the tv show json object
@Serializable
data class EpisodeMeta(
    @SerialName("id") val id: Int,
    @SerialName("tmdb_episode_id") val tmdbEpisodeId: Int,
    @SerialName("tmdb_episode_number") val tmdbEpisodeNumber: Int,
    @SerialName("cr_episode_ids") val crEpisodeIds: List<String>,
    @SerialName("opening_start") val openingStart: Long,
    @SerialName("opening_duration") val openingDuration: Long,
    @SerialName("ending_start") val endingStart: Long,
    @SerialName("ending_duration") val endingDuration: Long
)