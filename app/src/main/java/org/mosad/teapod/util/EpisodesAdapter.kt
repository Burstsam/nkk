package org.mosad.teapod.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.component_episode.view.*
import org.mosad.teapod.R

class EpisodesAdapter(private val data: List<Episode>, private val context: Context) : RecyclerView.Adapter<EpisodesAdapter.MyViewHolder>() {

    var onItemClick: ((String, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.component_episode, parent, false)

        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.view.text_episode_title.text = "Episode ${data[position].number} ${data[position].description}"

        if (data[position].posterLink.isNotEmpty()) {
            Glide.with(context).load(data[position].posterLink).into(holder.view.image_episode)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class MyViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        init {
            view.setOnClickListener {
                onItemClick?.invoke(data[adapterPosition].title, adapterPosition)
            }
        }
    }
}