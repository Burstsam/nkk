package org.mosad.teapod.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_media.*
import org.mosad.teapod.MainActivity
import org.mosad.teapod.R
import org.mosad.teapod.util.GUIMedia
import java.net.URL
import java.net.URLEncoder

class MediaFragment(val media: GUIMedia, val streams: List<String>) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // load poster
        Glide.with(requireContext()).load(media.posterLink).into(image_poster)
        text_title.text = media.title
        text_desc.text = media.shortDesc

        println("media streams: $streams")

        initActions()
    }

    private fun initActions() {
        button_play.setOnClickListener { onClickButtonPlay() }
    }

    private fun onClickButtonPlay() {
        println("play ${streams.first()}")

        val mainActivity = activity as MainActivity
        mainActivity.startPlayer(streams.first())
    }

}