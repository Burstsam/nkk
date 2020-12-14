package org.mosad.teapod.ui.components

import android.view.LayoutInflater
import android.view.ViewGroup
import org.mosad.teapod.databinding.PlayerEpisodesBinding
import org.mosad.teapod.player.PlayerViewModel
import org.mosad.teapod.util.adapter.PlayerEpisodeItemAdapter

/**
 * TODO toggle play/pause on close/open
 * TODO play selected episode
 * TODO scroll to current episode
 */
class EpisodesPlayer(val parent: ViewGroup, private val model: PlayerViewModel) {

    private val binding = PlayerEpisodesBinding.inflate(LayoutInflater.from(parent.context), parent, true)
    private var adapterRecEpisodes = PlayerEpisodeItemAdapter(model.media.episodes)

    init {
        binding.recyclerEpisodesPlayer.adapter = adapterRecEpisodes

        initActions()
    }

    private fun initActions() {
        binding.buttonCloseEpisodesList.setOnClickListener {
            parent.removeView(binding.root)
        }

        adapterRecEpisodes.onImageClick = { _, position ->
            println(model.media.episodes[position])
            //playEpisode(media.episodes[position])
        }
    }


}