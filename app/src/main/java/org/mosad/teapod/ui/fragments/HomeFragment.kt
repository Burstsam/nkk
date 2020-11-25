package org.mosad.teapod.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mosad.teapod.MainActivity
import org.mosad.teapod.databinding.FragmentHomeBinding
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.StorageController
import org.mosad.teapod.util.adapter.MediaItemAdapter
import org.mosad.teapod.util.decoration.MediaItemDecoration

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapterMyList: MediaItemAdapter
    private lateinit var adapterNewEpisodes: MediaItemAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                context?.let {
                    binding.recyclerMyList.addItemDecoration(MediaItemDecoration(9))

                    updateMyListMedia()

                    adapterNewEpisodes = MediaItemAdapter(AoDParser.newEpisodesList)
                    binding.recyclerNewEpisodes.adapter = adapterNewEpisodes
                    binding.recyclerNewEpisodes.addItemDecoration(MediaItemDecoration(9))

                    initActions()
                }
            }
        }
    }

    // TODO recreating the adapter on list change is not a good solution
    fun updateMyListMedia() {
        val myListMedia = StorageController.myList.map { elementId ->
            AoDParser.itemMediaList.first {
                elementId == it.id
            }
        }

        adapterMyList = MediaItemAdapter(myListMedia)
        adapterMyList.onItemClick = { mediaId, _ ->
            (activity as MainActivity).showMediaFragment(mediaId)
        }

        binding.recyclerMyList.adapter = adapterMyList
    }

    private fun initActions() {
        adapterNewEpisodes.onItemClick = { mediaId, _ ->
            (activity as MainActivity).showMediaFragment(mediaId)
        }
    }
}