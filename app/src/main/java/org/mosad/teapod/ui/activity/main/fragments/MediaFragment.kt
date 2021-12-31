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
import org.mosad.teapod.parser.crunchyroll.Item
import org.mosad.teapod.parser.crunchyroll.NoneItem
import org.mosad.teapod.ui.activity.main.MainActivity
import org.mosad.teapod.ui.activity.main.viewmodel.MediaFragmentViewModel
import org.mosad.teapod.util.tmdb.TMDBApiController
import org.mosad.teapod.util.tmdb.TMDBMovie
import org.mosad.teapod.util.tmdb.TMDBTVShow

/**
 * The media detail fragment.
 * Note: the fragment is created only once, when selecting a similar title etc.
 * therefore fragments may be not empty and model may be the old one
 */
class MediaFragment(private val mediaIdStr: String, mediaCr: Item = NoneItem) : Fragment() {

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
        // TODO is position 0 always episodes? (and 1 always similar titles)
        TabLayoutMediator(binding.tabEpisodesSimilar, binding.pagerEpisodesSimilar) { tab, position ->
            tab.text = when(position) {
                0 -> getString(R.string.episodes)
                1 -> getString(R.string.similar_titles)
                else -> ""
            }
        }.attach()

        lifecycleScope.launch {
            model.loadCrunchy(mediaIdStr)

            updateGUI()
            initActions()
        }
    }

    override fun onResume() {
        super.onResume()

        // update the next ep text if there is one, since it may have changed
        // TODO reimplement
//        if (model.media.getEpisodeById(model.nextEpisodeId).title.isNotEmpty()) {
//            binding.textTitle.text = model.media.getEpisodeById(model.nextEpisodeId).title
//        }
    }

    /**
     * if tmdb data is present, use it, else use the aod data
     */
    private fun updateGUI() = with(model) {
        // generic gui
        val backdropUrl = tmdbResult.backdropPath?.let { TMDBApiController.imageUrl + it }
            ?: seriesCrunchy.images.poster_wide[0][2].source
        val posterUrl = tmdbResult.posterPath?.let { TMDBApiController.imageUrl + it }
            ?: seriesCrunchy.images.poster_tall[0][2].source

        // load poster and backdrop
        Glide.with(requireContext()).load(posterUrl)
            .into(binding.imagePoster)
        Glide.with(requireContext()).load(backdropUrl)
            .apply(RequestOptions.placeholderOf(ColorDrawable(Color.DKGRAY)))
            .apply(RequestOptions.bitmapTransform(BlurTransformation(20, 3)))
            .into(binding.imageBackdrop)

        binding.textTitle.text = seriesCrunchy.title
        binding.textOverview.text = seriesCrunchy.description
        binding.textAge.text = seriesCrunchy.maturityRatings.firstOrNull()

        binding.textYear.text = when(tmdbResult) {
            is TMDBTVShow -> (tmdbResult as TMDBTVShow).firstAirDate.substring(0, 4)
            is TMDBMovie -> (tmdbResult as TMDBMovie).releaseDate.substring(0, 4)
            else -> ""
        }

        // TODO set "my list" indicator
//        if (StorageController.myList.contains(media.aodId)) {
//            Glide.with(requireContext()).load(R.drawable.ic_baseline_check_24).into(binding.imageMyListAction)
//        } else {
//            Glide.with(requireContext()).load(R.drawable.ic_baseline_add_24).into(binding.imageMyListAction)
//        }

        // clear fragments, since it lives in onCreate scope (don't do this in onPause/onStop -> FragmentManager transaction)
        val fragmentsSize = if (fragments.lastIndex < 0) 0 else fragments.lastIndex
        fragments.clear()
        pagerAdapter.notifyItemRangeRemoved(0, fragmentsSize)

        // add the episodes fragment (as tab). Note: Movies are tv shows!
        MediaFragmentEpisodes().also {
            fragments.add(it)
            pagerAdapter.notifyItemInserted(fragments.indexOf(it))
        }

        // TODO reimplement via tmdb/metaDB
        // specific gui
        when (tmdbResult) {
            is TMDBTVShow -> {
                // episodes count
                binding.textEpisodesOrRuntime.text = resources.getQuantityString(
                    R.plurals.text_episodes_count,
                    episodesCrunchy.total,
                    episodesCrunchy.total
                )
            }
            is TMDBMovie -> {
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
            else -> {
                println("else")
            }
        }

//        if (mediaCrunchy.type == MediaType.TVSHOW.str) {
//            // TODO get next episode
////            nextEpisodeId = media.playlist.firstOrNull{ !it.watched }?.mediaId
////                ?: media.playlist.first().mediaId
//
//            // TODO title is the next episodes title
////            binding.textTitle.text = media.getEpisodeById(nextEpisodeId).title
//
//            // episodes count
//            binding.textEpisodesOrRuntime.text = resources.getQuantityString(
//                R.plurals.text_episodes_count,
//                episodesCrunchy.total,
//                episodesCrunchy.total
//            )
//
//            // episodes
//            MediaFragmentEpisodes().also {
//                fragments.add(it)
//                pagerAdapter.notifyItemInserted(fragments.indexOf(it))
//            }
//        } else if (media.type == MediaType.MOVIE) {
//            val tmdbMovie = (tmdbResult as TMDBMovie?)
//
//            if (tmdbMovie?.runtime != null) {
//                binding.textEpisodesOrRuntime.text = resources.getQuantityString(
//                    R.plurals.text_runtime,
//                    tmdbMovie.runtime,
//                    tmdbMovie.runtime
//                )
//            } else {
//                binding.textEpisodesOrRuntime.visibility = View.GONE
//            }
//        }

        // if has similar titles
        // TODO reimplement
//        if (media.similar.isNotEmpty()) {
//            MediaFragmentSimilar().also {
//                fragments.add(it)
//                pagerAdapter.notifyItemInserted(fragments.indexOf(it))
//            }
//        }

        // disable scrolling on appbar, if no tabs where added
        if(fragments.isEmpty()) {
            val params = binding.linearMedia.layoutParams as AppBarLayout.LayoutParams
            params.scrollFlags = 0 // clear all scroll flags
        }

        binding.frameLoading.visibility = View.GONE // hide loading indicator
    }

    private fun initActions() = with(model) {
        binding.buttonPlay.setOnClickListener {
            // TODO reimplement
//            when (media.type) {
//                MediaType.MOVIE -> playEpisode(media.playlist.first().mediaId)
//                MediaType.TVSHOW -> playEpisode(nextEpisodeId)
//                else -> Log.e(javaClass.name, "Wrong Type: ${media.type}")
//            }
        }

        // add or remove media from myList
        binding.linearMyListAction.setOnClickListener {
            // TODO reimplement
//            if (StorageController.myList.contains(media.aodId)) {
//                StorageController.myList.remove(media.aodId)
//                Glide.with(requireContext()).load(R.drawable.ic_baseline_add_24).into(binding.imageMyListAction)
//            } else {
//                StorageController.myList.add(media.aodId)
//                Glide.with(requireContext()).load(R.drawable.ic_baseline_check_24).into(binding.imageMyListAction)
//            }
//            StorageController.saveMyList(requireContext())
//
//            // notify home fragment on change
//            parentFragmentManager.findFragmentByTag("HomeFragment")?.let {
//                (it as HomeFragment).updateMyListMedia()
//            }
        }
    }

    /**
     * play the current episode
     * TODO this is also used in MediaFragmentEpisode, we should only have on implementation
     */
    private fun playEpisode(seasonId: String, episodeId: String) {
        (activity as MainActivity).startPlayer(seasonId, episodeId)
        Log.d(javaClass.name, "Started Player with  episodeId: $episodeId")

        //model.updateNextEpisode(episodeId) // set the correct next episode
    }

    /**
     * A simple pager adapter
     */
    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]
    }

}