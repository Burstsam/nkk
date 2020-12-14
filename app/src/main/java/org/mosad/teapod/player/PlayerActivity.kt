package org.mosad.teapod.player

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.player_controls.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mosad.teapod.R
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.ui.components.EpisodesPlayer
import org.mosad.teapod.util.DataTypes
import org.mosad.teapod.util.Episode
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.scheduleAtFixedRate

class PlayerActivity : AppCompatActivity() {

    private val model: PlayerViewModel by viewModels()

    private lateinit var player: SimpleExoPlayer
    private lateinit var dataSourceFactory: DataSource.Factory
    private lateinit var controller: StyledPlayerControlView
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var timerUpdates: TimerTask

    private var nextEpManually = false
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var remainingTime: Long = 0

    private val rwdTime = 10000
    private val fwdTime = 10000

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

        gestureDetector = GestureDetectorCompat(this, PlayerGestureListener())

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
        if (model.mediaId <= 0) {
            Log.e(javaClass.name, "No media id was set.")
            this.finish()
        }

        initExoPlayer()
        initVideoView()
        initTimeUpdates()
    }

    private fun initExoPlayer() {
        player = SimpleExoPlayer.Builder(this).build()
        dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "Teapod"))
        controller = video_view.findViewById(R.id.exo_controller)

        controller.isAnimationEnabled = false // disable controls (time-bar) animation

        player.playWhenReady = playWhenReady
        player.addListener(object : Player.EventListener {
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
                    // if next episode btn was clicked, skipp playNextEpisode() on STATE_ENDED
                    if (nextEpManually) {
                        nextEpManually = false
                    } else {
                        playNextEpisode()
                    }
                }
            }
        })

        playCurrentMedia(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initVideoView() {
        video_view.player = player

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
        button_next_ep_c.setOnClickListener { playNextEpisode() }
        button_episodes.setOnClickListener { showEpisodesList() }
    }

    private fun initTimeUpdates() {
        if (this::timerUpdates.isInitialized) {
            timerUpdates.cancel()
        }

        timerUpdates = Timer().scheduleAtFixedRate(0, 500) {
            GlobalScope.launch {
                var btnNextEpIsVisible: Boolean
                var controlsVisible: Boolean

                withContext(Dispatchers.Main) {
                    remainingTime = player.duration - player.currentPosition
                    remainingTime = if (remainingTime < 0) 0 else remainingTime

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
        playbackPosition = player.currentPosition
        currentWindow = player.currentWindowIndex
        playWhenReady = player.playWhenReady
        player.release()
        timerUpdates.cancel()

        Log.d(javaClass.name, "Released player")
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
     * TODO set position of rewind/fast forward indicators programmatically
     */

    private fun rewind() {
        player.seekTo(player.currentPosition - rwdTime)

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
        player.seekTo(player.currentPosition + fwdTime)

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

    private fun togglePausePlay() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    private fun playNextEpisode() = model.nextEpisode?.let {
        model.nextEpisode() // current = next, next = new or null
        hideButtonNextEp()

        nextEpManually = true
        playCurrentMedia(false)
    }

    /**
     * start playing a episode
     * Note: movies are episodes too!
     */
    private fun playCurrentMedia(seekToPosition: Boolean) {
        // update the gui
        exo_text_title.text = if (model.media.type == DataTypes.MediaType.TVSHOW) {
            getString(
                R.string.component_episode_title,
                model.currentEpisode.number,
                model.currentEpisode.description
            )
        } else {
            model.currentEpisode.title
        }

        if (model.nextEpisode == null) {
            button_next_ep_c.visibility = View.GONE
        }

        player.clearMediaItems() //remove previous item
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(
            MediaItem.fromUri(Uri.parse(autoSelectStream(model.currentEpisode)))
        )
        if (seekToPosition) player.seekTo(playbackPosition)
        player.setMediaSource(mediaSource)
        player.prepare()
    }

    /**
     * If preferSecondary or priStreamUrl is empty and secondary is present (secStreamOmU),
     * use the secondary stream. Else, if the primary stream is set use the primary stream.
     * If no stream is present, close the activity.
     */
    private fun autoSelectStream(episode: Episode): String {
        return if ((Preferences.preferSecondary || episode.priStreamUrl.isEmpty()) && episode.secStreamOmU) {
            episode.secStreamUrl
        } else if (episode.priStreamUrl.isNotEmpty()) {
            episode.priStreamUrl
        } else {
            Log.e(javaClass.name, "No stream url set.")
            this.finish()
            ""
        }
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
        val rootView = window.decorView.rootView as ViewGroup
        EpisodesPlayer(rootView, model)
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
            if (eventPosX < viewCenterX) {
                rewind()
            } else {
                fastForward()
            }

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
            togglePausePlay()
        }

    }

}
