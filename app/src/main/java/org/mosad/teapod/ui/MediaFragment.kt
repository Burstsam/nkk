package org.mosad.teapod.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
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
import org.mosad.teapod.util.DataTypes.MediaType
import org.mosad.teapod.util.EpisodesAdapter
import org.mosad.teapod.util.Media
import org.mosad.teapod.util.TMDBResponse

class MediaFragment(private val media: Media, private val tmdb: TMDBResponse) : Fragment() {

    private lateinit var adapterRecEpisodes: EpisodesAdapter
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
        text_title.text = media.title

        if (tmdb.posterUrl.isNotEmpty()) {
            Glide.with(requireContext()).load(tmdb.backdropUrl)
                .apply(RequestOptions.placeholderOf(ColorDrawable(Color.DKGRAY)))
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3)))
                .into(image_backdrop)
        } else {
            Glide.with(requireContext()).load(ColorDrawable(Color.DKGRAY)).into(image_poster)
        }

        if (tmdb.posterUrl.isNotEmpty()) {
            Glide.with(requireContext()).load(tmdb.posterUrl)
                .into(image_poster)
        } else {
            Glide.with(requireContext()).load(media.posterLink)
                .into(image_poster)
        }

        text_overview.text = if (tmdb.overview.isNotEmpty()) {
            tmdb.overview
        } else {
            media.shortDesc
        }

        // specific gui
        if (media.type == MediaType.TVSHOW) {
            val episodeTitles = media.episodes.map { it.title }

            adapterRecEpisodes = EpisodesAdapter(episodeTitles)
            viewManager = LinearLayoutManager(context)
            recycler_episodes.layoutManager = viewManager
            recycler_episodes.adapter = adapterRecEpisodes

        } else if (media.type == MediaType.MOVIE) {
            recycler_episodes.visibility = View.GONE
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

        // set onItemClick only in adapter is initialized
        if (this::adapterRecEpisodes.isInitialized) {
            adapterRecEpisodes.onItemClick = { item, position ->
                playStream(media.episodes[position].streamUrl)
            }
        }
    }

    private fun playStream(url: String) {
        val mainActivity = activity as MainActivity
        mainActivity.startPlayer(url)
    }

}