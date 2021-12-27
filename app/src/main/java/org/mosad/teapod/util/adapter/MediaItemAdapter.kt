package org.mosad.teapod.util.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.mosad.teapod.databinding.ItemMediaBinding
import org.mosad.teapod.util.ItemMedia

class MediaItemAdapter(private val items: List<ItemMedia>) : RecyclerView.Adapter<MediaItemAdapter.MediaViewHolder>() {

    var onItemClick: ((id: String, position: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemAdapter.MediaViewHolder {
        return MediaViewHolder(ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MediaItemAdapter.MediaViewHolder, position: Int) {
        holder.binding.root.apply {
            holder.binding.textTitle.text = items[position].title
            Glide.with(context).load(items[position].posterUrl).into(holder.binding.imagePoster)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class MediaViewHolder(val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onItemClick?.invoke(
                    items[bindingAdapterPosition].id,
                    bindingAdapterPosition
                )
            }
        }
    }

}