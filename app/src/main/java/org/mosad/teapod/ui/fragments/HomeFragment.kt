package org.mosad.teapod.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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

    private lateinit var adapter: MediaItemAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GlobalScope.launch {
            if (AoDParser.mediaList.isEmpty()) {
                AoDParser().listAnimes()
            }

            withContext(Dispatchers.Main) {
                context?.let {
                    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    recycler_my_list.layoutManager = layoutManager
                    recycler_my_list.addItemDecoration(MediaItemDecoration(9))

                    updateMyListMedia()
                }
            }

        }
    }

    // TODO recreating the adapter on list change is not a good solution
    fun updateMyListMedia() {
        val myListMedia = StorageController.myList.map { listElement ->
            AoDParser.mediaList.first {
                listElement == it.link
            }
        }

        adapter = MediaItemAdapter(myListMedia)
        adapter.onItemClick = { media, _ ->
            (activity as MainActivity).showMediaFragment(media)
        }

        recycler_my_list.adapter = adapter
    }
}