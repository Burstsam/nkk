package org.mosad.teapod.player

import androidx.lifecycle.ViewModel
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.ui.fragments.MediaFragment
import org.mosad.teapod.util.DataTypes
import org.mosad.teapod.util.Episode
import org.mosad.teapod.util.Media

class PlayerViewModel : ViewModel() {

    var mediaId = 0
        internal set
    var episodeId = 0
        internal set

    var media: Media = Media(0, "", DataTypes.MediaType.OTHER)
        internal set
    var currentEpisode = Episode()
        internal set
    var nextEpisode: Episode? = null
        internal set

    fun loadMedia(iMediaId: Int, iEpisodeId: Int) {
        mediaId = iMediaId
        episodeId = iEpisodeId

        media = AoDParser.getMediaById(mediaId)
        currentEpisode = media.episodes.first { it.id == episodeId }
        nextEpisode = selectNextEpisode()
    }

    /**
     * update currentEpisode, episodeId, nextEpisode to new episode
     * updateWatchedState for the next (now current) episode
     */
    fun nextEpisode() = nextEpisode?.let { nextEp ->
        currentEpisode = nextEp // set current ep to next ep
        episodeId = nextEp.id
        MediaFragment.instance.updateWatchedState(nextEp) // watchedCallback for next ep

        nextEpisode = selectNextEpisode()
    }

    /**
     * Based on the current episodeId, get the next episode. If there is no next
     * episode, return null
     */
    private fun selectNextEpisode(): Episode? {
        val nextEpIndex = media.episodes.indexOfFirst { it.id == currentEpisode.id } + 1
        return if (nextEpIndex < (media.episodes.size)) {
            media.episodes[nextEpIndex]
        } else {
            null
        }
    }

}