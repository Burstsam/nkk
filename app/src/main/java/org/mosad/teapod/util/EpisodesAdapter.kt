package org.mosad.teapod.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.component_episode.view.*
import org.mosad.teapod.R

class EpisodesAdapter(private val data: List<String>) : RecyclerView.Adapter<EpisodesAdapter.MyViewHolder>() {

    var onItemClick: ((String, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.component_episode, parent, false)

        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.view .text_episode_title.text = data[position]
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class MyViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        init {
            view.setOnClickListener {
                onItemClick?.invoke(data[adapterPosition], adapterPosition)
            }
        }
    }
}