package org.mosad.teapod.ui.activity.player

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.runBlocking
import org.mosad.teapod.R
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.util.DataTypes
import org.mosad.teapod.util.Episode
import org.mosad.teapod.util.Media
import java.util.*
import kotlin.collections.ArrayList

/**
 * PlayerViewModel handles all stuff related to media/episodes.
 * When currentEpisode is changed the player will start playing it (not initial media),
 * the next episode will be update and the callback is handled.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val player = SimpleExoPlayer.Builder(application).build()
    val dataSourceFactory = DefaultDataSourceFactory(application, Util.getUserAgent(application, "Teapod"))

    val currentEpisodeChangedListener = ArrayList<() -> Unit>()
    val preferredLanguage = if (Preferences.preferSecondary) Locale.JAPANESE else Locale.GERMAN

    var media: Media = Media(-1, "", DataTypes.MediaType.OTHER)
        internal set
    var currentEpisode = Episode()
        internal set
    var nextEpisode: Episode? = null
        internal set
    var currentLanguage: Locale = Locale.ROOT
        internal set

    override fun onCleared() {
        super.onCleared()
        player.release()

        Log.d(javaClass.name, "Released player")
    }

    fun loadMedia(mediaId: Int, episodeId: Int) {
        runBlocking {
            media = AoDParser.getMediaById(mediaId)
        }

        currentEpisode = media.getEpisodeById(episodeId)
        nextEpisode = selectNextEpisode()
        currentLanguage = currentEpisode.getPreferredStream(preferredLanguage).language
    }

    fun setLanguage(language: Locale) {
        currentLanguage = language

        val seekTime = player.currentPosition
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(
            MediaItem.fromUri(Uri.parse(currentEpisode.getPreferredStream(language).url))
        )
        playMedia(mediaSource, true, seekTime)
    }

    // player actions

    fun seekToOffset(offset: Long) {
        player.seekTo(player.currentPosition + offset)
    }

    fun togglePausePlay() {
        if (player.isPlaying) player.pause() else player.play()
    }

    /**
     * play the next episode, if nextEpisode is not null
     */
    fun playNextEpisode() = nextEpisode?.let { it ->
        playEpisode(it, replace = true)
    }

    /**
     * set currentEpisode to the param episode and start playing it
     * update nextEpisode to reflect the change
     *
     * updateWatchedState for the next (now current) episode
     */
    fun playEpisode(episode: Episode, replace: Boolean = false, seekPosition: Long = 0) {
        val preferredStream = episode.getPreferredStream(currentLanguage)
        currentLanguage = preferredStream.language // update current language, since it may have changed
        currentEpisode = episode
        nextEpisode = selectNextEpisode()
        currentEpisodeChangedListener.forEach { it() } // update player gui (title)

        val mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(
            MediaItem.fromUri(Uri.parse(preferredStream.url))
        )
        playMedia(mediaSource, replace, seekPosition)

        // if episodes has not been watched, mark as watched
        if (!episode.watched) {
            AoDParser.markAsWatched(media.id, episode.id)
        }
    }

    fun playMedia(source: MediaSource, replace: Boolean = false, seekPosition: Long = 0) {
        if (replace || player.contentDuration == C.TIME_UNSET) {
            player.setMediaSource(source)
            player.prepare()
            if (seekPosition > 0) player.seekTo(seekPosition)
            player.playWhenReady = true
        }
    }

    fun getMediaTitle(): String {
        return if (media.type == DataTypes.MediaType.TVSHOW) {
            getApplication<Application>().getString(
                R.string.component_episode_title,
                currentEpisode.number,
                currentEpisode.description
            )
        } else {
            currentEpisode.title
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

}