package org.mosad.teapod.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.*
import org.mosad.teapod.MainActivity
import org.mosad.teapod.R
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.decoration.MediaItemDecoration
import org.mosad.teapod.util.adapter.MediaItemAdapter

class SearchFragment : Fragment() {

    private var adapter : MediaItemAdapter? = null
    private lateinit var layoutManager: GridLayoutManager


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GlobalScope.launch {
            if (AoDParser.mediaList.isEmpty()) {
                AoDParser().listAnimes()
            }

            // create and set the adapter, needs context
            withContext(Dispatchers.Main) {
                context?.let {
                    layoutManager = GridLayoutManager(context, 2)
                    adapter = MediaItemAdapter(AoDParser.mediaList)
                    adapter!!.onItemClick = { media, _ ->
                        (activity as MainActivity).showMediaFragment(media)
                    }

                    recycler_media_search.layoutManager = layoutManager
                    recycler_media_search.adapter = adapter
                    recycler_media_search.addItemDecoration(MediaItemDecoration(9))
                }
            }
        }

        initActions()
    }

    private fun initActions() {
        search_text.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                adapter?.filter?.filter(query)
                adapter?.notifyDataSetChanged()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter?.filter?.filter(newText)
                adapter?.notifyDataSetChanged()
                return false
            }
        })
    }
}