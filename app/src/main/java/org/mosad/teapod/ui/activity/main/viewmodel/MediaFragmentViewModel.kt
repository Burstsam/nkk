package org.mosad.teapod.ui.activity.main.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.*
import org.mosad.teapod.util.DataTypes.MediaType
import org.mosad.teapod.util.tmdb.Movie
import org.mosad.teapod.util.tmdb.TMDBApiController
import org.mosad.teapod.util.tmdb.TMDBResult

/**
 * handle media, next ep and tmdb
 */
class MediaFragmentViewModel(application: Application) : AndroidViewModel(application) {

    var media = Media(-1, "", MediaType.OTHER)
        internal set
    var nextEpisode = Episode()
        internal set
    lateinit var tmdbResult: TMDBResult // TODO rename
        internal set

    /**
     * set media, tmdb and nextEpisode
     */
    suspend fun load(mediaId: Int) {
        media = AoDParser.getMediaById(mediaId)

        val tmdbApiController = TMDBApiController()
        val searchTitle = stripTitleInfo(media.info.title)
        val tmdbId = tmdbApiController.search(searchTitle, media.type)

        tmdbResult = when (media.type) {
            MediaType.MOVIE -> tmdbApiController.getMovieDetails(tmdbId)
            MediaType.TVSHOW -> tmdbApiController.getTVShowDetails(tmdbId)
            else -> Movie(-1)
        }
        println(tmdbResult) // TODO

        // TESTING
        if (media.type == MediaType.TVSHOW) {
            val seasonNumber = guessSeasonFromTitle(media.info.title)
            Log.d("test", "season number: $seasonNumber")

            // TODO Important: only use tmdb info if media title and episode number match exactly
            val tmdbTVSeason = tmdbApiController.getTVSeasonDetails(tmdbId, seasonNumber)
            Log.d("test", "Season Info: $tmdbTVSeason.")
        }

        // TESTING END

        if (media.type == MediaType.TVSHOW) {
            nextEpisode = media.episodes.firstOrNull{ !it.watched } ?: media.episodes.first()
        }
    }

    /**
     * get the next episode based on episode number (the true next episode)
     * if no matching is found, use first episode
     */
    fun updateNextEpisode(currentEp: Episode) {
        if (media.type == MediaType.MOVIE) return // return if movie

        nextEpisode = media.episodes.firstOrNull{ it.number > currentEp.number }
            ?: media.episodes.first()
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