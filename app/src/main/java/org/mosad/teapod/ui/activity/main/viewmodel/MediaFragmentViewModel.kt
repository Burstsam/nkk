package org.mosad.teapod.ui.activity.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import org.mosad.teapod.parser.crunchyroll.*
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.util.DataTypes.MediaType
import org.mosad.teapod.util.Meta
import org.mosad.teapod.util.tmdb.*

/**
 * handle media, next ep and tmdb
 * TODO this lives in activity, is this correct?
 */
class MediaFragmentViewModel(application: Application) : AndroidViewModel(application) {

//    var mediaCrunchy = NoneItem
//        internal set
    var seriesCrunchy = NoneSeries // movies are also series
        internal set
    var seasonsCrunchy = NoneSeasons
        internal set
    var currentSeasonCrunchy = NoneSeason
        internal set
    var episodesCrunchy = NoneEpisodes
        internal set
    val currentEpisodesCrunchy = arrayListOf<Episode>() // used for EpisodeItemAdapter (easier updates)

    var mediaType = MediaType.OTHER
        internal set
    var tmdbResult: TMDBResult = NoneTMDB // TODO rename
        internal set
    var tmdbTVSeason: TMDBTVSeason = NoneTMDBTVSeason
        internal set
    var mediaMeta: Meta? = null
        internal set

    /**
     * @param crunchyId the crunchyroll series id
     */

    suspend fun loadCrunchy(crunchyId: String) {
        // load series and seasons info in parallel
        listOf(
            viewModelScope.launch { seriesCrunchy = Crunchyroll.series(crunchyId) },
            viewModelScope.launch { seasonsCrunchy = Crunchyroll.seasons(crunchyId) }
        ).joinAll()

        println("series: $seriesCrunchy")
        println("seasons: $seasonsCrunchy")

        // TODO load episodes, metaDB and tmdb in parallel

        // load the preferred season (preferred language, language per season, not per stream)
        currentSeasonCrunchy = seasonsCrunchy.getPreferredSeason(Preferences.preferredLocal)
        episodesCrunchy = Crunchyroll.episodes(currentSeasonCrunchy.id)
        currentEpisodesCrunchy.clear()
        currentEpisodesCrunchy.addAll(episodesCrunchy.items)
        println("episodes: $episodesCrunchy")

        // set media type
        mediaType = episodesCrunchy.items.firstOrNull()?.let {
            if (it.episodeNumber != null) MediaType.TVSHOW else MediaType.MOVIE
        } ?: MediaType.OTHER

        // TODO check if metaDB knows the title
        mediaMeta = null // set mediaMeta to null, if metaDB doesn't know the media

        // use tmdb search to get media info
        loadTmdbInfo()
    }

    /**
     * Set currentSeasonCrunchy based on the season id. Also set the new seasons episodes.
     *
     * @param seasonId the id of the season to set
     */
    suspend fun setCurrentSeason(seasonId: String) {
        // return if the id hasn't changed (performance)
        if (currentSeasonCrunchy.id == seasonId) return

        // set currentSeasonCrunchy to the new season with id == seasonId, if the id isn't found,
        // don't change the current season (this should/can never happen)
        currentSeasonCrunchy = seasonsCrunchy.items.firstOrNull {
            it.id == seasonId
        } ?: currentSeasonCrunchy

        episodesCrunchy = Crunchyroll.episodes(currentSeasonCrunchy.id)
        currentEpisodesCrunchy.clear()
        currentEpisodesCrunchy.addAll(episodesCrunchy.items)
    }

    /**
     * Load the tmdb info for the selected media.
     * The TMDB search return a media type, use this to get the details (movie/tv show and season)
     */
    @ExperimentalSerializationApi
    suspend fun loadTmdbInfo() {
        val tmdbApiController = TMDBApiController()

        val tmdbSearchResult = when(mediaType) {
            MediaType.MOVIE -> tmdbApiController.searchMovie(seriesCrunchy.title)
            MediaType.TVSHOW -> tmdbApiController.searchTVShow(seriesCrunchy.title)
            else -> NoneTMDBSearch
        }
        println(tmdbSearchResult)

        tmdbResult = if (tmdbSearchResult.results.isNotEmpty()) {
            when (val result = tmdbSearchResult.results.first()) {
                is TMDBSearchResultMovie -> tmdbApiController.getMovieDetails(result.id)
                is TMDBSearchResultTVShow -> tmdbApiController.getTVShowDetails(result.id)
                else -> NoneTMDB
            }
        } else NoneTMDB

        println(tmdbResult)

        // currently not used
//        tmdbTVSeason = if (tmdbResult is TMDBTVShow) {
//            tmdbApiController.getTVSeasonDetails(tmdbResult.id, 0)
//        } else NoneTMDBTVSeason
    }

    /**
     * get the next episode based on episodeId
     * if no matching is found, use first episode
     */
    fun updateNextEpisode(episodeId: Int) {
        // TODO reimplement if needed
//        if (media.type == MediaType.MOVIE) return // return if movie
//
//        nextEpisodeId = media.playlist.firstOrNull { it.index > media.getEpisodeById(episodeId).index }?.mediaId
//            ?: media.playlist.first().mediaId
    }

}
