package org.mosad.teapod.ui.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation
import org.mosad.teapod.MainActivity
import org.mosad.teapod.R
import org.mosad.teapod.databinding.FragmentMediaBinding
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.DataTypes.MediaType
import org.mosad.teapod.util.Episode
import org.mosad.teapod.util.Media
import org.mosad.teapod.util.StorageController
import org.mosad.teapod.util.TMDBResponse
import org.mosad.teapod.util.adapter.EpisodeItemAdapter

class MediaFragment(private val media: Media, private val tmdb: TMDBResponse) : Fragment() {

    private lateinit var binding: FragmentMediaBinding
    private lateinit var adapterRecEpisodes: EpisodeItemAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var nextEpisode: Episode

    companion object {
        lateinit var instance: MediaFragment
    }

    init {
        instance = this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initGUI()
        initActions()
    }

    /**
     * if tmdb data is present, use it, else use the aod data
     */
    private fun initGUI() {
        // generic gui
        val backdropUrl = if (tmdb.backdropUrl.isNotEmpty()) tmdb.backdropUrl else media.info.posterUrl
        val posterUrl = if (tmdb.posterUrl.isNotEmpty()) tmdb.posterUrl else media.info.posterUrl

        Glide.with(requireContext()).load(backdropUrl)
            .apply(RequestOptions.placeholderOf(ColorDrawable(Color.DKGRAY)))
            .apply(RequestOptions.bitmapTransform(BlurTransformation(20, 3)))
            .into(binding.imageBackdrop)

        Glide.with(requireContext()).load(posterUrl)
            .into(binding.imagePoster)

        binding.textTitle.text = media.info.title
        binding.textYear.text = media.info.year.toString()
        binding.textAge.text = media.info.age.toString()
        binding.textOverview.text = media.info.shortDesc
        if (StorageController.myList.contains(media.id)) {
            Glide.with(requireContext()).load(R.drawable.ic_baseline_check_24).into(binding.imageMyListAction)
        } else {
            Glide.with(requireContext()).load(R.drawable.ic_baseline_add_24).into(binding.imageMyListAction)
        }

        // specific gui
        if (media.type == MediaType.TVSHOW) {
            adapterRecEpisodes = EpisodeItemAdapter(media.episodes)
            viewManager = LinearLayoutManager(context)
            binding.recyclerEpisodes.layoutManager = viewManager
            binding.recyclerEpisodes.adapter = adapterRecEpisodes

            binding.textEpisodesOrRuntime.text = getString(R.string.text_episodes_count, media.info.episodesCount)

            // get next episode
            nextEpisode = if (media.episodes.firstOrNull{ !it.watched } != null) {
                media.episodes.first{ !it.watched }
            } else {
                media.episodes.first()
            }

            // title is the next episodes title
            binding.textTitle.text = nextEpisode.title
        } else if (media.type == MediaType.MOVIE) {
            binding.recyclerEpisodes.visibility = View.GONE

            if (tmdb.runtime > 0) {
                binding.textEpisodesOrRuntime.text = getString(R.string.text_runtime, tmdb.runtime)
            } else {
                binding.textEpisodesOrRuntime.visibility = View.GONE
            }
        }
    }

    private fun initActions() {
        binding.buttonPlay.setOnClickListener {
            when (media.type) {
                MediaType.MOVIE -> playStream(media.episodes.first())
                MediaType.TVSHOW -> playEpisode(nextEpisode)
                else -> Log.e(javaClass.name, "Wrong Type: $media.type")
            }
        }

        // add or remove media from myList
        binding.linearMyListAction.setOnClickListener {
            if (StorageController.myList.contains(media.id)) {
                StorageController.myList.remove(media.id)
                Glide.with(requireContext()).load(R.drawable.ic_baseline_add_24).into(binding.imageMyListAction)
            } else {
                StorageController.myList.add(media.id)
                Glide.with(requireContext()).load(R.drawable.ic_baseline_check_24).into(binding.imageMyListAction)
            }
            StorageController.saveMyList(requireContext())

            // notify home fragment on change
            parentFragmentManager.findFragmentByTag("HomeFragment")?.let {
                (it as HomeFragment).updateMyListMedia()
            }
        }

        // set onItemClick only in adapter is initialized
        if (this::adapterRecEpisodes.isInitialized) {
            adapterRecEpisodes.onImageClick = { _, position ->
                playEpisode(media.episodes[position])
            }
        }
    }

    private fun playEpisode(ep: Episode) {
        playStream(ep)

        // update watched state
        updateWatchedState(ep)
        //AoDParser.sendCallback(ep.watchedCallback)
        //adapterRecEpisodes.updateWatchedState(true, media.episodes.indexOf(ep))
        //adapterRecEpisodes.notifyDataSetChanged()

        // update nextEpisode
        nextEpisode = if (media.episodes.firstOrNull{ !it.watched } != null) {
            media.episodes.first{ !it.watched }
        } else {
            media.episodes.first()
        }
        binding.textTitle.text = nextEpisode.title
    }

    private fun playStream(ep: Episode) {
        Log.d(javaClass.name, "Starting Player with  mediaId: ${media.id}")
        (activity as MainActivity).startPlayer(media.id, ep.id)
    }

    fun updateWatchedState(ep: Episode) {
        AoDParser.sendCallback(ep.watchedCallback)
        adapterRecEpisodes.updateWatchedState(true, media.episodes.indexOf(ep))
        adapterRecEpisodes.notifyDataSetChanged()
    }


}