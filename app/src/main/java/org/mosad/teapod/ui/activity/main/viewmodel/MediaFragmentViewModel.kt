package org.mosad.teapod.ui.activity.main.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.*
import org.mosad.teapod.util.DataTypes.MediaType
import org.mosad.teapod.util.tmdb.TMDBApiController
import org.mosad.teapod.util.tmdb.TMDBResult
import org.mosad.teapod.util.tmdb.TMDBTVSeason

/**
 * handle media, next ep and tmdb
 * TODO this lives in activity, is this correct?
 */
class MediaFragmentViewModel(application: Application) : AndroidViewModel(application) {

    var media = AoDMediaNone
        internal set
    var nextEpisodeId = -1
        internal set

    var tmdbResult: TMDBResult? = null // TODO rename
        internal set
    var tmdbTVSeason: TMDBTVSeason? =null
        internal set
    var mediaMeta: Meta? = null
        internal set

    /**
     * set media, tmdb and nextEpisode
     * TODO run aod and tmdb load parallel
     */
    suspend fun load(aodId: Int) {
        val tmdbApiController = TMDBApiController()
        media = AoDParser.getMediaById(aodId)

        // check if metaDB knows the title
        val tmdbId: Int = if (MetaDBController.mediaList.media.contains(aodId)) {
            // load media info from metaDB
            val metaDB = MetaDBController()
            mediaMeta = when (media.type) {
                MediaType.MOVIE -> metaDB.getMovieMetadata(media.aodId)
                MediaType.TVSHOW -> metaDB.getTVShowMetadata(media.aodId)
                else -> null
            }

            mediaMeta?.tmdbId ?: -1
        } else {
            // use tmdb search to get media info
            mediaMeta = null // set mediaMeta to null, if metaDB doesn't know the media
            tmdbApiController.search(stripTitleInfo(media.title), media.type)
        }

        tmdbResult = when (media.type) {
            MediaType.MOVIE -> tmdbApiController.getMovieDetails(tmdbId)
            MediaType.TVSHOW -> tmdbApiController.getTVShowDetails(tmdbId)
            else -> null
        }

        // get season info, if metaDB knows the tv show
        tmdbTVSeason = if (media.type == MediaType.TVSHOW && mediaMeta != null) {
            val tvShowMeta = mediaMeta as TVShowMeta
            tmdbApiController.getTVSeasonDetails(tvShowMeta.tmdbId, tvShowMeta.tmdbSeasonNumber)
        } else {
            null
        }

        if (media.type == MediaType.TVSHOW) {
            //nextEpisode = media.episodes.firstOrNull{ !it.watched } ?: media.episodes.first()
            nextEpisodeId = media.playlist.firstOrNull { !it.watched }?.mediaId
                ?: media.playlist.first().mediaId
        }
    }

    /**
     * get the next episode based on episodeId
     * if no matching is found, use first episode
     */
    fun updateNextEpisode(episodeId: Int) {
        if (media.type == MediaType.MOVIE) return // return if movie

        nextEpisodeId = media.playlist.firstOrNull { it.number > media.getEpisodeById(episodeId).number }?.mediaId
            ?: media.playlist.first().mediaId
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