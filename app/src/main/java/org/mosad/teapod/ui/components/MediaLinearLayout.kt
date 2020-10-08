package org.mosad.teapod.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.linear_media.view.*
import org.mosad.teapod.R


class MediaLinearLayout(context: Context?) : LinearLayout(context) {

    init {
        inflate(context, R.layout.linear_media, this)
    }

    fun setTitle(title: String): MediaLinearLayout = apply {
        text_title.text = title
    }

    fun setPoster(url: String): MediaLinearLayout = apply {
        Glide.with(context)
            .load(url)
            .into(image_poster)
    }


}