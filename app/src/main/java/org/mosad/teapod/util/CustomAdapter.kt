package org.mosad.teapod.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import org.mosad.teapod.R

class CustomAdapter(context: Context, private val media: ArrayList<GUIMedia>) : ArrayAdapter<GUIMedia>(context, R.layout.linear_media, media) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.linear_media, parent, false)

        val textTitle = view.findViewById<TextView>(R.id.text_title)
        val imagePoster = view.findViewById<ImageView>(R.id.image_poster)

        textTitle.text = media[position].title
        Glide.with(context).load(media[position].posterLink).into(imagePoster)

        return view
    }

}