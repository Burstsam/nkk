package org.mosad.teapod.util.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import org.mosad.teapod.R
import org.mosad.teapod.databinding.ItemEpisodeBinding
import org.mosad.teapod.util.AoDEpisode
import org.mosad.teapod.util.tmdb.TMDBTVEpisode

class EpisodeItemAdapter(private val episodes: List<AoDEpisode>, private val tmdbEpisodes: List<TMDBTVEpisode>?) : RecyclerView.Adapter<EpisodeItemAdapter.EpisodeViewHolder>() {

    var onImageClick: ((String, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        return EpisodeViewHolder(ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val context = holder.binding.root.context
        val ep = episodes[position]

        val titleText = if (ep.hasDub()) {
            context.getString(R.string.component_episode_title, ep.numberStr, ep.description)
        } else {
            context.getString(R.string.component_episode_title_sub, ep.numberStr, ep.description)
        }

        holder.binding.textEpisodeTitle.text = titleText
        holder.binding.textEpisodeDesc.text = if (ep.shortDesc.isNotEmpty()) {
            ep.shortDesc
        } else if (tmdbEpisodes != null && position < tmdbEpisodes.size){
            tmdbEpisodes[position].overview
        } else {
            ""
        }

        if (ep.imageURL.isNotEmpty()) {
            Glide.with(context).load(ep.imageURL)
                .apply(RequestOptions.placeholderOf(ColorDrawable(Color.DKGRAY)))
                .apply(RequestOptions.bitmapTransform(RoundedCornersTransformation(10, 0)))
                .into(holder.binding.imageEpisode)
        }

        if (ep.watched) {
            holder.binding.imageWatched.setImageDrawable(
                ContextCompat.getDrawable(context, R.drawable.ic_baseline_check_circle_24)
            )
        } else {
            holder.binding.imageWatched.setImageDrawable(null)
        }
    }

    override fun getItemCount(): Int {
        return episodes.size
    }

    fun updateWatchedState(watched: Boolean, position: Int) {
        // use getOrNull as there could be a index out of bound when running this in onResume()
        episodes.getOrNull(position)?.watched = watched
    }

    inner class EpisodeViewHolder(val binding: ItemEpisodeBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.imageEpisode.setOnClickListener {
                onImageClick?.invoke(episodes[adapterPosition].title, adapterPosition)
            }
        }
    }
}