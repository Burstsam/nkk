package org.mosad.teapod.util.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.item_media.view.*
import org.mosad.teapod.R
import org.mosad.teapod.util.Media
import java.util.*

class MediaItemAdapter(private val media: List<Media>) : RecyclerView.Adapter<MediaItemAdapter.ViewHolder>(), Filterable {

    var onItemClick: ((Media, Int) -> Unit)? = null
    private val filter = MediaFilter()
    private var filteredMedia = media.map { it.copy() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaItemAdapter.ViewHolder, position: Int) {
        holder.view.apply {
            text_title.text = filteredMedia[position].title
            Glide.with(context).load(filteredMedia[position].info.posterLink).into(image_poster)
        }
    }

    override fun getItemCount(): Int {
        return filteredMedia.size
    }

    override fun getFilter(): Filter {
        return filter
    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        init {
            view.setOnClickListener {
                onItemClick?.invoke(filteredMedia[adapterPosition], adapterPosition)
            }
        }
    }

    inner class MediaFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterTerm = constraint.toString().toLowerCase(Locale.ROOT)
            val results = FilterResults()

            val filteredList = if (filterTerm.isEmpty()) {
                media
            } else {
                media.filter {
                    it.title.toLowerCase(Locale.ROOT).contains(filterTerm)
                }
            }

            results.values = filteredList
            results.count = filteredList.size

            return results
        }

        @Suppress("unchecked_cast")
        /**
         * suppressing unchecked cast is safe, since we only use Media
         */
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            filteredMedia = results?.values as List<Media>
            notifyDataSetChanged()
        }
    }

}