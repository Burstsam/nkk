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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.player_controls.*
import kotlinx.coroutines.launch
import org.mosad.teapod.R
import org.mosad.teapod.parser.crunchyroll.NoneEpisode
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.ui.components.EpisodesListPlayer
import org.mosad.teapod.ui.components.LanguageSettingsPlayer
import org.mosad.teapod.util.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.scheduleAtFixedRate

class PlayerActivity : AppCompatActivity() {

    private val model: PlayerViewModel by viewModels()

    private lateinit var controller: StyledPlayerControlView
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var timerUpdates: TimerTask

    private var wasInPiP = false
    private var remainingTime: Long = 0

    private val rwdTime: Long = 10000.unaryMinus()
    private val fwdTime: Long = 10000
    private val defaultShowTimeoutMs = 5000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        hideBars() // Initial hide the bars

        model.loadMedia(
            intent.getStringExtra(getString(R.string.intent_season_id)) ?: "",
            intent.getStringExtra(getString(R.string.intent_episode_id)) ?: ""
        )
        model.currentEpisodeChangedListener.add { onMediaChanged() }
        gestureDetector = GestureDetectorCompat(this, PlayerGestureListener())

        controller = video_view.findViewById(R.id.exo_controller)
        controller.isAnimationEnabled = false // disable controls (time-bar) animation

