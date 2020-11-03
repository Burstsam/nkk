package org.mosad.teapod.ui.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.fragment_media.*
import org.mosad.teapod.MainActivity
import org.mosad.teapod.R
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.util.DataTypes.MediaType
import org.mosad.teapod.util.Episode
import org.mosad.teapod.util.Media
import org.mosad.teapod.util.StorageController
import org.mosad.teapod.util.TMDBResponse
import org.mosad.teapod.util.adapter.EpisodeItemAdapter

class MediaFragment(private val media: Media, private val tmdb: TMDBResponse) : Fragment() {

    private lateinit var adapterRecEpisodes: EpisodeItemAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var nextEpisode: Episode

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initGUI()
        initActions()
    }

    /**
     * if tmdb data is present, use it, else use the aod data
     */
    private fun initGUI() {
        // generic gui
        val backdropUrl = if (tmdb.backdropUrl.isNotEmpty()) tmdb.backdropUrl else media.info.posterUrl
        val posterUrl = if (tmdb.posterUrl.isNotEmpty()) tmdb.posterUrl else media.info.posterUrl

        Glide.with(requireContext()).load(backdropUrl)
            .apply(RequestOptions.placeholderOf(ColorDrawable(Color.DKGRAY)))
            .apply(RequestOptions.bitmapTransform(BlurTransformation(20, 3)))
            .into(image_backdrop)

        Glide.with(requireContext()).load(posterUrl)
            .into(image_poster)

        text_title.text = media.info.title
        text_year.text = media.info.year.toString()
        text_age.text = media.info.age.toString()
        text_overview.text = media.info.shortDesc
        if (StorageController.myList.contains(media.id)) {
            Glide.with(requireContext()).load(R.drawable.ic_baseline_check_24).into(image_my_list_action)
        } else {
            Glide.with(requireContext()).load(R.drawable.ic_baseline_add_24).into(image_my_list_action)
        }

        // specific gui
        if (media.type == MediaType.TVSHOW) {
            adapterRecEpisodes = EpisodeItemAdapter(media.episodes)
            viewManager = LinearLayoutManager(context)
            recycler_episodes.layoutManager = viewManager
            recycler_episodes.adapter = adapterRecEpisodes

            text_episodes_or_runtime.text = getString(R.string.text_episodes_count, media.info.episodesCount)

            // get next episode
            nextEpisode = if (media.episodes.firstOrNull{ !it.watched } != null) {
                media.episodes.first{ !it.watched }
            } else {
                media.episodes.first()
            }

            // title is the next episodes title
            text_title.text = nextEpisode.title
        } else if (media.type == MediaType.MOVIE) {
            recycler_episodes.visibility = View.GONE

            if (tmdb.runtime > 0) {
                text_episodes_or_runtime.text = getString(R.string.text_runtime, tmdb.runtime)
            } else {
                text_episodes_or_runtime.visibility = View.GONE
            }
        }
    }

    private fun initActions() {
        button_play.setOnClickListener {
            when (media.type) {
                MediaType.MOVIE -> playStream(media.episodes.first())
                MediaType.TVSHOW -> playEpisode(nextEpisode)
                else -> Log.e(javaClass.name, "Wrong Type: $media.type")
            }
        }

        // add or remove media from myList
        linear_my_list_action.setOnClickListener {
            if (StorageController.myList.contains(media.id)) {
                StorageController.myList.remove(media.id)
                Glide.with(requireContext()).load(R.drawable.ic_baseline_add_24).into(image_my_list_action)
            } else {
                StorageController.myList.add(media.id)
                Glide.with(requireContext()).load(R.drawable.ic_baseline_check_24).into(image_my_list_action)
            }
            StorageController.saveMyList(requireContext())

            // notify home fragment on change
            parentFragmentManager.findFragmentByTag("HomeFragment")?.let {
                (it as HomeFragment).updateMyListMedia()
            }
        }

        // set onItemClick only in adapter is initialized
        if (this::adapterRecEpisodes.isInitialized) {
            adapterRecEpisodes.onImageClick = { _, position ->
                playEpisode(media.episodes[position])
            }
        }
    }

    private fun playEpisode(ep: Episode) {
        playStream(ep)

        // update watched state
        AoDParser.sendCallback(ep.watchedCallback)
        adapterRecEpisodes.updateWatchedState(true, media.episodes.indexOf(ep))
        adapterRecEpisodes.notifyDataSetChanged()

        // update nextEpisode
        nextEpisode = if (media.episodes.firstOrNull{ !it.watched } != null) {
            media.episodes.first{ !it.watched }
        } else {
            media.episodes.first()
        }
        text_title.text = nextEpisode.title
    }

    /**
     * Play the media's stream
     * If prefer secondary or primary is empty and secondary is present (secStreamOmU),
     * use the secondary stream. Else, if the primary stream is set use the primary stream.
     */
    private fun playStream(ep: Episode) {
        val streamUrl = if ((Preferences.preferSecondary || ep.priStreamUrl.isEmpty()) && ep.secStreamOmU) {
            ep.secStreamUrl
        } else if (ep.priStreamUrl.isNotEmpty()) {
            ep.priStreamUrl
        } else {
            Log.e(javaClass.name, "No stream url set.")
            ""
        }

        Log.d(javaClass.name, "Playing stream: $streamUrl")
        (activity as MainActivity).startPlayer(streamUrl)
    }

}