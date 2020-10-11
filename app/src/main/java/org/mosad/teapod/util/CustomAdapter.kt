package org.mosad.teapod.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import org.mosad.teapod.R
import java.util.*
import kotlin.collections.ArrayList

class CustomAdapter(val context: Context, private val originalMedia: ArrayList<GUIMedia>) : BaseAdapter(), Filterable {

    private var filteredMedia = originalMedia.map { it.copy() }
    private val customFilter = CustomFilter()

    init {
        println("initial filtered size is: ${filteredMedia.size}")
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.linear_media, parent, false)

        val textTitle = view.findViewById<TextView>(R.id.text_title)
        val imagePoster = view.findViewById<ImageView>(R.id.image_poster)

        textTitle.text = filteredMedia[position].title
        Glide.with(context).load(filteredMedia[position].posterLink).into(imagePoster)

        return view
    }

    override fun getFilter(): Filter {
        return customFilter
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

    inner class CustomFilter : Filter() {
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

            println("filtered size is: ${results.count}")

            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            filteredMedia = results?.values as ArrayList<GUIMedia>
            notifyDataSetChanged()
        }

    }

}