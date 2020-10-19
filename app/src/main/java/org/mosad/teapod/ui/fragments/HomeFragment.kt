package org.mosad.teapod.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mosad.teapod.MainActivity
import org.mosad.teapod.R
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.StorageController
import org.mosad.teapod.util.adapter.MediaItemAdapter
import org.mosad.teapod.util.decoration.MediaItemDecoration

class HomeFragment : Fragment() {

    private lateinit var adapterMyList: MediaItemAdapter
    private lateinit var adapterNewEpisodes: MediaItemAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                context?.let {
                    recycler_my_list.addItemDecoration(MediaItemDecoration(9))

                    updateMyListMedia()

                    adapterNewEpisodes = MediaItemAdapter(AoDParser.newEpisodesList)
                    recycler_new_episodes.adapter = adapterNewEpisodes
                    recycler_new_episodes.addItemDecoration(MediaItemDecoration(9))

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

        recycler_my_list.adapter = adapterMyList
    }

    private fun initActions() {
        adapterNewEpisodes.onItemClick = { mediaId, _ ->
            (activity as MainActivity).showMediaFragment(mediaId)
        }
    }
}