package org.mosad.teapod.ui.activity.main.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.mosad.teapod.ui.activity.main.MainActivity
import org.mosad.teapod.ui.activity.main.viewmodel.MediaFragmentViewModel
import org.mosad.teapod.databinding.FragmentMediaEpisodesBinding
import org.mosad.teapod.util.Episode
import org.mosad.teapod.util.adapter.EpisodeItemAdapter

class MediaFragmentEpisodes : Fragment() {

    private lateinit var binding: FragmentMediaEpisodesBinding
    private lateinit var adapterRecEpisodes: EpisodeItemAdapter

    private val model: MediaFragmentViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMediaEpisodesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapterRecEpisodes = EpisodeItemAdapter(model.media.episodes, model.tmdbTVSeason?.episodes)
        binding.recyclerEpisodes.adapter = adapterRecEpisodes

        // set onItemClick only in adapter is initialized
        if (this::adapterRecEpisodes.isInitialized) {
            adapterRecEpisodes.onImageClick = { _, position ->
                playEpisode(model.media.episodes[position])
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // if adapterRecEpisodes is initialized, update the watched state for the episodes
        if (this::adapterRecEpisodes.isInitialized) {
            model.media.episodes.forEachIndexed { index, episode ->
                adapterRecEpisodes.updateWatchedState(episode.watched, index)
            }
            adapterRecEpisodes.notifyDataSetChanged()
        }
    }

    private fun playEpisode(ep: Episode) {
        (activity as MainActivity).startPlayer(model.media.id, ep.id)
        Log.d(javaClass.name, "Started Player with  episodeId: ${ep.id}")

        model.updateNextEpisode(ep) // set the correct next episode
    }

}