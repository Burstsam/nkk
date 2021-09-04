package org.mosad.teapod.ui.activity.player

import android.app.Application
import android.net.Uri
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.mosad.teapod.R
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.util.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * PlayerViewModel handles all stuff related to media/episodes.
 * When currentEpisode is changed the player will start playing it (not initial media),
 * the next episode will be update and the callback is handled.
 *
 * TODO rework don't use episodes for everything, use media instead
 *  this is a major rework of the AoDParser/Player/Media architecture
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val player = SimpleExoPlayer.Builder(application).build()
    private val dataSourceFactory = DefaultDataSourceFactory(application, Util.getUserAgent(application, "Teapod"))
    private val mediaSession = MediaSessionCompat(application, "TEAPOD_PLAYER_SESSION")

    val currentEpisodeChangedListener = ArrayList<() -> Unit>()
    private val preferredLanguage = if (Preferences.preferSecondary) Locale.JAPANESE else Locale.GERMAN

    var media: AoDMedia = AoDMediaNone
        internal set
    var currentEpisode = AoDEpisodeNone
        internal set
    var nextEpisode: AoDEpisode? = null
        internal set
    var mediaMeta: Meta? = null
        internal set
    var currentEpisodeMeta: EpisodeMeta? = null
        internal set
    var currentLanguage: Locale = Locale.ROOT
        internal set

    init {
        initMediaSession()
    }

    override fun onCleared() {
        super.onCleared()

        mediaSession.release()
        player.release()

        Log.d(javaClass.name, "Released player")
    }

    /**
     * set the media session to active
     * create a media session connector to set title and description
     */
    private fun initMediaSession() {
        val mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(player)

        mediaSession.isActive = true
    }

    fun loadMedia(mediaId: Int, episodeId: Int) {
        runBlocking {
            media = AoDParser.getMediaById(mediaId)
            mediaMeta = loadMediaMeta(media.aodId) // can be done blocking, since it should be cached
        }

        currentEpisode = media.getEpisodeById(episodeId)
        nextEpisode = selectNextEpisode()
        currentEpisodeMeta = getEpisodeMetaByAoDMediaId(currentEpisode.mediaId)
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
    fun playEpisode(episode: AoDEpisode, replace: Boolean = false, seekPosition: Long = 0) {
        val preferredStream = episode.getPreferredStream(currentLanguage)
        currentLanguage = preferredStream.language // update current language, since it may have changed
        currentEpisode = episode
        nextEpisode = selectNextEpisode()
        currentEpisodeMeta = getEpisodeMetaByAoDMediaId(episode.mediaId)
        currentEpisodeChangedListener.forEach { it() } // update player gui (title)

        val mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(
            MediaItem.fromUri(Uri.parse(preferredStream.url))
        )
        playMedia(mediaSource, replace, seekPosition)

        // if episodes has not been watched, mark as watched
        if (!episode.watched) {
            viewModelScope.launch {
                AoDParser.markAsWatched(media.aodId, episode.mediaId)
            }
        }
    }

    /**
     * change the players media source and start playback
     */
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

    fun getEpisodeMetaByAoDMediaId(aodMediaId: Int): EpisodeMeta? {
        val meta = mediaMeta
        return if (meta is TVShowMeta) {
            meta.episodes.firstOrNull { it.aodMediaId == aodMediaId }
        } else {
            null
        }
    }

    private suspend fun loadMediaMeta(aodId: Int): Meta? {
        return if (media.type == DataTypes.MediaType.TVSHOW) {
            MetaDBController().getTVShowMetadata(aodId)
        } else {
            null
        }
    }

    /**
     * Based on the current episodeId, get the next episode. If there is no next
     * episode, return null
     */
    private fun selectNextEpisode(): AoDEpisode? {
        return media.playlist.firstOrNull { it.number > media.getEpisodeById(currentEpisode.mediaId).number }
    }

}