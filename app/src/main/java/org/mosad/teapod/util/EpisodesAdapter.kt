package org.mosad.teapod.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.component_episode.view.*
import org.mosad.teapod.R

class EpisodesAdapter(private val episodes: List<Episode>, private val context: Context) : RecyclerView.Adapter<EpisodesAdapter.MyViewHolder>() {

    var onItemClick: ((String, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.component_episode, parent, false)

        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.view.text_episode_title.text = context.getString(
            R.string.component_episode_title,
            episodes[position].number,
            episodes[position].description
        )
        holder.view.text_episode_desc.text = episodes[position].shortDesc

        if (episodes[position].posterLink.isNotEmpty()) {
            Glide.with(context).load(episodes[position].posterLink).into(holder.view.image_episode)
        }

        if (!episodes[position].watched) {
            holder.view.image_watched.setImageDrawable(null)
        }
    }

    override fun getItemCount(): Int {
        return episodes.size
    }

    fun updateWatchedState(watched: Boolean, position: Int) {
        episodes[position].watched = watched
    }

    inner class MyViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        init {
            view.setOnClickListener {
                onItemClick?.invoke(episodes[adapterPosition].title, adapterPosition)
            }
        }
    }
}