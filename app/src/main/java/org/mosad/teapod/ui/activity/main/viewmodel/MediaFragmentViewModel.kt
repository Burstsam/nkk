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

//    var media = Media(-1, "", MediaType.OTHER)
//        internal set
//    var nextEpisode = Episode()
//        internal set

    var media2 = AoDMediaNone
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
    suspend fun load(mediaId: Int) {
        val tmdbApiController = TMDBApiController()
        //media = AoDParser.getMediaById(mediaId)
        media2 = AoDParser.getMediaById2(mediaId)

        // check if metaDB knows the title
        val tmdbId: Int = if (MetaDBController.mediaList.media.contains(media2.aodId)) {
            // load media info from metaDB
            val metaDB = MetaDBController()
            mediaMeta = when (media2.type) {
                MediaType.MOVIE -> metaDB.getMovieMetadata(media2.aodId)
                MediaType.TVSHOW -> metaDB.getTVShowMetadata(media2.aodId)
                else -> null
            }

            mediaMeta?.tmdbId ?: -1
        } else {
            // use tmdb search to get media info
            mediaMeta = null // set mediaMeta to null, if metaDB doesn't know the media
            tmdbApiController.search(stripTitleInfo(media2.title), media2.type)
        }

        tmdbResult = when (media2.type) {
            MediaType.MOVIE -> tmdbApiController.getMovieDetails(tmdbId)
            MediaType.TVSHOW -> tmdbApiController.getTVShowDetails(tmdbId)
            else -> null
        }
        println(tmdbResult) // TODO

        // get season info, if metaDB knows the tv show
        tmdbTVSeason = if (media2.type == MediaType.TVSHOW && mediaMeta != null) {
            val tvShowMeta = mediaMeta as TVShowMeta
            tmdbApiController.getTVSeasonDetails(tvShowMeta.tmdbId, tvShowMeta.tmdbSeasonNumber)
        } else {
            null
        }

        if (media2.type == MediaType.TVSHOW) {
            //nextEpisode = media.episodes.firstOrNull{ !it.watched } ?: media.episodes.first()
            nextEpisodeId = media2.playlist.firstOrNull { !it.watched }?.mediaId
                ?: media2.playlist.first().mediaId
        }
    }

    /**
     * get the next episode based on episodeId
     * if no matching is found, use first episode
     */
    fun updateNextEpisode(episodeId: Int) {
        if (media2.type == MediaType.MOVIE) return // return if movie

//        nextEpisode = media.episodes.firstOrNull{ it.number > currentEp.number }
//            ?: media.episodes.first()

        nextEpisodeId = media2.playlist.firstOrNull { it.number > media2.getEpisodeById(episodeId).number }?.mediaId
            ?: media2.playlist.first().mediaId
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