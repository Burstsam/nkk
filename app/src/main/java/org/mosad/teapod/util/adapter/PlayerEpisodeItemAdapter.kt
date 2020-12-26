package org.mosad.teapod.util.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import org.mosad.teapod.R
import org.mosad.teapod.databinding.ItemEpisodePlayerBinding
import org.mosad.teapod.util.Episode

class PlayerEpisodeItemAdapter(private val episodes: List<Episode>) : RecyclerView.Adapter<PlayerEpisodeItemAdapter.EpisodeViewHolder>() {

    var onImageClick: ((String, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        return EpisodeViewHolder(ItemEpisodePlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val context = holder.binding.root.context
        val ep = episodes[position]

        val titleText = if (ep.hasDub()) {
            context.getString(R.string.component_episode_title, ep.number, ep.description)
        } else {
            context.getString(R.string.component_episode_title_sub, ep.number, ep.description)
        }

        holder.binding.textEpisodeTitle2.text = titleText
        holder.binding.textEpisodeDesc2.text = ep.shortDesc

        if (episodes[position].posterUrl.isNotEmpty()) {
            Glide.with(context).load(ep.posterUrl)
                .apply(RequestOptions.bitmapTransform(RoundedCornersTransformation(10, 0)))
                .into(holder.binding.imageEpisode)
        }
    }

    override fun getItemCount(): Int {
        return episodes.size
    }

    inner class EpisodeViewHolder(val binding: ItemEpisodePlayerBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.imageEpisode.setOnClickListener {
                onImageClick?.invoke(episodes[adapterPosition].title, adapterPosition)
            }
        }
    }
}