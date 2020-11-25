package org.mosad.teapod.util.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.mosad.teapod.databinding.ItemMediaBinding
import org.mosad.teapod.util.ItemMedia
import java.util.*

class MediaItemAdapter(private val media: List<ItemMedia>) : RecyclerView.Adapter<MediaItemAdapter.MediaViewHolder>(), Filterable {

    var onItemClick: ((Int, Int) -> Unit)? = null
    private val filter = MediaFilter()
    private var filteredMedia = media.map { it.copy() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemAdapter.MediaViewHolder {
        return MediaViewHolder(ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MediaItemAdapter.MediaViewHolder, position: Int) {
        holder.binding.root.apply {
            holder.binding.textTitle.text = filteredMedia[position].title
            Glide.with(context).load(filteredMedia[position].posterUrl).into(holder.binding.imagePoster)
        }
    }

    override fun getItemCount(): Int {
        return filteredMedia.size
    }

    override fun getFilter(): Filter {
        return filter
    }

    inner class MediaViewHolder(val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onItemClick?.invoke(filteredMedia[adapterPosition].id, adapterPosition)
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
            filteredMedia = results?.values as List<ItemMedia>
            notifyDataSetChanged()
        }
    }

}