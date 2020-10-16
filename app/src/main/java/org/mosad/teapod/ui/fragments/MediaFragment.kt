package org.mosad.teapod.ui.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.fragment_media.*
import org.mosad.teapod.MainActivity
import org.mosad.teapod.R
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.StorageController
import org.mosad.teapod.util.DataTypes.MediaType
import org.mosad.teapod.util.adapter.EpisodeItemAdapter
import org.mosad.teapod.util.Media
import org.mosad.teapod.util.TMDBResponse

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
        val backdropUrl = if (tmdb.backdropUrl.isNotEmpty()) tmdb.backdropUrl else media.info.posterLink
        val posterUrl = if (tmdb.posterUrl.isNotEmpty()) tmdb.posterUrl else media.info.posterLink

        Glide.with(requireContext()).load(backdropUrl)
            .apply(RequestOptions.placeholderOf(ColorDrawable(Color.DKGRAY)))
            .apply(RequestOptions.bitmapTransform(BlurTransformation(20, 3)))
            .into(image_backdrop)

        Glide.with(requireContext()).load(posterUrl)
            .into(image_poster)

        text_title.text = media.title
        text_year.text = media.info.year.toString()
        text_age.text = media.info.age.toString()
        text_overview.text = media.info.shortDesc
        check_my_list.isChecked = StorageController.myList.contains(media.link)

        // specific gui
        if (media.type == MediaType.TVSHOW) {
            adapterRecEpisodes = EpisodeItemAdapter(media.episodes, requireContext())
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
                MediaType.MOVIE -> playStream(media.episodes.first().streamUrl)
                MediaType.TVSHOW -> playStream(media.episodes.first().streamUrl)
                else -> Log.e(javaClass.name, "Wrong Type: $media.type")
            }
        }

        // add or remove media from myList
        check_my_list.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                StorageController.myList.add(media.link)
            } else {
                StorageController.myList.remove(media.link)
            }
            StorageController.saveMyList(requireContext())

            // TODO notify home fragment on change
            parentFragmentManager.findFragmentByTag("HomeFragment")?.let {
                (it as HomeFragment).updateMyListMedia()
            }
        }

        // set onItemClick only in adapter is initialized
        if (this::adapterRecEpisodes.isInitialized) {
            adapterRecEpisodes.onImageClick = { _, position ->
                playStream(media.episodes[position].streamUrl)

                // update watched state
                AoDParser().sendCallback(media.episodes[position].watchedCallback)
                adapterRecEpisodes.updateWatchedState(true, position)
                adapterRecEpisodes.notifyDataSetChanged()
            }
        }
    }

    private fun playStream(url: String) {
        val mainActivity = activity as MainActivity

        println("url is: $url")

        mainActivity.startPlayer(url)
    }

}