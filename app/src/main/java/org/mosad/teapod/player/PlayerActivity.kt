package org.mosad.teapod.player

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.player_controls.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mosad.teapod.R
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.ui.components.EpisodesListPlayer
import org.mosad.teapod.ui.components.LanguageSettingsPlayer
import org.mosad.teapod.util.DataTypes
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.scheduleAtFixedRate

class PlayerActivity : AppCompatActivity() {

    private val model: PlayerViewModel by viewModels()

    private lateinit var controller: StyledPlayerControlView
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var timerUpdates: TimerTask

    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var remainingTime: Long = 0

    private val rwdTime: Long = 10000.unaryMinus()
    private val fwdTime: Long = 10000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        hideBars() // Initial hide the bars

        savedInstanceState?.let {
            currentWindow = it.getInt(getString(R.string.state_resume_window))
            playbackPosition = it.getLong(getString(R.string.state_resume_position))
            playWhenReady = it.getBoolean(getString(R.string.state_is_playing))
        }

        model.loadMedia(
            intent.getIntExtra(getString(R.string.intent_media_id), 0),
            intent.getIntExtra(getString(R.string.intent_episode_id), 0)
        )
        model.currentEpisodeChangedListener.add { onMediaChanged() }
        gestureDetector = GestureDetectorCompat(this, PlayerGestureListener())

        initGUI()
        initActions()
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initPlayer()
            video_view?.onResume()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23) {
            initPlayer()
            video_view?.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            video_view?.onPause()
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            video_view?.onPause()
            releasePlayer()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(getString(R.string.state_resume_window), currentWindow)
        outState.putLong(getString(R.string.state_resume_position), playbackPosition)
        outState.putBoolean(getString(R.string.state_is_playing), playWhenReady)
        super.onSaveInstanceState(outState)
    }

    private fun initPlayer() {
        if (model.media.id < 0) {
            Log.e(javaClass.name, "No media was set.")
            this.finish()
        }

        initExoPlayer()
        initVideoView()
        initTimeUpdates()
    }

    // TODO if view model was not destroyed, don't start a new media
    private fun initExoPlayer() {
        controller = video_view.findViewById(R.id.exo_controller)
        controller.isAnimationEnabled = false // disable controls (time-bar) animation

        model.player.playWhenReady = playWhenReady
        model.player.addListener(object : Player.EventListener {
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

                if (state == ExoPlayer.STATE_ENDED && model.nextEpisode != null && Preferences.autoplay) {
                    playNextEpisode()
                }
            }
        })

        // start playing the current episode, after all needed player components have been initialized
        model.playEpisode(model.currentEpisode, true, playbackPosition)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initVideoView() {
        video_view.player = model.player

        // when the player controls get hidden, hide the bars too
        video_view.setControllerVisibilityListener {
            when (it) {
                View.GONE -> hideBars()
                View.VISIBLE -> updateControls()
            }
        }

        video_view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun initActions() {
        exo_close_player.setOnClickListener { this.finish() }
        rwd_10.setOnButtonClickListener { rewind() }
        ffwd_10.setOnButtonClickListener { fastForward() }
        button_next_ep.setOnClickListener { playNextEpisode() }
        button_language.setOnClickListener { showLanguageSettings() }
        button_episodes.setOnClickListener { showEpisodesList() }
        button_next_ep_c.setOnClickListener { playNextEpisode() }
    }

    private fun initGUI() {
        if (model.media.type == DataTypes.MediaType.MOVIE) {
            button_episodes.visibility = View.GONE
        }
    }

    private fun initTimeUpdates() {
        if (this::timerUpdates.isInitialized) {
            timerUpdates.cancel()
        }

        timerUpdates = Timer().scheduleAtFixedRate(0, 500) {
            GlobalScope.launch {
                var btnNextEpIsVisible: Boolean
                var controlsVisible: Boolean

                remainingTime = model.player.duration - model.player.currentPosition
                remainingTime = if (remainingTime < 0) 0 else remainingTime

                withContext(Dispatchers.Main) {
                    btnNextEpIsVisible = button_next_ep.isVisible
                    controlsVisible = controller.isVisible
                }

                if (remainingTime in 1..20000) {
                    // if the next ep button is not visible, make it visible
                    if (!btnNextEpIsVisible && model.nextEpisode != null && Preferences.autoplay) {
                        withContext(Dispatchers.Main) { showButtonNextEp() }
                    }
                } else if (btnNextEpIsVisible) {
                    withContext(Dispatchers.Main) { hideButtonNextEp() }
                }

                // if controls are visible, update them
                if (controlsVisible) {
                    withContext(Dispatchers.Main) { updateControls() }
                }
            }
        }
    }

    private fun releasePlayer(){
        playbackPosition = model.player.currentPosition
        currentWindow = model.player.currentWindowIndex
        playWhenReady = model.player.playWhenReady
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

        if (model.nextEpisode == null) {
            button_next_ep_c.visibility = View.GONE
        }
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

    /**
     * hide the status and navigation bar
     */
    private fun hideBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_SWIPE
            }
        } else {
            @Suppress("deprecation")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    /**
     * show the next episode button
     * TODO improve the show animation
     */
    private fun showButtonNextEp() {
        button_next_ep.visibility = View.VISIBLE
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
                    button_next_ep.visibility = View.GONE
                }
            })

    }

    private fun showEpisodesList() {
        val episodesList = EpisodesListPlayer(this, model = model).apply {
            onViewRemovedAction = { model.player.play() }
        }
        player_layout.addView(episodesList)

        // hide player controls and pause playback
        model.player.pause()
        controller.hide()
    }

    private fun showLanguageSettings() {
        val languageSettings = LanguageSettingsPlayer(this, model = model).apply {
            onViewRemovedAction = { model.player.play() }
        }
        player_layout.addView(languageSettings)

        // hide player controls and pause playback
        model.player.pause()
        controller.hideImmediately()
    }

    inner class PlayerGestureListener : GestureDetector.SimpleOnGestureListener() {

        /**
         * on single tap hide or show the controls
         */
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            if (controller.isVisible) controller.hide() else  controller.show()
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
