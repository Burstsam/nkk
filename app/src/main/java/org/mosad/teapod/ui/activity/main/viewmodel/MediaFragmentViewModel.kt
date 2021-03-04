package org.mosad.teapod.ui.activity.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.*
import org.mosad.teapod.util.DataTypes.MediaType

/**
 * handle media, next ep and tmdb
 */
class MediaFragmentViewModel(application: Application) : AndroidViewModel(application) {

    var media = Media(-1, "", MediaType.OTHER)
        internal set
    var nextEpisode = Episode()
        internal set
    var tmdb = TMDBResponse()
        internal set

    /**
     * set media, tmdb and nextEpisode
     */
    suspend fun load(mediaId: Int) {
        media = AoDParser.getMediaById(mediaId)
        tmdb = TMDBApiController().search(media.info.title, media.type)

        if (media.type == MediaType.TVSHOW) {
            nextEpisode = if (media.episodes.firstOrNull{ !it.watched } != null) {
                media.episodes.first{ !it.watched }
            } else {
                media.episodes.first()
            }
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

}