        initExoPlayer() // call in onCreate, exoplayer lives in view model
        initGUI()
        initActions()
    }

    /**
     * once minimum is android 7.0 this can be simplified
     * only onStart and onStop should be needed then
     * see: https://developer.android.com/guide/topics/ui/picture-in-picture#continuing_playback
     */
    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initPlayer()
            video_view?.onResume()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isInPiPMode()) { return }

        if (Util.SDK_INT <= 23) {
            initPlayer()
            video_view?.onResume()
        }
    }

    override fun onPause() {
        super.onPause()

        if (isInPiPMode()) { return }
        if (Util.SDK_INT <= 23) { onPauseOnStop() }
    }

    override fun onStop() {
        super.onStop()

        if (Util.SDK_INT > 23) { onPauseOnStop() }
        // if the player was in pip, it's on a different task
        if (wasInPiP) { navToLauncherTask() }
        // if the player is in pip, remove the task, else we'll get a zombie
        if (isInPiPMode()) { finishAndRemoveTask() }
    }

    /**
     * used, when the player is in pip and the user selects a new media
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // when the intent changed, load the new media and play it
        intent?.let {
            model.loadMedia(
                it.getStringExtra(getString(R.string.intent_season_id)) ?: "",
                it.getStringExtra(getString(R.string.intent_episode_id)) ?: ""
            )
            model.playCurrentMedia()
        }
    }

    /**
     * previous to android n, don't override
     */
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        // start pip mode, if supported
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                @Suppress("deprecation")
                enterPictureInPictureMode()
            } else {
                val width = model.player.videoFormat?.width ?: 0
                val height = model.player.videoFormat?.height ?: 0
                val contentFrame: View = video_view.findViewById(R.id.exo_content_frame)
                val contentRect = with(contentFrame) {
                    val (x, y) = intArrayOf(0, 0).also(::getLocationInWindow)
                    Rect(x, y, x + width, y + height)
                }

                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(width, height))
                    .setSourceRectHint(contentRect)
                    .build()
                enterPictureInPictureMode(params)
            }

            wasInPiP = isInPiPMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        // Hide the full-screen UI (controls, etc.) while in picture-in-picture mode.
        video_view.useController = !isInPictureInPictureMode
    }

    private fun initPlayer() {
        if (model.currentEpisode == NoneEpisode) {
            Log.e(javaClass.name, "No media was set.")
            this.finish()
        }

        initVideoView()
        initTimeUpdates()

        // if the player is ready or buffering we can simply play the file again, else do nothing
        val playbackState = model.player.playbackState
        if ((playbackState == ExoPlayer.STATE_READY || playbackState == ExoPlayer.STATE_BUFFERING)) {
            model.player.play()
        }
    }

    /**
     * set play when ready and listeners
     */
    private fun initExoPlayer() {
        model.player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)

                loading.visibility = when (state) {
                    ExoPlayer.STATE_READY -> View.GONE
                    ExoPlayer.STATE_BUFFERING -> View.VISIBLE
                    else -> View.GONE
                }

                exo_play_pause.visibility = when (loading.visibility) {
                    View.GONE -> View.VISIBLE
                    View.VISIBLE -> View.INVISIBLE
                    else -> View.VISIBLE
                }

                if (state == ExoPlayer.STATE_ENDED && hasNextEpisode() && Preferences.autoplay) {
                    playNextEpisode()
                }
            }
        })
        
        // start playing the current episode, after all needed player components have been initialized
        model.playCurrentMedia()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initVideoView() {
        video_view.player = model.player

        // when the player controls get hidden, hide the bars too
        video_view.setControllerVisibilityListener {
            when (it) {
                View.GONE -> {
                    hideBars()
                    // TODO also hide the skip op button
                }
                View.VISIBLE -> updateControls()
            }
        }

        video_view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun initActions() {
        exo_close_player.setOnClickListener {
            this.finish()
        }
        rwd_10.setOnButtonClickListener { rewind() }
        ffwd_10.setOnButtonClickListener { fastForward() }
        button_next_ep.setOnClickListener { playNextEpisode() }
        button_skip_op.setOnClickListener { skipOpening() }
        button_language.setOnClickListener { showLanguageSettings() }
        button_episodes.setOnClickListener { showEpisodesList() }
        button_next_ep_c.setOnClickListener { playNextEpisode() }
    }

    private fun initGUI() {
        // TODO reimplement for cr
//        if (model.media.type == DataTypes.MediaType.MOVIE) {
//            button_episodes.visibility = View.GONE
//        }
    }

    private fun initTimeUpdates() {
        if (this::timerUpdates.isInitialized) {
            timerUpdates.cancel()
        }

        timerUpdates = Timer().scheduleAtFixedRate(0, 500) {
            lifecycleScope.launch {
                val currentPosition = model.player.currentPosition
                val btnNextEpIsVisible = button_next_ep.isVisible
                val controlsVisible = controller.isVisible

                // make sure remaining time is > 0
                if (model.player.duration > 0) {
                    remainingTime = model.player.duration - currentPosition
                    remainingTime = if (remainingTime < 0) 0 else remainingTime
                }

                // TODO add metaDB ending_start support
                // if remaining time < 20 sec, a next ep is set, autoplay is enabled and not in pip:
                // show next ep button
                if (remainingTime in 1..20000) {
                    if (!btnNextEpIsVisible && hasNextEpisode() && Preferences.autoplay && !isInPiPMode()) {
                        showButtonNextEp()
                    }
                } else if (btnNextEpIsVisible) {
                    hideButtonNextEp()
                }

                // if meta data is present and opening_start & opening_duration are valid, show skip opening
                model.currentEpisodeMeta?.let {
                    if (it.openingDuration > 0 &&
                        currentPosition in it.openingStart..(it.openingStart + 10000) &&
                        !button_skip_op.isVisible
                    ) {
                        showButtonSkipOp()
                    } else if (button_skip_op.isVisible && currentPosition !in it.openingStart..(it.openingStart + 10000)) {
                        // the button should only be visible, if currentEpisodeMeta != null
                        hideButtonSkipOp()
                    }
                }

                // if controls are visible, update them
                if (controlsVisible) {
                    updateControls()
                }
            }
        }
    }

    private fun onPauseOnStop() {
        video_view?.onPause()
        model.player.pause()
        timerUpdates.cancel()
    }

    /**
     * update the custom controls
     */
    private fun updateControls() {
        // update remaining time label
        val hours =  TimeUnit.MILLISECONDS.toHours(remainingTime) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTime) % 60

        // if remaining time is below 60 minutes, don't show hours
        exo_remaining.text = if (TimeUnit.MILLISECONDS.toMinutes(remainingTime) < 60) {
            getString(R.string.time_min_sec, minutes, seconds)
        } else {
            getString(R.string.time_hour_min_sec, hours, minutes, seconds)
        }
    }

    /**
     * update title text and next ep button visibility, set ignoreNextStateEnded
     */
    private fun onMediaChanged() {
        exo_text_title.text = model.getMediaTitle()

        // hide the next episode button, if there is none
        button_next_ep_c.visibility = if (hasNextEpisode()) View.VISIBLE else View.GONE
    }

    /**
     * Check if the current episode has a next episode.
     *
     * @return Boolean: true if there is a next episode, else false.
     */
    private fun hasNextEpisode(): Boolean {
        return (model.currentEpisode.nextEpisodeId != null && !model.currentEpisodeIsLastEpisode())
    }

    /**
     * TODO set position of rewind/fast forward indicators programmatically
     */

    private fun rewind() {
        model.seekToOffset(rwdTime)

        // hide/show needed components
        exo_double_tap_indicator.visibility = View.VISIBLE
        ffwd_10_indicator.visibility = View.INVISIBLE
        rwd_10.visibility = View.INVISIBLE

        rwd_10_indicator.onAnimationEndCallback = {
            exo_double_tap_indicator.visibility = View.GONE
            ffwd_10_indicator.visibility = View.VISIBLE
            rwd_10.visibility = View.VISIBLE
        }

        // run animation
        rwd_10_indicator.runOnClickAnimation()
    }

    private fun fastForward() {
        model.seekToOffset(fwdTime)

        // hide/show needed components
        exo_double_tap_indicator.visibility = View.VISIBLE
        rwd_10_indicator.visibility = View.INVISIBLE
        ffwd_10.visibility = View.INVISIBLE

        ffwd_10_indicator.onAnimationEndCallback = {
            exo_double_tap_indicator.visibility = View.GONE
            rwd_10_indicator.visibility = View.VISIBLE
            ffwd_10.visibility = View.VISIBLE
        }

        // run animation
        ffwd_10_indicator.runOnClickAnimation()
    }

    private fun playNextEpisode() {
        model.playNextEpisode()
        hideButtonNextEp()
    }

    private fun skipOpening() {
        // calculate the seek time
        model.currentEpisodeMeta?.let {
            val seekTime = (it.openingStart + it.openingDuration) - model.player.currentPosition
            model.seekToOffset(seekTime)
        }

    }

    /**
     * show the next episode button
     * TODO improve the show animation
     */
    private fun showButtonNextEp() {
        button_next_ep.isVisible = true
        button_next_ep.alpha = 0.0f

        button_next_ep.animate()
            .alpha(1.0f)
            .setListener(null)
    }

    /**
     * hide the next episode button
     * TODO improve the hide animation
     */
    private fun hideButtonNextEp() {
        button_next_ep.animate()
            .alpha(0.0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    button_next_ep.isVisible = false
                }
            })

    }

    private fun showButtonSkipOp() {
        button_skip_op.isVisible = true
        button_skip_op.alpha = 0.0f

        button_skip_op.animate()
            .alpha(1.0f)
            .setListener(null)
    }

    private fun hideButtonSkipOp() {
        button_skip_op.animate()
            .alpha(0.0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    button_skip_op.isVisible = false
                }
            })

    }

    private fun showEpisodesList() {
        val episodesList = EpisodesListPlayer(this, model = model).apply {
            onViewRemovedAction = { model.player.play() }
        }
        player_layout.addView(episodesList)
        pauseAndHideControls()
    }

    private fun showLanguageSettings() {
        val languageSettings = LanguageSettingsPlayer(this, model = model).apply {
            onViewRemovedAction = { model.player.play() }
        }
        player_layout.addView(languageSettings)
        pauseAndHideControls()
    }

    /**
     * pause playback and hide controls
     */
    private fun pauseAndHideControls() {
        model.player.pause() // showTimeoutMs is set to 0 when calling pause, but why
        controller.showTimeoutMs = defaultShowTimeoutMs // fix showTimeoutMs set to 0
        controller.hide()
    }

    inner class PlayerGestureListener : GestureDetector.SimpleOnGestureListener() {

        /**
         * on single tap hide or show the controls
         */
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            if (!isInPiPMode()) {
                if (controller.isVisible) controller.hide() else controller.show()
            }

            return true
        }

        /**
         * on double tap rewind or forward
         */
        override fun onDoubleTap(e: MotionEvent?): Boolean {
            val eventPosX = e?.x?.toInt() ?: 0
            val viewCenterX = video_view.measuredWidth / 2

            // if the event position is on the left side rewind, if it's on the right forward
            if (eventPosX < viewCenterX) rewind() else fastForward()

            return true
        }

        /**
         * not used
         */
        override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
            return true
        }

        /**
         * on long press toggle pause/play
         */
        override fun onLongPress(e: MotionEvent?) {
            model.togglePausePlay()
        }

    }

}
