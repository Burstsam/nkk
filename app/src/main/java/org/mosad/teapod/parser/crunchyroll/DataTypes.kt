/**
 * Teapod
 *
 * Copyright 2020-2022  <seil0@mosad.xyz>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 *
 */

package org.mosad.teapod.parser.crunchyroll

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

val supportedLocals = listOf(
    Locale.forLanguageTag("ar-SA"),
    Locale.forLanguageTag("de-DE"),
    Locale.forLanguageTag("en-US"),
    Locale.forLanguageTag("es-419"),
    Locale.forLanguageTag("es-ES"),
    Locale.forLanguageTag("fr-FR"),
    Locale.forLanguageTag("it-IT"),
    Locale.forLanguageTag("pt-BR"),
    Locale.forLanguageTag("pt-PT"),
    Locale.forLanguageTag("ru-RU"),
    Locale.ROOT
)

/**
 * data classes for browse
 * TODO make class names more clear/possibly overlapping for now
 */
enum class SortBy(val str: String) {
    ALPHABETICAL("alphabetical"),
    NEWLY_ADDED("newly_added"),
    POPULARITY("popularity")
}

/**
 * token, index, account. This must pe present for the app to work!
 */
@Serializable
data class Token(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String,
    @SerialName("scope") val scope: String,
    @SerialName("country") val country: String,
    @SerialName("account_id") val accountId: String,
)

@Serializable
data class Index(
    @SerialName("cms") val cms: CMS,
    @SerialName("service_available") val serviceAvailable: Boolean,
)

@Serializable
data class CMS(
    @SerialName("bucket") val bucket: String,
    @SerialName("policy") val policy: String,
    @SerialName("signature") val signature: String,
    @SerialName("key_pair_id") val keyPairId: String,
    @SerialName("expires") val expires: String,
)

@Serializable
data class Account(
    @SerialName("account_id") val accountId: String,
    @SerialName("external_id") val externalId: String,
    @SerialName("email_verified") val emailVerified: Boolean,
    @SerialName("created") val created: String,
)
val NoneAccount = Account("", "", false, "")

/**
 * search, browse, DiscSeasonList, Watchlist, ContinueWatchingList data types all use Collection
 */

@Serializable
data class Collection<T>(
    @SerialName("total") val total: Int,
    @SerialName("items") val items: List<T>
)

typealias SearchResult = Collection<SearchCollection>
typealias SearchCollection = Collection<Item>
typealias BrowseResult = Collection<Item>
typealias DiscSeasonList = Collection<SeasonListItem>
typealias Watchlist = Collection<Item>
typealias ContinueWatchingList = Collection<ContinueWatchingItem>

@Serializable
data class UpNextSeriesItem(
    @SerialName("playhead") val playhead: Int,
    @SerialName("fully_watched") val fullyWatched: Boolean,
    @SerialName("never_watched") val neverWatched: Boolean,
    @SerialName("panel") val panel: EpisodePanel,
)

/**
 * panel data classes
 */

// the data class Item is used in browse and search
// TODO rename to MediaPanel
@Serializable
data class Item(
    val id: String,
    val title: String,
    val type: String,
    val channel_id: String,
    val description: String,
    val images: Images
    // TODO series_metadata etc.
)

@Serializable
data class Images(val poster_tall: List<List<Poster>>, val poster_wide: List<List<Poster>>)
// crunchyroll why?

@Serializable
data class Poster(val height: Int, val width: Int, val source: String, val type: String)

/**
 * season list data classes
 */
@Serializable
data class SeasonListItem(
    @SerialName("id") val id: String,
    @SerialName("localization") val localization: SeasonListLocalization
)

@Serializable
data class SeasonListLocalization(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
)

/**
 * continue_watching_item data classes
 */
@Serializable
data class ContinueWatchingItem(
    @SerialName("panel") val panel: EpisodePanel,
    @SerialName("new") val new: Boolean,
    @SerialName("new_content") val newContent: Boolean,
    // not present in up_next_account -> continue_watching_item
//    @SerialName("is_favorite") val isFavorite: Boolean,
//    @SerialName("never_watched") val neverWatched: Boolean,
//    @SerialName("completion_status") val completionStatus: Boolean,
    @SerialName("playhead") val playhead: Int,
    // not present in watchlist -> continue_watching_item
    @SerialName("fully_watched") val fullyWatched: Boolean = false,
)

