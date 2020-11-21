package org.mosad.teapod.util.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.item_episode.view.*
import org.mosad.teapod.R
import org.mosad.teapod.util.Episode

class EpisodeItemAdapter(private val episodes: List<Episode>) : RecyclerView.Adapter<EpisodeItemAdapter.EpisodeViewHolder>() {

    var onItemClick: ((String, Int) -> Unit)? = null
    var onImageClick: ((String, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false)

        return EpisodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val context = holder.view.context
        val ep = episodes[position]

        val titleText = if (ep.priStreamUrl.isEmpty() && ep.secStreamOmU) {
            context.getString(R.string.component_episode_title_sub, ep.number, ep.description)
        } else {
            context.getString(R.string.component_episode_title, ep.number, ep.description)
        }

        holder.view.text_episode_title.text = titleText
        holder.view.text_episode_desc.text = ep.shortDesc

        if (episodes[position].posterUrl.isNotEmpty()) {
            Glide.with(context).load(ep.posterUrl)
                .apply(RequestOptions.bitmapTransform(RoundedCornersTransformation(10, 0)))
                .into(holder.view.image_episode)
        }

        if (ep.watched) {
            holder.view.image_watched.setImageDrawable(
                ContextCompat.getDrawable(context, R.drawable.ic_baseline_check_circle_24)
            )
        } else {
            holder.view.image_watched.setImageDrawable(null)
        }
    }

    override fun getItemCount(): Int {
        return episodes.size
    }

    fun updateWatchedState(watched: Boolean, position: Int) {
        episodes[position].watched = watched
    }

    inner class EpisodeViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        init {
            view.setOnClickListener {
                onItemClick?.invoke(episodes[adapterPosition].title, adapterPosition)
            }

            view.image_episode.setOnClickListener {
                onImageClick?.invoke(episodes[adapterPosition].title, adapterPosition)
            }
        }
    }
}