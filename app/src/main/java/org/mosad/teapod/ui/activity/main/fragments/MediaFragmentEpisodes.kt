package org.mosad.teapod.ui.activity.main.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.mosad.teapod.databinding.FragmentMediaEpisodesBinding
import org.mosad.teapod.ui.activity.main.MainActivity
import org.mosad.teapod.ui.activity.main.viewmodel.MediaFragmentViewModel
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

        adapterRecEpisodes = EpisodeItemAdapter(model.currentEpisodesCrunchy, model.tmdbTVSeason.episodes)
        binding.recyclerEpisodes.adapter = adapterRecEpisodes

        // set onItemClick, adapter is initialized
        adapterRecEpisodes.onImageClick = { seasonId, episodeId ->
            playEpisode(seasonId, episodeId)
        }

        // don't show season selection if only one season is present
        if (model.seasonsCrunchy.total < 2) {
            binding.buttonSeasonSelection.visibility = View.GONE
        } else {
            binding.buttonSeasonSelection.text = model.currentSeasonCrunchy.title
            binding.buttonSeasonSelection.setOnClickListener { v ->
                showSeasonSelection(v)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // if adapterRecEpisodes is initialized, update the watched state for the episodes
        if (this::adapterRecEpisodes.isInitialized) {
            // TODO reimplement, if needed
//            model.media.playlist.forEachIndexed { index, episodeInfo ->
//                adapterRecEpisodes.updateWatchedState(episodeInfo.watched, index)
//            }
//            adapterRecEpisodes.notifyDataSetChanged()
        }
    }

    private fun showSeasonSelection(v: View) {
        // TODO replace with Exposed dropdown menu: https://material.io/components/menus/android#exposed-dropdown-menus
        val popup = PopupMenu(requireContext(), v)
        model.seasonsCrunchy.items.forEach { season ->
            popup.menu.add(season.title).also {
                it.setOnMenuItemClickListener {
                    onSeasonSelected(season.id)
                    false
                }
            }
        }

        popup.show()
    }

    /**
     * Call model to load a new season.
     * Once loaded update buttonSeasonSelection text and adapterRecEpisodes.
     *
     * Suppress waring since invalid.
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun onSeasonSelected(seasonId: String) {
        // load the new season
        lifecycleScope.launch {
            model.setCurrentSeason(seasonId)
            binding.buttonSeasonSelection.text = model.currentSeasonCrunchy.title
            adapterRecEpisodes.notifyDataSetChanged()
        }
    }

    private fun playEpisode(seasonId: String, episodeId: String) {
        (activity as MainActivity).startPlayer(seasonId, episodeId)
        Log.d(javaClass.name, "Started Player with  episodeId: $episodeId")

        //model.updateNextEpisode(episodeId) // set the correct next episode
    }

}