// EpisodePanel is used in ContinueWatchingItem
@Serializable
data class EpisodePanel(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("type") val type: String,
    @SerialName("channel_id") val channelId: String,
    @SerialName("description") val description: String,
    @SerialName("episode_metadata") val episodeMetadata: EpisodeMetadata,
    @SerialName("images") val images: Thumbnail,
    @SerialName("playback") val playback: String,
)

@Serializable
data class EpisodeMetadata(
    @SerialName("duration_ms") val durationMs: Int,
    @SerialName("season_id") val seasonId: String,
    @SerialName("series_id") val seriesId: String,
    @SerialName("series_title") val seriesTitle: String,
)

val NoneItem = Item("", "", "", "", "", Images(emptyList(), emptyList()))
val NoneEpisodeMetadata = EpisodeMetadata(0, "", "", "")
val NoneEpisodePanel = EpisodePanel("", "", "", "", "", NoneEpisodeMetadata, Thumbnail(listOf()), "")

val NoneCollection = Collection<Item>(0, emptyList())
val NoneSearchResult = SearchResult(0, emptyList())
val NoneBrowseResult = BrowseResult(0, emptyList())
val NoneDiscSeasonList = DiscSeasonList(0, emptyList())
val NoneContinueWatchingList = ContinueWatchingList(0, emptyList())

val NoneUpNextSeriesItem = UpNextSeriesItem(0, false, false, NoneEpisodePanel)

/**
 * Series data type
 */
@Serializable
data class Series(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("images") val images: Images,
    @SerialName("maturity_ratings") val maturityRatings: List<String>
)
val NoneSeries = Series("", "", "", Images(emptyList(), emptyList()), emptyList())

/**
 * Seasons data type
 */
@Serializable
data class Seasons(
    @SerialName("total") val total: Int,
    @SerialName("items") val items: List<Season>
) {
    fun getPreferredSeason(local: Locale): Season {
        // try to get the the first seasons which matches the preferred local
        items.forEach { season ->
            if (season.title.startsWith("(${local.language})", true)) {
                return season
            }
        }

        // if there is no season with the preferred local, try to find a subbed season
        items.forEach { season ->
            if (season.isSubbed) {
                return season
            }
        }

        // if there is no preferred language season and no sub, use the first season
        return items.first()
    }
}

@Serializable
data class Season(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("series_id") val seriesId: String,
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("is_subbed") val isSubbed: Boolean,
    @SerialName("is_dubbed") val isDubbed: Boolean,
)

val NoneSeasons = Seasons(0, emptyList())
val NoneSeason = Season("", "", "", 0, isSubbed = false, isDubbed = false)


/**
 * Episodes data type
 */
@Serializable
data class Episodes(
    @SerialName("total") val total: Int,
    @SerialName("items") val items: List<Episode>
)

@Serializable
data class Episode(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("series_id") val seriesId: String,
    @SerialName("season_title") val seasonTitle: String,
    @SerialName("season_id") val seasonId: String,
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("episode") val episode: String,
    @SerialName("episode_number") val episodeNumber: Int? = null,
    @SerialName("description") val description: String,
    @SerialName("next_episode_id") val nextEpisodeId: String? = null, // default/nullable value since optional
    @SerialName("next_episode_title") val nextEpisodeTitle: String? = null, // default/nullable value since optional
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

typealias PlayheadsMap = Map<String, PlayheadObject>

@Serializable
data class PlayheadObject(
    @SerialName("playhead") val playhead: Int,
    @SerialName("content_id") val contentId: String,
    @SerialName("fully_watched") val fullyWatched: Boolean,
    @SerialName("last_modified") val lastModified: String,
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

@Serializable
data class Profile(
    @SerialName("avatar") val avatar: String,
    @SerialName("email") val email: String,
    @SerialName("maturity_rating") val maturityRating: String,
    @SerialName("preferred_content_subtitle_language") val preferredContentSubtitleLanguage: String,
    @SerialName("username") val username: String,
)
val NoneProfile = Profile(
    avatar = "",
    email = "",
    maturityRating = "",
    preferredContentSubtitleLanguage = "",
    username = ""
)
