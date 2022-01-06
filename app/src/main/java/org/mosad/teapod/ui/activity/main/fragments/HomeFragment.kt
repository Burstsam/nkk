package org.mosad.teapod.ui.activity.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.mosad.teapod.databinding.FragmentHomeBinding
import org.mosad.teapod.parser.crunchyroll.Crunchyroll
import org.mosad.teapod.parser.crunchyroll.SortBy
import org.mosad.teapod.util.ItemMedia
import org.mosad.teapod.util.adapter.MediaItemAdapter
import org.mosad.teapod.util.decoration.MediaItemDecoration
import org.mosad.teapod.util.showFragment
import org.mosad.teapod.util.toItemMediaList

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapterUpNext: MediaItemAdapter
    private lateinit var adapterWatchlist: MediaItemAdapter
    private lateinit var adapterNewTitles: MediaItemAdapter
    private lateinit var adapterTopTen: MediaItemAdapter

    private lateinit var highlightMedia: ItemMedia

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
        // TODO
//        if (AoDParser.highlightsList.isNotEmpty()) {
//            highlightMedia =  AoDParser.highlightsList[0]
//
//            binding.textHighlightTitle.text = highlightMedia.title
//            Glide.with(requireContext()).load(highlightMedia.posterUrl)
//                .into(binding.imageHighlight)
//
//            if (StorageController.myList.contains(0)) {
//                binding.textHighlightMyList.setDrawableTop(R.drawable.ic_baseline_check_24)
//            } else {
//                binding.textHighlightMyList.setDrawableTop(R.drawable.ic_baseline_add_24)
//            }
//        }
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

        // new simulcasts TODO replace with new titles? browse(sortBy = SortBy.NEWLY_ADDED, n = 50)
        val simulcastsJob = lifecycleScope.launch {
//            val latestSeasonTag = Crunchyroll.seasonList().items.first().id
//            val newSimulcasts = Crunchyroll.browse(seasonTag = latestSeasonTag, n = 50)

            val newSimulcasts = Crunchyroll.browse(sortBy = SortBy.NEWLY_ADDED, n = 50)

            adapterNewTitles = MediaItemAdapter(newSimulcasts.toItemMediaList())
            binding.recyclerNewTitles.adapter = adapterNewTitles
        }
        asyncJobList.add(simulcastsJob)

        // top ten TODO
//        adapterTopTen = MediaItemAdapter(AoDParser.topTenList)
//        binding.recyclerTopTen.adapter = adapterTopTen

        asyncJobList.joinAll()
    }

    private fun initActions() {
        binding.buttonPlayHighlight.setOnClickListener {
            // TODO get next episode
            lifecycleScope.launch {
                // TODO
                //val media = AoDParser.getMediaById(0)

                // Log.d(javaClass.name, "Starting Player with  mediaId: ${media.aodId}")
                //(activity as MainActivity).startPlayer(media.aodId, media.playlist.first().mediaId)
            }
        }

        binding.textHighlightMyList.setOnClickListener {
            // TODO implement if needed

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
            activity?.showFragment(MediaFragment(""))
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

//        adapterTopTen.onItemClick = { id, _ ->
//            activity?.showFragment(MediaFragment("")) //(mediaId))
//        }
    }

}