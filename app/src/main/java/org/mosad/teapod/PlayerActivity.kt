package org.mosad.teapod

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
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
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.util.DataTypes.MediaType
import org.mosad.teapod.util.Episode
import org.mosad.teapod.util.Media
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity() {

    private lateinit var player: SimpleExoPlayer
    private lateinit var dataSourceFactory: DataSource.Factory
    private lateinit var controller: StyledPlayerControlView
    private lateinit var gestureDetector: GestureDetectorCompat

    private var streamUrl = ""

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
    }

    private fun initMedia() {
        media = AoDParser.getMediaById(mediaId)
        currentEpisode = media.episodes.first { it.id == episodeId }
        streamUrlFromEp(currentEpisode) // get current stream

        // get next episode if present
        val nextEpIndex = media.episodes.indexOfFirst { it.id == episodeId } + 1
        if (nextEpIndex < (media.episodes.size - 1)) {
            println("has next episode")
            nextEpisode = media.episodes[nextEpIndex]
        }
    }

    private fun initExoPlayer() {
        player = SimpleExoPlayer.Builder(this).build()
        dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "Teapod"))
        controller = video_view.findViewById(R.id.exo_controller)

        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(streamUrl)))

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
        exo_rew_10.setOnClickListener { rewind() }
        exo_ffwd_10.setOnClickListener { forward() }
    }

    private fun releasePlayer(){
        playbackPosition = player.currentPosition
        currentWindow = player.currentWindowIndex
        playWhenReady = player.playWhenReady
        player.release()

        Log.d(javaClass.name, "Released player")
    }

    private fun rewind() {
        player.seekTo(player.currentPosition - rwdTime)
    }

    private fun forward() {
        player.seekTo(player.currentPosition + fwdTime)
    }

    @Suppress("unused")
    private fun playNextEpisode() {
        nextEpisode?.let { streamUrlFromEp(it) }
        // TODO play
        // TODO set next episode if present
    }

    /**
     * If preferSecondary or priStreamUrl is empty and secondary is present (secStreamOmU),
     * use the secondary stream. Else, if the primary stream is set use the primary stream.
     * If no stream is present, close the activity.
     */
    private fun streamUrlFromEp(episode: Episode) {
        streamUrl = if ((Preferences.preferSecondary || episode.priStreamUrl.isEmpty()) && episode.secStreamOmU) {
            episode.secStreamUrl
        } else if (episode.priStreamUrl.isNotEmpty()) {
            episode.priStreamUrl
        } else {
            Log.e(javaClass.name, "No stream url set.")
            this.finish()
            return
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
            val eventPos = e?.x?.toInt() ?: 0
            val viewCenter = video_view.measuredWidth / 2

            // TODO show indicator for tap action
            // if the event position is on the left side rewind, if it's on the right forward
            if (eventPos < viewCenter) {
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

    }

}
