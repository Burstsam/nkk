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
import org.mosad.teapod.util.DataTypes.MediaType
import org.mosad.teapod.util.Media
import org.mosad.teapod.util.StorageController
import org.mosad.teapod.util.TMDBResponse
import org.mosad.teapod.util.adapter.EpisodeItemAdapter

class MediaFragment(private val media: Media, private val tmdb: TMDBResponse) : Fragment() {

    private lateinit var adapterRecEpisodes: EpisodeItemAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager


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
                MediaType.MOVIE -> playStream(media.episodes.first().priStreamUrl)
                MediaType.TVSHOW -> playStream(media.episodes.first().priStreamUrl)
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
                // TODO add option to prefer secondary stream
                // try to use secondary stream if primary is missing
                if (media.episodes[position].priStreamUrl.isNotEmpty()) {
                    playStream(media.episodes[position].priStreamUrl)
                } else if (media.episodes[position].secStreamUrl.isNotEmpty()) {
                    playStream(media.episodes[position].secStreamUrl)
                }

                // update watched state
                AoDParser.sendCallback(media.episodes[position].watchedCallback)
                adapterRecEpisodes.updateWatchedState(true, position)
                adapterRecEpisodes.notifyDataSetChanged()
            }
        }
    }

    private fun playStream(url: String) {
        Log.d(javaClass.name, "Playing stream: $url")
        (activity as MainActivity).startPlayer(url)
    }

}