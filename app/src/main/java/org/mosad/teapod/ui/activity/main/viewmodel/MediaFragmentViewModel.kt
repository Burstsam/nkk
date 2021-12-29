package org.mosad.teapod.ui.activity.main.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.mosad.teapod.parser.crunchyroll.*
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.util.DataTypes.MediaType
import org.mosad.teapod.util.Meta
import org.mosad.teapod.util.tmdb.TMDBApiController
import org.mosad.teapod.util.tmdb.TMDBResult
import org.mosad.teapod.util.tmdb.TMDBTVSeason

/**
 * handle media, next ep and tmdb
 * TODO this lives in activity, is this correct?
 */
class MediaFragmentViewModel(application: Application) : AndroidViewModel(application) {

//    var mediaCrunchy = NoneItem
//        internal set
    var seriesCrunchy = NoneSeries // TODO it seems movies also series?
        internal set
    var seasonsCrunchy = NoneSeasons
        internal set
    var currentSeasonCrunchy = NoneSeason
        internal set
    var episodesCrunchy = NoneEpisodes
        internal set
    val currentEpisodesCrunchy = arrayListOf<Episode>()

    var tmdbResult: TMDBResult? = null // TODO rename
        internal set
    var tmdbTVSeason: TMDBTVSeason? =null
        internal set
    var mediaMeta: Meta? = null
        internal set

    /**
     * @param crunchyId the crunchyroll series id
     */
    suspend fun loadCrunchy(crunchyId: String) {
        val tmdbApiController = TMDBApiController()

        // load series and seasons info in parallel
        listOf(
            viewModelScope.launch { seriesCrunchy = Crunchyroll.series(crunchyId) },
            viewModelScope.launch { seasonsCrunchy = Crunchyroll.seasons(crunchyId) }
        ).joinAll()

        println("series: $seriesCrunchy")
        println("seasons: $seasonsCrunchy")

        // load the preferred season (preferred language, language per season, not per stream)
        currentSeasonCrunchy = seasonsCrunchy.getPreferredSeason(Preferences.preferredLocal)
        episodesCrunchy = Crunchyroll.episodes(currentSeasonCrunchy.id)
        currentEpisodesCrunchy.addAll(episodesCrunchy.items)
        println("episodes: $episodesCrunchy")

        // TODO check if metaDB knows the title

        // use tmdb search to get media info TODO media type is hardcoded, use episodeNumber? (if null it should be a movie)
        mediaMeta = null // set mediaMeta to null, if metaDB doesn't know the media
        val tmdbId = tmdbApiController.search(seriesCrunchy.title, MediaType.TVSHOW)

        tmdbResult = when (MediaType.TVSHOW) {
            MediaType.MOVIE -> tmdbApiController.getMovieDetails(tmdbId)
            MediaType.TVSHOW -> tmdbApiController.getTVShowDetails(tmdbId)
            else -> null
        }
    }

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
     * set media, tmdb and nextEpisode
     * TODO run aod and tmdb load parallel
     */
//    suspend fun loadAoD(aodId: Int) {
//        val tmdbApiController = TMDBApiController()
//        media = AoDParser.getMediaById(aodId)
//
//        // check if metaDB knows the title
//        val tmdbId: Int = if (MetaDBController.mediaList.media.contains(aodId)) {
//            // load media info from metaDB
//            val metaDB = MetaDBController()
//            mediaMeta = when (media.type) {
//                MediaType.MOVIE -> metaDB.getMovieMetadata(media.aodId)
//                MediaType.TVSHOW -> metaDB.getTVShowMetadata(media.aodId)
//                else -> null
//            }
//
//            mediaMeta?.tmdbId ?: -1
//        } else {
//            // use tmdb search to get media info
//            mediaMeta = null // set mediaMeta to null, if metaDB doesn't know the media
//            tmdbApiController.search(stripTitleInfo(media.title), media.type)
//        }
//
//        tmdbResult = when (media.type) {
//            MediaType.MOVIE -> tmdbApiController.getMovieDetails(tmdbId)
//            MediaType.TVSHOW -> tmdbApiController.getTVShowDetails(tmdbId)
//            else -> null
//        }
//
//        // get season info, if metaDB knows the tv show
//        tmdbTVSeason = if (media.type == MediaType.TVSHOW && mediaMeta != null) {
//            val tvShowMeta = mediaMeta as TVShowMeta
//            tmdbApiController.getTVSeasonDetails(tvShowMeta.tmdbId, tvShowMeta.tmdbSeasonNumber)
//        } else {
//            null
//        }
//
//        if (media.type == MediaType.TVSHOW) {
//            //nextEpisode = media.episodes.firstOrNull{ !it.watched } ?: media.episodes.first()
//            nextEpisodeId = media.playlist.firstOrNull { !it.watched }?.mediaId
//                ?: media.playlist.first().mediaId
//        }
//    }

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

    // remove unneeded info from the media title before searching
    private fun stripTitleInfo(title: String): String {
        return title.replace("(Sub)", "")
            .replace(Regex("-?\\s?[0-9]+.\\s?(Staffel|Season)"), "")
            .replace(Regex("(Staffel|Season)\\s?[0-9]+"), "")
            .trim()
    }

    /** guess Season from title
     * if the title ends with a number, that could be the season
     * if the title ends with Regex("-?\\s?[0-9]+.\\s?(Staffel|Season)") or
     * Regex("(Staffel|Season)\\s?[0-9]+"), that is the season information
     */
    private fun guessSeasonFromTitle(title: String): Int {
        val helpTitle = title.replace("(Sub)", "").trim()
        Log.d("test", "helpTitle: $helpTitle")

        return if (helpTitle.last().isDigit()) {
            helpTitle.last().digitToInt()
        } else {
            Regex("([0-9]+.\\s?(Staffel|Season))|((Staffel|Season)\\s?[0-9]+)")
                .find(helpTitle)
                ?.value?.filter { it.isDigit() }?.toInt() ?: 1
        }
    }

}