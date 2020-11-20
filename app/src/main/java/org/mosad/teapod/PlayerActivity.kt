package org.mosad.teapod

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
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
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.ui.fragments.MediaFragment
import org.mosad.teapod.util.DataTypes.MediaType
import org.mosad.teapod.util.Episode
import org.mosad.teapod.util.Media
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.scheduleAtFixedRate

class PlayerActivity : AppCompatActivity() {

    private lateinit var player: SimpleExoPlayer
    private lateinit var dataSourceFactory: DataSource.Factory
    private lateinit var controller: StyledPlayerControlView
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var timerUpdates: TimerTask

    private var mediaId = 0
    private var episodeId = 0

    private var media: Media = Media(0, "", MediaType.OTHER)
    private var currentEpisode = Episode()
    private var nextEpisode: Episode? = null

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

        mediaId = intent.getIntExtra(getString(R.string.intent_media_id), 0)
        episodeId = intent.getIntExtra(getString(R.string.intent_episode_id), 0)

        gestureDetector = GestureDetectorCompat(this, PlayerGestureListener())

        initActions()
    }


    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initPlayer()
            if (video_view != null) video_view.onResume()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23) {
            initPlayer()
            if (video_view != null) video_view.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            if (video_view != null) video_view.onPause()
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
        if (mediaId <= 0) {
            Log.e(javaClass.name, "No media id was set.")
            this.finish()
        }

        initMedia()
        initExoPlayer()
        initVideoView()
        initController()
        initTimeUpdates()
    }

    private fun initMedia() {
        media = AoDParser.getMediaById(mediaId)
        currentEpisode = media.episodes.first { it.id == episodeId }
        nextEpisode = selectNextEpisode()
    }

    private fun initExoPlayer() {
        player = SimpleExoPlayer.Builder(this).build()
        dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "Teapod"))
        controller = video_view.findViewById(R.id.exo_controller)

        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(selectStream(currentEpisode))))

        player.playWhenReady = playWhenReady
        player.setMediaSource(mediaSource)
        player.seekTo(playbackPosition)
        player.prepare()

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

                if (state == ExoPlayer.STATE_ENDED && nextEpisode != null && Preferences.autoplay) {
                    playNextEpisode()
                }

            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initVideoView() {
        video_view.player = player

        // when the player controls get hidden, hide the bars too
        video_view.setControllerVisibilityListener {
            if (it == View.GONE) {
                hideBars()
            }
        }

        video_view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun initController() {
        controller.isAnimationEnabled = false // disable controls (time-bar) animation
        controller.setProgressUpdateListener { _, _ ->
            remainingTime = player.duration - player.currentPosition

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

        exo_text_title.text = currentEpisode.title // set media title
    }

    private fun initActions() {
        exo_close_player.setOnClickListener { this.finish() }
        rwd_10.setOnButtonClickListener { rewind() }
        ffwd_10.setOnButtonClickListener { forward() }
        button_next_ep.setOnClickListener { playNextEpisode() }
    }

    private fun initTimeUpdates() {
        if (this::timerUpdates.isInitialized) {
            timerUpdates.cancel()
        }

        timerUpdates = Timer().scheduleAtFixedRate(0, 1000) {
            GlobalScope.launch {
                var btnNextEpIsVisible: Boolean
                var remainingTime: Long

                withContext(Dispatchers.Main) {
                    btnNextEpIsVisible = button_next_ep.isVisible
                    remainingTime = player.duration - player.currentPosition
                }

                if (remainingTime in 0..20000) {
                    if (!btnNextEpIsVisible && nextEpisode != null && Preferences.autoplay) {
                        // if the next ep button is not visible, make it visible
                        withContext(Dispatchers.Main) { showButtonNextEp() }
                    }
                } else {
                    if (btnNextEpIsVisible) {
                        withContext(Dispatchers.Main) { hideButtonNextEp() } 
                    }
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
     * TODO set position of rewind/fast forward indicators programmatically
     */

    private fun rewind() {
        player.seekTo(player.currentPosition - rwdTime)

        // hide/show needed components
        exo_double_tap_indicator.visibility = View.VISIBLE
        ffwd_10_indicator.visibility = View.INVISIBLE
        ffwd_10.visibility = View.INVISIBLE

        rwd_10_indicator.onAnimationEndCallback = {
            exo_double_tap_indicator.visibility = View.GONE
            ffwd_10_indicator.visibility = View.VISIBLE
            ffwd_10.visibility = View.VISIBLE
        }

        // run animation
        rwd_10_indicator.runOnClickAnimation()
    }

    private fun forward() {
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

    private fun playNextEpisode() {
        nextEpisode?.let { nextEp ->


            // update the gui
            exo_text_title.text = nextEp.title
            hideButtonNextEp()

            player.clearMediaItems() //remove previous item
            val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(selectStream(nextEp))))
            player.setMediaSource(mediaSource)
            player.prepare()

            // watchedCallback for next ep
            currentEpisode = nextEp // set current ep to next ep
            episodeId = nextEp.id
            MediaFragment.instance.updateWatchedState(nextEp)

            nextEpisode = selectNextEpisode()
        }
    }

    /**
     * If preferSecondary or priStreamUrl is empty and secondary is present (secStreamOmU),
     * use the secondary stream. Else, if the primary stream is set use the primary stream.
     * If no stream is present, close the activity.
     */
    private fun selectStream(episode: Episode): String {
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
                forward()
            }

            return true
        }

        /**
         * not used
         */
        override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
            return true
        }

        override fun onLongPress(e: MotionEvent?) {
            togglePausePlay()
        }

    }

}
