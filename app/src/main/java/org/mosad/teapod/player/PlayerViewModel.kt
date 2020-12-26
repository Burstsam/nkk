package org.mosad.teapod.player

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.runBlocking
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.ui.fragments.MediaFragment
import org.mosad.teapod.util.DataTypes
import org.mosad.teapod.util.Episode
import org.mosad.teapod.util.Media
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

/**
 * PlayerViewModel handles all stuff related to media/episodes.
 * When currentEpisode is changed the player will start playing it (not initial media),
 * the next episode will be update and the callback is handled.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val player = SimpleExoPlayer.Builder(application).build()
    val dataSourceFactory = DefaultDataSourceFactory(application, Util.getUserAgent(application, "Teapod"))

    val currentEpisodeChangedListener = ArrayList<() -> Unit>()

    var media: Media = Media(-1, "", DataTypes.MediaType.OTHER)
        internal set

    // TODO rework
    var currentEpisode: Episode by Delegates.observable(Episode()) { _, _, _ ->
        currentEpisodeChangedListener.forEach { it() }
        MediaFragment.instance.updateWatchedState(currentEpisode) // watchedCallback for the new episode
        currentStreamUrl = autoSelectStream(currentEpisode)
        nextEpisode = selectNextEpisode() // update next ep
    }
    var currentStreamUrl = "" // TODO don't save selected stream for language, instead save selected language
        internal set
    var nextEpisode: Episode? = null
        internal set

    fun loadMedia(mediaId: Int, episodeId: Int) {
        runBlocking {
            media = AoDParser.getMediaById(mediaId)
        }

        currentEpisode = media.getEpisodeById(episodeId)
        currentStreamUrl = autoSelectStream(currentEpisode)
        nextEpisode = selectNextEpisode()
    }

    fun changeLanguage(url: String) {
        println("new stream is: $url")

        val seekTime = player.currentPosition
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(
            MediaItem.fromUri(Uri.parse(url))
        )
        currentStreamUrl = url

        playMedia(mediaSource, true, seekTime)
    }

    /**
     * update currentEpisode
     * updateWatchedState for the next (now current) episode
     */
    fun nextEpisode() = nextEpisode?.let { nextEp ->
        currentEpisode = nextEp // set current ep to next ep
    }

    // player actions
    fun seekToOffset(offset: Long) {
        player.seekTo(player.currentPosition + offset)
    }

    fun togglePausePlay() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun playMedia(episode: Episode, replace: Boolean = false, seekPosition: Long = 0) {
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(
            MediaItem.fromUri(Uri.parse(autoSelectStream(episode)))
        )

        playMedia(mediaSource, replace, seekPosition)
    }

    fun playMedia(source: MediaSource, replace: Boolean = false, seekPosition: Long = 0) {
        if (replace || player.contentDuration == C.TIME_UNSET) {
            player.setMediaSource(source)
            player.prepare()
            if (seekPosition > 0) player.seekTo(seekPosition)
            player.playWhenReady = true
        }
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

    /**
     * If preferSecondary use the japanese stream, if present.
     * If the preferred stream is not present the default (first)
     * stream will be used
     */
    private fun autoSelectStream(episode: Episode): String {
        return if (Preferences.preferSecondary) {
            episode.getPreferredStream(Locale.JAPANESE).url
        } else {
            episode.getPreferredStream(Locale.GERMAN).url
        }
    }


}