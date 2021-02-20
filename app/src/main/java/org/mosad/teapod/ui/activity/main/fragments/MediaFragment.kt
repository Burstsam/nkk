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
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayoutMediator
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.*
import org.mosad.teapod.R
import org.mosad.teapod.databinding.FragmentMediaBinding
import org.mosad.teapod.ui.activity.main.MainActivity
import org.mosad.teapod.ui.activity.main.viewmodel.MediaFragmentViewModel
import org.mosad.teapod.util.*
import org.mosad.teapod.util.DataTypes.MediaType


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

        // tab layout and pager TODO
        pagerAdapter = ScreenSlidePagerAdapter(requireActivity())
        // fix material components issue #1878, if more tabs are added increase
        binding.pagerEpisodesSimilar.offscreenPageLimit = 2
        binding.pagerEpisodesSimilar.adapter = pagerAdapter
        TabLayoutMediator(binding.tabEpisodesSimilar, binding.pagerEpisodesSimilar) { tab, position ->
            tab.text = if (model.media.type == MediaType.TVSHOW && position == 0) {
                getString(R.string.episodes)
            } else {
                getString(R.string.similar_titles)
            }
        }.attach()

        GlobalScope.launch(Dispatchers.Main) {
            model.load(mediaId) // load the streams and tmdb for the selected media

            if (this@MediaFragment.isAdded) {
                updateGUI()
                initActions()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // update the next ep text, since it may have changed
        binding.textTitle.text = model.nextEpisode.title
    }

    /**
     * if tmdb data is present, use it, else use the aod data
     */
    private fun updateGUI() = with(model) {
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

        // clear fragments, since it lives in onCreate scope (don't do this in onPause/onStop -> FragmentManager transaction)
        fragments.clear()
        pagerAdapter.notifyDataSetChanged()

        // specific gui
        if (media.type == MediaType.TVSHOW) {
            // get next episode
            nextEpisode = if (media.episodes.firstOrNull{ !it.watched } != null) {
                media.episodes.first{ !it.watched }
            } else {
                media.episodes.first()
            }

            // title is the next episodes title
            binding.textTitle.text = nextEpisode.title

            // episodes count
            binding.textEpisodesOrRuntime.text = resources.getQuantityString(
                R.plurals.text_episodes_count,
                media.info.episodesCount,
                media.info.episodesCount
            )

            // episodes
            fragments.add(MediaFragmentEpisodes())
            pagerAdapter.notifyDataSetChanged()
        } else if (media.type == MediaType.MOVIE) {

            if (tmdb.runtime > 0) {
                binding.textEpisodesOrRuntime.text = resources.getQuantityString(
                    R.plurals.text_runtime,
                    tmdb.runtime,
                    tmdb.runtime
                )
            } else {
                binding.textEpisodesOrRuntime.visibility = View.GONE
            }
        }

        // if has similar titles
        if (media.info.similar.isNotEmpty()) {
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
            when (media.type) {
                MediaType.MOVIE -> playEpisode(media.episodes.first())
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
    }

    /**
     * play the current episode
     * TODO this is also used in MediaFragmentEpisode, we should only have on implementation
     */
    private fun playEpisode(ep: Episode) {
        (activity as MainActivity).startPlayer(model.media.id, ep.id)
        Log.d(javaClass.name, "Started Player with  episodeId: ${ep.id}")

        model.updateNextEpisode(ep) // set the correct next episode
    }

    /**
     * A simple pager adapter
     */
    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]
    }

}