package org.mosad.teapod.util.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import org.mosad.teapod.R
import org.mosad.teapod.util.Media
import java.util.*

class MediaItemAdapter(val context: Context, private val originalMedia: ArrayList<Media>) : BaseAdapter(), Filterable {

    private var filteredMedia = originalMedia.map { it.copy() }
    private val filer = MediaFilter()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_media, parent, false)

        val textTitle = view.findViewById<TextView>(R.id.text_title)
        val imagePoster = view.findViewById<ImageView>(R.id.image_poster)

        textTitle.text = filteredMedia[position].title
        Glide.with(context).load(filteredMedia[position].info.posterLink).into(imagePoster)

        return view
    }

    override fun getFilter(): Filter {
        return filer
    }

    override fun getCount(): Int {
        return filteredMedia.size
    }

    override fun getItem(position: Int): Any {
        return filteredMedia[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }


    inner class MediaFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterTerm = constraint.toString().toLowerCase(Locale.ROOT)
            val results = FilterResults()

            val filteredList = if (filterTerm.isEmpty()) {
                originalMedia
            } else {
                originalMedia.filter {
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
            filteredMedia = results?.values as ArrayList<Media>
            notifyDataSetChanged()
        }

    }

}