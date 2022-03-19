package org.mosad.teapod.util.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import org.mosad.teapod.R
import org.mosad.teapod.databinding.ItemEpisodePlayerBinding
import org.mosad.teapod.parser.crunchyroll.Episodes
import org.mosad.teapod.util.tmdb.TMDBTVEpisode

class PlayerEpisodeItemAdapter(private val episodes: Episodes, private val tmdbEpisodes: List<TMDBTVEpisode>?) : RecyclerView.Adapter<PlayerEpisodeItemAdapter.EpisodeViewHolder>() {

    var onImageClick: ((seasonId: String, episodeId: String) -> Unit)? = null
    var currentSelected: Int = -1 // -1, since position should never be < 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        return EpisodeViewHolder(ItemEpisodePlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val context = holder.binding.root.context
        val ep = episodes.items[position]

        val titleText = if (ep.episodeNumber != null) {
            // for tv shows add ep prefix and episode number
            if (ep.isDubbed) {
                context.getString(R.string.component_episode_title, ep.episode, ep.title)
            } else {
                context.getString(R.string.component_episode_title_sub, ep.episode, ep.title)
            }
        } else {
            ep.title
        }

        holder.binding.textEpisodeTitle2.text = titleText
        holder.binding.textEpisodeDesc2.text = if (ep.description.isNotEmpty()) {
            ep.description
        } else if (tmdbEpisodes != null && position < tmdbEpisodes.size){
            tmdbEpisodes[position].overview
        } else {
            ""
        }

        if (ep.images.thumbnail[0][0].source.isNotEmpty()) {
            Glide.with(context).load(ep.images.thumbnail[0][0].source)
                .apply(RequestOptions.bitmapTransform(RoundedCornersTransformation(10, 0)))
                .into(holder.binding.imageEpisode)
        }

        // hide the play icon, if it's the current episode
        holder.binding.imageEpisodePlay.visibility = if (currentSelected == position) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return episodes.items.size
    }

    inner class EpisodeViewHolder(val binding: ItemEpisodePlayerBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.imageEpisode.setOnClickListener {
                // don't execute, if it's the current episode
                if (currentSelected != bindingAdapterPosition) {
                    onImageClick?.invoke(
                        episodes.items[bindingAdapterPosition].seasonId,
                        episodes.items[bindingAdapterPosition].id
                    )
                }
            }
        }
    }
}