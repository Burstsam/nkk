package org.mosad.teapod.ui.activity.main.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayoutMediator
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.launch
import org.mosad.teapod.R
import org.mosad.teapod.databinding.FragmentMediaBinding
import org.mosad.teapod.ui.activity.main.MainActivity
import org.mosad.teapod.ui.activity.main.viewmodel.MediaFragmentViewModel
import org.mosad.teapod.util.DataTypes.MediaType
import org.mosad.teapod.util.StorageController
import org.mosad.teapod.util.tmdb.TMDBMovie
import org.mosad.teapod.util.tmdb.TMDBApiController

/**
 * The media detail fragment.
 * Note: the fragment is created only once, when selecting a similar title etc.
 * therefore fragments may be not empty and model may be the old one
 */
class MediaFragment(private val mediaId: Int) : Fragment() {

    private lateinit var binding: FragmentMediaBinding
    private lateinit var pagerAdapter: FragmentStateAdapter

    private val fragments = arrayListOf<Fragment>()

    private val model: MediaFragmentViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.frameLoading.visibility = View.VISIBLE

        // tab layout and pager
        pagerAdapter = ScreenSlidePagerAdapter(requireActivity())
        // fix material components issue #1878, if more tabs are added increase
        binding.pagerEpisodesSimilar.offscreenPageLimit = 2
        binding.pagerEpisodesSimilar.adapter = pagerAdapter
        TabLayoutMediator(binding.tabEpisodesSimilar, binding.pagerEpisodesSimilar) { tab, position ->
            tab.text = if (model.media2.type == MediaType.TVSHOW && position == 0) {
                getString(R.string.episodes)
            } else {
                getString(R.string.similar_titles)
            }
        }.attach()

        lifecycleScope.launch {
            model.load(mediaId) // load the streams and tmdb for the selected media

            updateGUI()
            initActions()
        }
    }

    override fun onResume() {
        super.onResume()

        // update the next ep text if there is one, since it may have changed
        println(model.nextEpisodeId)
        if (model.media2.getEpisodeById(model.nextEpisodeId).title.isNotEmpty()) {
            binding.textTitle.text = model.media2.getEpisodeById(model.nextEpisodeId).title
        }
    }

    /**
     * if tmdb data is present, use it, else use the aod data
     */
    private fun updateGUI() = with(model) {
        // generic gui
        val backdropUrl = tmdbResult?.backdropPath?.let { TMDBApiController.imageUrl + it }
            ?: media2.posterURL
        val posterUrl = tmdbResult?.posterPath?.let { TMDBApiController.imageUrl + it }
            ?: media2.posterURL

        // load poster and backdrop
        Glide.with(requireContext()).load(posterUrl)
            .into(binding.imagePoster)
        Glide.with(requireContext()).load(backdropUrl)
            .apply(RequestOptions.placeholderOf(ColorDrawable(Color.DKGRAY)))
            .apply(RequestOptions.bitmapTransform(BlurTransformation(20, 3)))
            .into(binding.imageBackdrop)

        binding.textTitle.text = media2.title
        binding.textYear.text = media2.year.toString()
        binding.textAge.text = media2.age.toString()
        binding.textOverview.text = media2.shortText

        // set "my list" indicator
        if (StorageController.myList.contains(media2.aodId)) {
            Glide.with(requireContext()).load(R.drawable.ic_baseline_check_24).into(binding.imageMyListAction)
        } else {
            Glide.with(requireContext()).load(R.drawable.ic_baseline_add_24).into(binding.imageMyListAction)
        }

        // clear fragments, since it lives in onCreate scope (don't do this in onPause/onStop -> FragmentManager transaction)
        fragments.clear()
        pagerAdapter.notifyDataSetChanged()

        // specific gui
        if (media2.type == MediaType.TVSHOW) {
            // get next episode
            nextEpisodeId = media2.playlist.firstOrNull{ !it.watched }?.mediaId
                ?: media2.playlist.first().mediaId

            // title is the next episodes title
            binding.textTitle.text = media2.getEpisodeById(nextEpisodeId).title

            // episodes count
            binding.textEpisodesOrRuntime.text = resources.getQuantityString(
                R.plurals.text_episodes_count,
                media2.playlist.size,
                media2.playlist.size
            )

            // episodes
            fragments.add(MediaFragmentEpisodes())
            pagerAdapter.notifyDataSetChanged()
        } else if (media2.type == MediaType.MOVIE) {
            val tmdbMovie = (tmdbResult as TMDBMovie?)

            if (tmdbMovie?.runtime != null) {
                binding.textEpisodesOrRuntime.text = resources.getQuantityString(
                    R.plurals.text_runtime,
                    tmdbMovie.runtime,
                    tmdbMovie.runtime
                )
            } else {
                binding.textEpisodesOrRuntime.visibility = View.GONE
            }
        }

        // if has similar titles
        if (media2.similar.isNotEmpty()) {
            fragments.add(MediaFragmentSimilar())
            pagerAdapter.notifyDataSetChanged()
        }

        // disable scrolling on appbar, if no tabs where added
        if(fragments.isEmpty()) {
            val params = binding.linearMedia.layoutParams as AppBarLayout.LayoutParams
            params.scrollFlags = 0 // clear all scroll flags
        }

        binding.frameLoading.visibility = View.GONE // hide loading indicator
    }

    private fun initActions() = with(model) {
        binding.buttonPlay.setOnClickListener {
            when (media2.type) {
                MediaType.MOVIE -> playEpisode(media2.playlist.first().mediaId)
                MediaType.TVSHOW -> playEpisode(nextEpisodeId)
                else -> Log.e(javaClass.name, "Wrong Type: ${media2.type}")
            }
        }

        // add or remove media from myList
        binding.linearMyListAction.setOnClickListener {
            if (StorageController.myList.contains(media2.aodId)) {
                StorageController.myList.remove(media2.aodId)
                Glide.with(requireContext()).load(R.drawable.ic_baseline_add_24).into(binding.imageMyListAction)
            } else {
                StorageController.myList.add(media2.aodId)
                Glide.with(requireContext()).load(R.drawable.ic_baseline_check_24).into(binding.imageMyListAction)
            }
            StorageController.saveMyList(requireContext())

            // notify home fragment on change
            parentFragmentManager.findFragmentByTag("HomeFragment")?.let {
                (it as HomeFragment).updateMyListMedia()
            }
        }
    }

    /**
     * play the current episode
     * TODO this is also used in MediaFragmentEpisode, we should only have on implementation
     */
    private fun playEpisode(episodeId: Int) {
        (activity as MainActivity).startPlayer(model.media2.aodId, episodeId)
        Log.d(javaClass.name, "Started Player with  episodeId: $episodeId")

        model.updateNextEpisode(episodeId) // set the correct next episode
    }

    /**
     * A simple pager adapter
     */
    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]
    }

}