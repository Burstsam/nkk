package org.mosad.teapod.ui.activity.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.mosad.teapod.parser.crunchyroll.*
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.util.DataTypes.MediaType
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

    // additional media info
    val currentPlayheads: MutableMap<String, PlayheadObject> = mutableMapOf()
    var isWatchlist = false
        internal set
    var upNextSeries = NoneUpNextSeriesItem

    // TMDB stuff
    var mediaType = MediaType.OTHER
        internal set
    var tmdbResult: TMDBResult = NoneTMDB // TODO rename
        internal set
    var tmdbTVSeason: TMDBTVSeason = NoneTMDBTVSeason
        internal set

    /**
     * @param crunchyId the crunchyroll series id
     */

    suspend fun loadCrunchy(crunchyId: String) {
        // load series and seasons info in parallel
        listOf(
            viewModelScope.launch { seriesCrunchy = Crunchyroll.series(crunchyId) },
            viewModelScope.launch { seasonsCrunchy = Crunchyroll.seasons(crunchyId) },
            viewModelScope.launch { isWatchlist = Crunchyroll.isWatchlist(crunchyId) },
            viewModelScope.launch { upNextSeries = Crunchyroll.upNextSeries(crunchyId) }
        ).joinAll()

        // load the preferred season (preferred language, language per season, not per stream)
        currentSeasonCrunchy = seasonsCrunchy.getPreferredSeason(Preferences.preferredLocale)

        // Note: if we need to query metaDB, do it now

        // load episodes and metaDB in parallel (tmdb needs mediaType, which is set via episodes)
        viewModelScope.launch { episodesCrunchy = Crunchyroll.episodes(currentSeasonCrunchy.id) }.join()
        currentEpisodesCrunchy.clear()
        currentEpisodesCrunchy.addAll(episodesCrunchy.items)

        // set media type
        mediaType = episodesCrunchy.items.firstOrNull()?.let {
            if (it.episodeNumber != null) MediaType.TVSHOW else MediaType.MOVIE
        } ?: MediaType.OTHER

        // load playheads and tmdb in parallel
        listOf(
            viewModelScope.launch {
                // get playheads (including fully watched state)
                val episodeIDs = episodesCrunchy.items.map { it.id }
                currentPlayheads.clear()
                currentPlayheads.putAll(Crunchyroll.playheads(episodeIDs))
            },
            viewModelScope.launch { loadTmdbInfo() } // use tmdb search to get media info
        ).joinAll()
    }

    /**
     * Load the tmdb info for the selected media.
     * The TMDB search return a media type, use this to get the details (movie/tv show and season)
     */
    private suspend fun loadTmdbInfo() {
        val tmdbApiController = TMDBApiController()

        val tmdbSearchResult = when(mediaType) {
            MediaType.MOVIE -> tmdbApiController.searchMovie(seriesCrunchy.title)
            MediaType.TVSHOW -> tmdbApiController.searchTVShow(seriesCrunchy.title)
            else -> NoneTMDBSearch
        }
//        println(tmdbSearchResult)

        tmdbResult = if (tmdbSearchResult.results.isNotEmpty()) {
            when (val result = tmdbSearchResult.results.first()) {
                is TMDBSearchResultMovie -> tmdbApiController.getMovieDetails(result.id)
                is TMDBSearchResultTVShow -> tmdbApiController.getTVShowDetails(result.id)
                else -> NoneTMDB
            }
        } else NoneTMDB
//        println(tmdbResult)

        // currently not used
//        tmdbTVSeason = if (tmdbResult is TMDBTVShow) {
//            tmdbApiController.getTVSeasonDetails(tmdbResult.id, 0)
//        } else NoneTMDBTVSeason
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

        // update playheads playheads (including fully watched state)
        val episodeIDs = episodesCrunchy.items.map { it.id }
        currentPlayheads.clear()
        currentPlayheads.putAll(Crunchyroll.playheads(episodeIDs))
    }

    suspend fun setWatchlist() {
        isWatchlist = if (isWatchlist) {
            Crunchyroll.deleteWatchlist(seriesCrunchy.id)
            false
        } else {
            Crunchyroll.postWatchlist(seriesCrunchy.id)
            true
        }
    }

    suspend fun updateOnResume() {
        joinAll(
            viewModelScope.launch {
                val episodeIDs = episodesCrunchy.items.map { it.id }
                currentPlayheads.clear()
                currentPlayheads.putAll(Crunchyroll.playheads(episodeIDs))
            },
            viewModelScope.launch { upNextSeries = Crunchyroll.upNextSeries(seriesCrunchy.id) }
        )
    }

}
