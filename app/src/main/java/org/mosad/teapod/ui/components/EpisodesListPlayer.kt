package org.mosad.teapod.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import org.mosad.teapod.databinding.PlayerEpisodesListBinding
import org.mosad.teapod.player.PlayerViewModel
import org.mosad.teapod.util.adapter.PlayerEpisodeItemAdapter

class EpisodesListPlayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    model: PlayerViewModel? = null
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = PlayerEpisodesListBinding.inflate(LayoutInflater.from(context), this, true)
    private lateinit var adapterRecEpisodes: PlayerEpisodeItemAdapter

    var onViewRemovedAction: (() -> Unit)? = null // TODO find a better solution for this

    init {
        binding.buttonCloseEpisodesList.setOnClickListener {
            (this.parent as ViewGroup).removeView(this)
            onViewRemovedAction?.invoke()
        }

        model?.let {
            adapterRecEpisodes = PlayerEpisodeItemAdapter(it.media.episodes)

            adapterRecEpisodes.onImageClick = { _, position ->
                (this.parent as ViewGroup).removeView(this)
                it.currentEpisode = it.media.episodes[position]
            }

            binding.recyclerEpisodesPlayer.adapter = adapterRecEpisodes
            binding.recyclerEpisodesPlayer.scrollToPosition(it.currentEpisode.number - 1) // number != index
        }
    }

}