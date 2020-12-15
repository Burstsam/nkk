package org.mosad.teapod.player

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.runBlocking
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.ui.fragments.MediaFragment
import org.mosad.teapod.util.DataTypes
import org.mosad.teapod.util.Episode
import org.mosad.teapod.util.Media
import kotlin.properties.Delegates

/**
 * PlayerViewModel handles all stuff related to media/episodes.
 * When currentEpisode is changed the player will start playing it (not initial media),
 * the next episode will be update and the callback is handled.
 */
class PlayerViewModel : ViewModel() {

    val currentEpisodeChangedListener = ArrayList<() -> Unit>()

    var mediaId = 0
        internal set
    var episodeId = 0
        internal set

    var media: Media = Media(0, "", DataTypes.MediaType.OTHER)
        internal set
    var currentEpisode: Episode by Delegates.observable(Episode()) { _, _, _ ->
        currentEpisodeChangedListener.forEach { it() }
        MediaFragment.instance.updateWatchedState(currentEpisode) // watchedCallback for the new episode
        nextEpisode = selectNextEpisode() // update next ep
    }
    var nextEpisode: Episode? = null
        internal set

    fun loadMedia(iMediaId: Int, iEpisodeId: Int) {
        mediaId = iMediaId
        episodeId = iEpisodeId

        runBlocking {
            media = AoDParser.getMediaById(mediaId)
        }

        currentEpisode = media.episodes.first { it.id == episodeId }
        nextEpisode = selectNextEpisode()

        currentEpisode
    }

    /**
     * If preferSecondary or priStreamUrl is empty and secondary is present (secStreamOmU),
     * use the secondary stream. Else, if the primary stream is set use the primary stream.
     * If no stream is present, return empty string.
     */
    fun autoSelectStream(episode: Episode): String {
        return if ((Preferences.preferSecondary || episode.priStreamUrl.isEmpty()) && episode.secStreamOmU) {
            episode.secStreamUrl
        } else if (episode.priStreamUrl.isNotEmpty()) {
            episode.priStreamUrl
        } else {
            Log.e(javaClass.name, "No stream url set. ${episode.id}")
            ""
        }
    }

    /**
     * update currentEpisode, episodeId, nextEpisode to new episode
     * updateWatchedState for the next (now current) episode
     */
    fun nextEpisode() = nextEpisode?.let { nextEp ->
        currentEpisode = nextEp // set current ep to next ep
        episodeId = nextEp.id
    }

    /**
     * Based on the current episodeId, get the next episode. If there is no next
     * episode, return null
     */
    private fun selectNextEpisode(): Episode? {
        val nextEpIndex = media.episodes.indexOfFirst { it.id == currentEpisode.id } + 1
        return if (nextEpIndex < media.episodes.size) {
            media.episodes[nextEpIndex]
        } else {
            null
        }
    }

}