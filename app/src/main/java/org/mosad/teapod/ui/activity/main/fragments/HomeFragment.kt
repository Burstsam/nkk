package org.mosad.teapod.ui.activity.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.mosad.teapod.databinding.FragmentHomeBinding
import org.mosad.teapod.parser.crunchyroll.Crunchyroll
import org.mosad.teapod.parser.crunchyroll.Item
import org.mosad.teapod.parser.crunchyroll.SortBy
import org.mosad.teapod.util.adapter.MediaItemAdapter
import org.mosad.teapod.util.decoration.MediaItemDecoration
import org.mosad.teapod.util.showFragment
import org.mosad.teapod.util.toItemMediaList
import kotlin.random.Random

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapterUpNext: MediaItemAdapter
    private lateinit var adapterWatchlist: MediaItemAdapter
    private lateinit var adapterNewTitles: MediaItemAdapter
    private lateinit var adapterTopTen: MediaItemAdapter

    private lateinit var highlightMedia: Item

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            context?.let {
                initHighlight()
                initRecyclerViews()
                initActions()
            }
        }
    }

    private fun initHighlight() {
        lifecycleScope.launch {
            val newTitles = Crunchyroll.browse(sortBy = SortBy.NEWLY_ADDED, n = 10)
            // FIXME crashes on newTitles.items.size == 0
            highlightMedia =  newTitles.items[Random.nextInt(newTitles.items.size)]

            // add media item to gui
            binding.textHighlightTitle.text = highlightMedia.title
            Glide.with(requireContext()).load(highlightMedia.images.poster_wide[0][3].source)
                .into(binding.imageHighlight)

            // TODO watchlist indicator
//            if (StorageController.myList.contains(0)) {
//                binding.textHighlightMyList.setDrawableTop(R.drawable.ic_baseline_check_24)
//            } else {
//                binding.textHighlightMyList.setDrawableTop(R.drawable.ic_baseline_add_24)
//            }
        }
    }

    /**
     * Suspend, since adapters need to be initialized before we can initialize the actions.
     */
    private suspend fun initRecyclerViews() {
        binding.recyclerWatchlist.addItemDecoration(MediaItemDecoration(9))
        binding.recyclerNewEpisodes.addItemDecoration(MediaItemDecoration(9))
        binding.recyclerNewTitles.addItemDecoration(MediaItemDecoration(9))
        binding.recyclerTopTen.addItemDecoration(MediaItemDecoration(9))

        val asyncJobList = arrayListOf<Job>()

        // continue watching
        val upNextJob = lifecycleScope.launch {
            // TODO create EpisodeItemAdapter, which will start the playback of the selected episode immediately
            adapterUpNext = MediaItemAdapter(Crunchyroll.upNextAccount().toItemMediaList())
            binding.recyclerNewEpisodes.adapter = adapterUpNext
        }
        asyncJobList.add(upNextJob)

        // watchlist
        val watchlistJob = lifecycleScope.launch {
            adapterWatchlist = MediaItemAdapter(Crunchyroll.watchlist(50).toItemMediaList())
            binding.recyclerWatchlist.adapter = adapterWatchlist
        }
        asyncJobList.add(watchlistJob)

        // new simulcasts
        val simulcastsJob = lifecycleScope.launch {
            // val latestSeasonTag = Crunchyroll.seasonList().items.first().id
            // val newSimulcasts = Crunchyroll.browse(seasonTag = latestSeasonTag, n = 50)
            val newSimulcasts = Crunchyroll.browse(sortBy = SortBy.NEWLY_ADDED, n = 50)

            adapterNewTitles = MediaItemAdapter(newSimulcasts.toItemMediaList())
            binding.recyclerNewTitles.adapter = adapterNewTitles
        }
        asyncJobList.add(simulcastsJob)

        // newly added / top ten
        val newlyAddedJob = lifecycleScope.launch {
            adapterTopTen = MediaItemAdapter(Crunchyroll.browse(sortBy = SortBy.POPULARITY, n = 10).toItemMediaList())
            binding.recyclerTopTen.adapter = adapterTopTen
        }
        asyncJobList.add(newlyAddedJob)

        asyncJobList.joinAll()
    }

    private fun initActions() {
        binding.buttonPlayHighlight.setOnClickListener {
            // TODO implement
            lifecycleScope.launch {
                //val media = AoDParser.getMediaById(0)

                // Log.d(javaClass.name, "Starting Player with  mediaId: ${media.aodId}")
                //(activity as MainActivity).startPlayer(media.aodId, media.playlist.first().mediaId)
            }
        }

        binding.textHighlightMyList.setOnClickListener {
            // TODO implement
//            if (StorageController.myList.contains(0)) {
//                StorageController.myList.remove(0)
//                binding.textHighlightMyList.setDrawableTop(R.drawable.ic_baseline_add_24)
//            } else {
//                StorageController.myList.add(0)
//                binding.textHighlightMyList.setDrawableTop(R.drawable.ic_baseline_check_24)
//            }
//            StorageController.saveMyList(requireContext())
        }

        binding.textHighlightInfo.setOnClickListener {
            activity?.showFragment(MediaFragment(highlightMedia.id))
        }

        adapterUpNext.onItemClick = { id, _ ->
            activity?.showFragment(MediaFragment(id))
        }

        adapterWatchlist.onItemClick = { id, _ ->
            activity?.showFragment(MediaFragment(id))
        }

        adapterNewTitles.onItemClick = { id, _ ->
            activity?.showFragment(MediaFragment(id))
        }

        adapterTopTen.onItemClick = { id, _ ->
            activity?.showFragment(MediaFragment(id)) //(mediaId))
        }
    }

}
