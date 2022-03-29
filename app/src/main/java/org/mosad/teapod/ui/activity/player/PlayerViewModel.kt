/**
 * Teapod
 *
 * Copyright 2020-2022  <seil0@mosad.xyz>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 *
 */

package org.mosad.teapod.ui.activity.player

import android.app.Application
import android.net.Uri
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.mosad.teapod.R
import org.mosad.teapod.parser.crunchyroll.Crunchyroll
import org.mosad.teapod.parser.crunchyroll.NoneEpisode
import org.mosad.teapod.parser.crunchyroll.NoneEpisodes
import org.mosad.teapod.parser.crunchyroll.NonePlayback
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.util.metadb.EpisodeMeta
import org.mosad.teapod.util.metadb.Meta
import org.mosad.teapod.util.metadb.MetaDBController
import org.mosad.teapod.util.metadb.TVShowMeta
import java.util.*

/**
 * PlayerViewModel handles all stuff related to media/episodes.
 * When currentEpisode is changed the player will start playing it (not initial media),
 * the next episode will be update and the callback is handled.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val classTag = javaClass.name

    val player = SimpleExoPlayer.Builder(application).build()
    private val dataSourceFactory = DefaultDataSourceFactory(application, Util.getUserAgent(application, "Teapod"))
    private val mediaSession = MediaSessionCompat(application, "TEAPOD_PLAYER_SESSION")

    val currentEpisodeChangedListener = ArrayList<() -> Unit>()
    private var currentPlayhead: Long = 0

    // tmdb/meta data
    var mediaMeta: Meta? = null
        internal set
    var currentEpisodeMeta: EpisodeMeta? = null
        internal set
//    var tmdbTVSeason: TMDBTVSeason? =null
//        internal set

    // crunchyroll episodes/playback
    var episodes = NoneEpisodes
        internal set
    var currentEpisode = NoneEpisode
        internal set
    var currentPlayback = NonePlayback

    // current playback settings
    var currentLanguage: Locale = Preferences.preferredLocale
        internal set

    init {
        initMediaSession()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)

                if (state == ExoPlayer.STATE_ENDED) updatePlayhead()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)

                if (!isPlaying) updatePlayhead()
            }
        })
    }

    override fun onCleared() {
        super.onCleared()

        mediaSession.release()
        player.release()

        Log.d(classTag, "Released player")
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

    fun loadMediaAsync(seasonId: String, episodeId: String) = viewModelScope.launch {
        episodes = Crunchyroll.episodes(seasonId)
        mediaMeta = loadMediaMeta(episodes.items.first().seriesId)

        Log.d(classTag, "meta: $mediaMeta")

        setCurrentEpisode(episodeId)
        playCurrentMedia(currentPlayhead)
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
        updatePlayhead() // update playhead before switching to new episode
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

        // update current episode meta
        currentEpisodeMeta = if (mediaMeta is TVShowMeta && currentEpisode.episodeNumber != null) {
            (mediaMeta as TVShowMeta)
                .seasons[currentEpisode.seasonNumber - 1]
                .episodes[currentEpisode.episodeNumber!! - 1]
        } else {
            null
        }

        // update player gui (title, next ep button) after currentEpisode has changed
        currentEpisodeChangedListener.forEach { it() }

        // needs to be blocking, currentPlayback must be present when calling playCurrentMedia()
        runBlocking {
            joinAll(
                viewModelScope.launch(Dispatchers.IO) {
                    currentPlayback = Crunchyroll.playback(currentEpisode.playback)
                },
                viewModelScope.launch(Dispatchers.IO) {
                    Crunchyroll.playheads(listOf(currentEpisode.id))[currentEpisode.id]?.let {
                        // if the episode was fully watched, start at the beginning
                        currentPlayhead = if (it.fullyWatched) {
                            0
                        } else {
                            (it.playhead.times(1000)).toLong()
                        }
                    }
                }
            )
        }
        Log.i(classTag, "playback: ${currentEpisode.playback}")

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
                // if no language tag is present use the first entry
                currentLanguage = Locale.ROOT
                currentPlayback.streams.adaptive_hls.entries.first().value.url
            }
        }
        Log.d(classTag, "stream url: $url")

        // create the media source object
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(
            MediaItem.fromUri(Uri.parse(url))
        )

        // the actual player playback code
        player.setMediaSource(mediaSource)
        player.prepare()
        if (seekPosition > 0) player.seekTo(seekPosition)
        player.playWhenReady = true
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

    /**
     * Check if the current episode is the last in the episodes list.
     *
     * @return Boolean: true if it is the last, else false.
     */
    fun currentEpisodeIsLastEpisode(): Boolean {
        return episodes.items.lastOrNull()?.id == currentEpisode.id
    }

    private suspend fun loadMediaMeta(crSeriesId: String): Meta? {
        return MetaDBController.getTVShowMetadata(crSeriesId)
    }

    /**
     * Update the playhead of the current episode, if currentPosition > 1000ms.
     */
    private fun updatePlayhead() {
        val playhead = (player.currentPosition / 1000)

        if (playhead > 0 && Preferences.updatePlayhead) {
            viewModelScope.launch { Crunchyroll.postPlayheads(currentEpisode.id, playhead.toInt()) }
            Log.i(javaClass.name, "Set playhead for episode ${currentEpisode.id} to $playhead sec.")
        }
    }

}
