package org.mosad.teapod.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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

        // generic gui
        text_title.text = media.title

        if (tmdb.posterUrl.isNotEmpty()) {
            Glide.with(requireContext()).load(tmdb.posterUrl).into(image_poster)
            text_desc.text = tmdb.overview
            Log.d(javaClass.name, "TMDB data present")
        } else {
            Glide.with(requireContext()).load(media.posterLink).into(image_poster)
            text_desc.text = media.shortDesc
            Log.d(javaClass.name, "No TMDB data present, using Aod")
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


        println("media streams: ${media.episodes}")

        initActions()
    }

    private fun initActions() {
        button_play.setOnClickListener {
            when (media.type) {
                MediaType.MOVIE -> playStream(media.episodes.first().streamUrl)
                MediaType.TVSHOW -> playStream(media.episodes.first().streamUrl)
                MediaType.OTHER -> Log.e(javaClass.name, "Wrong Type, please report this issue.")
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