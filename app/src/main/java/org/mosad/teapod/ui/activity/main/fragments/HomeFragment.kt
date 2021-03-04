package org.mosad.teapod.ui.activity.main.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.mosad.teapod.R
import org.mosad.teapod.ui.activity.main.MainActivity
import org.mosad.teapod.databinding.FragmentHomeBinding
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.ItemMedia
import org.mosad.teapod.util.StorageController
import org.mosad.teapod.util.adapter.MediaItemAdapter
import org.mosad.teapod.util.decoration.MediaItemDecoration
import org.mosad.teapod.util.setDrawableTop
import org.mosad.teapod.util.showFragment

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapterMyList: MediaItemAdapter
    private lateinit var adapterNewEpisodes: MediaItemAdapter
    private lateinit var adapterNewSimulcasts: MediaItemAdapter
    private lateinit var adapterNewTitles: MediaItemAdapter
    private lateinit var adapterTopTen: MediaItemAdapter

    private lateinit var highlightMedia: ItemMedia

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GlobalScope.launch(Dispatchers.Main) {
            context?.let {
                initHighlight()
                initRecyclerViews()
                initActions()
            }
        }
    }

    private fun initHighlight() {
        if (AoDParser.highlightsList.isNotEmpty()) {
            highlightMedia =  AoDParser.highlightsList[0]

            binding.textHighlightTitle.text = highlightMedia.title
            Glide.with(requireContext()).load(highlightMedia.posterUrl)
                .into(binding.imageHighlight)

            if (StorageController.myList.contains(highlightMedia.id)) {
                binding.textHighlightMyList.setDrawableTop(R.drawable.ic_baseline_check_24)
            } else {
                binding.textHighlightMyList.setDrawableTop(R.drawable.ic_baseline_add_24)
            }
        }
    }

    private fun initRecyclerViews() {
        binding.recyclerMyList.addItemDecoration(MediaItemDecoration(9))
        binding.recyclerNewEpisodes.addItemDecoration(MediaItemDecoration(9))
        binding.recyclerNewSimulcasts.addItemDecoration(MediaItemDecoration(9))
        binding.recyclerNewTitles.addItemDecoration(MediaItemDecoration(9))
        binding.recyclerTopTen.addItemDecoration(MediaItemDecoration(9))

        // my list
        val myListMedia = StorageController.myList.map { elementId ->
            AoDParser.itemMediaList.first {
                elementId == it.id
            }
        }
        adapterMyList = MediaItemAdapter(myListMedia)
        binding.recyclerMyList.adapter = adapterMyList

        // new episodes
        adapterNewEpisodes = MediaItemAdapter(AoDParser.newEpisodesList)
        binding.recyclerNewEpisodes.adapter = adapterNewEpisodes

        // new simulcasts
        adapterNewSimulcasts = MediaItemAdapter(AoDParser.newSimulcastsList)
        binding.recyclerNewSimulcasts.adapter = adapterNewSimulcasts

        // new titles
        adapterNewTitles = MediaItemAdapter(AoDParser.newTitlesList)
        binding.recyclerNewTitles.adapter = adapterNewTitles

        // top ten
        adapterTopTen = MediaItemAdapter(AoDParser.topTenList)
        binding.recyclerTopTen.adapter = adapterTopTen
    }

    private fun initActions() {
        binding.buttonPlayHighlight.setOnClickListener {
            // TODO get next episode
            GlobalScope.launch {
                val media = AoDParser.getMediaById(highlightMedia.id)

                Log.d(javaClass.name, "Starting Player with  mediaId: ${media.id}")
                (activity as MainActivity).startPlayer(media.id, media.episodes.first().id)
            }
        }

        binding.textHighlightMyList.setOnClickListener {
            if (StorageController.myList.contains(highlightMedia.id)) {
                StorageController.myList.remove(highlightMedia.id)
                binding.textHighlightMyList.setDrawableTop(R.drawable.ic_baseline_add_24)
            } else {
                StorageController.myList.add(highlightMedia.id)
                binding.textHighlightMyList.setDrawableTop(R.drawable.ic_baseline_check_24)
            }
            StorageController.saveMyList(requireContext())

            updateMyListMedia() // update my list, since it has changed
        }

        binding.textHighlightInfo.setOnClickListener {
            activity?.showFragment(MediaFragment(highlightMedia.id))
        }

        adapterMyList.onItemClick = { mediaId, _ ->
            activity?.showFragment(MediaFragment(mediaId))
        }

        adapterNewEpisodes.onItemClick = { mediaId, _ ->
            activity?.showFragment(MediaFragment(mediaId))
        }

        adapterNewSimulcasts.onItemClick = { mediaId, _ ->
            activity?.showFragment(MediaFragment(mediaId))
        }

        adapterNewTitles.onItemClick = { mediaId, _ ->
            activity?.showFragment(MediaFragment(mediaId))
        }

        adapterTopTen.onItemClick = { mediaId, _ ->
            activity?.showFragment(MediaFragment(mediaId))
        }
    }

    /**
     * update my media list
     * TODO
     *  * auto call when StorageController.myList is changed
     *  * only update actual change and not all data (performance)
     */
    fun updateMyListMedia() {
        val myListMedia = StorageController.myList.map { elementId ->
            AoDParser.itemMediaList.first {
                elementId == it.id
            }
        }

        adapterMyList.updateMediaList(myListMedia)
        adapterMyList.notifyDataSetChanged()
    }

}