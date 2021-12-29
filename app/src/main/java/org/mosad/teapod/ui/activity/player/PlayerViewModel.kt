package org.mosad.teapod.ui.activity.player

import android.app.Application
import android.net.Uri
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.runBlocking
import org.mosad.teapod.R
import org.mosad.teapod.parser.crunchyroll.Crunchyroll
import org.mosad.teapod.parser.crunchyroll.NoneEpisode
import org.mosad.teapod.parser.crunchyroll.NoneEpisodes
import org.mosad.teapod.parser.crunchyroll.NonePlayback
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.util.EpisodeMeta
import org.mosad.teapod.util.Meta
import org.mosad.teapod.util.TVShowMeta
import org.mosad.teapod.util.tmdb.TMDBTVSeason
import java.util.*
import kotlin.collections.ArrayList

/**
 * PlayerViewModel handles all stuff related to media/episodes.
 * When currentEpisode is changed the player will start playing it (not initial media),
 * the next episode will be update and the callback is handled.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val player = SimpleExoPlayer.Builder(application).build()
    private val dataSourceFactory = DefaultDataSourceFactory(application, Util.getUserAgent(application, "Teapod"))
    private val mediaSession = MediaSessionCompat(application, "TEAPOD_PLAYER_SESSION")

    val currentEpisodeChangedListener = ArrayList<() -> Unit>()
    private val preferredLanguage = if (Preferences.preferSecondary) Locale.JAPANESE else Locale.GERMAN

    // tmdb/meta data TODO currently not implemented for cr
    var mediaMeta: Meta? = null
        internal set
    var tmdbTVSeason: TMDBTVSeason? =null
        internal set
    var currentEpisodeMeta: EpisodeMeta? = null
        internal set

    // crunchyroll episodes/playback
    var episodes = NoneEpisodes
        internal set
    var currentEpisode = NoneEpisode
        internal set
    var currentPlayback = NonePlayback

    // current playback settings
    var currentLanguage: Locale = Preferences.preferredLocal
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

    fun loadMedia(seasonId: String, episodeId: String) {
        runBlocking {
            episodes = Crunchyroll.episodes(seasonId)
            //mediaMeta = loadMediaMeta(media.aodId) // can be done blocking, since it should be cached

            // TODO replace this with setCurrentEpisode
            currentEpisode = episodes.items.find { episode ->
                episode.id == episodeId
            } ?: NoneEpisode
            println("loading playback ${currentEpisode.playback}")

            currentPlayback = Crunchyroll.playback(currentEpisode.playback)
        }

        // TODO reimplement for cr
        // run async as it should be loaded by the time the episodes a
//        viewModelScope.launch {
//            // get tmdb season info, if metaDB knows the tv show
//            if (media.type == DataTypes.MediaType.TVSHOW && mediaMeta != null) {
//                val tvShowMeta = mediaMeta as TVShowMeta
//                tmdbTVSeason = TMDBApiController().getTVSeasonDetails(tvShowMeta.tmdbId, tvShowMeta.tmdbSeasonNumber)
//            }
//        }
//
//        currentEpisodeMeta = getEpisodeMetaByAoDMediaId(currentEpisodeAoD.mediaId)
//        currentLanguage = currentEpisodeAoD.getPreferredStream(preferredLanguage).language
    }

    fun setLanguage(language: Locale) {
        currentLanguage = language
        playCurrentMedia(player.currentPosition)
    }

    // player actions

    fun seekToOffset(offset: Long) {
        player.seekTo(player.currentPosition + offset)
    }

    fun togglePausePlay() {
        if (player.isPlaying) player.pause() else player.play()
    }

    /**
     * play the next episode, if nextEpisodeId is not null
     */
    fun playNextEpisode() = currentEpisode.nextEpisodeId?.let { nextEpisodeId ->
        setCurrentEpisode(nextEpisodeId, startPlayback = true)
    }

    /**
     * Set currentEpisodeCr to the episode of the given ID
     * @param episodeId The ID of the episode you want to set currentEpisodeCr to
     */
    fun setCurrentEpisode(episodeId: String, startPlayback: Boolean = false) {
        currentEpisode = episodes.items.find { episode ->
            episode.id == episodeId
        } ?: NoneEpisode

        // TODO don't run blocking
        runBlocking {
            currentPlayback = Crunchyroll.playback(currentEpisode.playback)
        }

        // TODO update metadata and language (it should not be needed to update the language here!)

        if (startPlayback) {
            playCurrentMedia()
        }
    }

    /**
     * Play the current media from currentPlaybackCr.
     *
     * @param seekPosition The seek position for the episode (default = 0).
     */
    fun playCurrentMedia(seekPosition: Long = 0) {
        // update player gui (title, next ep button) after nextEpisodeId has been set
        currentEpisodeChangedListener.forEach { it() }

        // get preferred stream url, set current language if it differs from the preferred one
        val preferredLocale = currentLanguage
        val fallbackLocal = Locale.US
        val url = when {
            currentPlayback.streams.adaptive_hls.containsKey(preferredLocale.toLanguageTag()) -> {
                currentPlayback.streams.adaptive_hls[preferredLocale.toLanguageTag()]?.url
            }
            currentPlayback.streams.adaptive_hls.containsKey(fallbackLocal.toLanguageTag()) -> {
                currentLanguage = fallbackLocal
                currentPlayback.streams.adaptive_hls[fallbackLocal.toLanguageTag()]?.url
            }
            else -> {
                currentLanguage = Locale.ROOT
                currentPlayback.streams.adaptive_hls[Locale.ROOT.toLanguageTag()]?.url ?: ""
            }
        }


        println("stream url: $url")

        // create the media source object
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(
            MediaItem.fromUri(Uri.parse(url))
        )

        // the actual player playback code
        player.setMediaSource(mediaSource)
        player.prepare()
        if (seekPosition > 0) player.seekTo(seekPosition)
        player.playWhenReady = true

        // TODO reimplement mark as watched for cr, if needed
    }

    /**
     * Returns the current episode title (with episode number, if it's a tv show)
     */
    fun getMediaTitle(): String {
        // currentEpisode.episodeNumber defines the media type (tv show = none null, movie = null)
        return if (currentEpisode.episodeNumber != null) {
            getApplication<Application>().getString(
                R.string.component_episode_title,
                currentEpisode.episode,
                currentEpisode.title
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

    // TODO reimplement for cr
    private suspend fun loadMediaMeta(aodId: Int): Meta? {
//        return if (media.type == DataTypes.MediaType.TVSHOW) {
//            MetaDBController().getTVShowMetadata(aodId)
//        } else {
//            null
//        }

        return null
    }

}