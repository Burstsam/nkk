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
import org.mosad.teapod.util.GUIMedia
import org.mosad.teapod.util.StreamMedia

class MediaFragment(private val guiMedia: GUIMedia, private val streamMedia: StreamMedia) : Fragment() {

    private lateinit var adapterRecEpisodes: EpisodesAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // generic gui
        Glide.with(requireContext()).load(guiMedia.posterLink).into(image_poster)
        text_title.text = guiMedia.title
        text_desc.text = guiMedia.shortDesc

        // specific gui
        if (streamMedia.type == MediaType.TVSHOW) {
            val episodes = streamMedia.streams.mapIndexed { index, _ ->
                "${guiMedia.title} - Ep. ${index + 1}"
            }


            adapterRecEpisodes = EpisodesAdapter(episodes)
            viewManager = LinearLayoutManager(context)
            recycler_episodes.layoutManager = viewManager
            recycler_episodes.adapter = adapterRecEpisodes

        } else if (streamMedia.type == MediaType.MOVIE) {
            recycler_episodes.visibility = View.GONE
        }


        println("media streams: ${streamMedia.streams}")

        initActions()
    }

    private fun initActions() {
        button_play.setOnClickListener {
            onClickButtonPlay()
        }

        // set onItemClick only in adapter is initialized
        if (this::adapterRecEpisodes.isInitialized) {
            adapterRecEpisodes.onItemClick = { item, position ->
                playStream(streamMedia.streams[position])
            }
        }
    }

    private fun onClickButtonPlay() {
        when (streamMedia.type) {
            MediaType.MOVIE -> playStream(streamMedia.streams.first())
            MediaType.TVSHOW -> playStream(streamMedia.streams.first())
            MediaType.OTHER -> Log.e(javaClass.name, "Wrong Type, please report this issue.")
        }
    }

    private fun playStream(url: String) {
        val mainActivity = activity as MainActivity
        mainActivity.startPlayer(url)
    }

}