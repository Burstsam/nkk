package org.mosad.teapod.ui.activity.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.mosad.teapod.databinding.FragmentSearchBinding
import org.mosad.teapod.parser.crunchyroll.Crunchyroll
import org.mosad.teapod.util.ItemMedia
import org.mosad.teapod.util.adapter.MediaItemAdapter
import org.mosad.teapod.util.decoration.MediaItemDecoration
import org.mosad.teapod.util.showFragment

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var adapter: MediaItemAdapter

    private val itemList = arrayListOf<ItemMedia>()
    private var searchJob: Job? = null
    private var oldSearchQuery = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            // create and set the adapter, needs context
                context?.let {
                    adapter = MediaItemAdapter(itemList)
                    adapter.onItemClick = { mediaIdStr, _ ->
                        binding.searchText.clearFocus()
                        activity?.showFragment(MediaFragment(mediaIdStr))
                    }

                    binding.recyclerMediaSearch.adapter = adapter
                    binding.recyclerMediaSearch.addItemDecoration(MediaItemDecoration(9))
                }
        }

        initActions()
    }

    private fun initActions() {
        binding.searchText.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { search(it) }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { search(it) }
                return false
            }
        })
    }

    private fun search(query: String) {
        // if the query hasn't changed since the last successful search, return
        if (query == oldSearchQuery) return

        // cancel search job if one is already running
        if (searchJob?.isActive == true) searchJob?.cancel()

        searchJob = lifecycleScope.async {
            // TODO maybe wait a few ms (500ms?) before searching, if the user inputs any other chars
            val results = Crunchyroll.search(query, 50)

            itemList.clear() // TODO needs clean up

            // TODO add top results first heading
            itemList.addAll(results.items[0].items.map { item ->
                ItemMedia(item.id, item.title, item.images.poster_wide[0][0].source)
            })

            // TODO currently only tv shows are supported, hence only the first items array
            //  should be always present

//            // TODO add tv shows heading
//            if (results.items.size >= 2) {
//                itemList.addAll(results.items[1].items.map { item ->
//                    ItemMedia(item.id, item.title, item.images.poster_wide[0][0].source)
//                })
//            }
//
//            // TODO add movies heading
//            if (results.items.size >= 3) {
//                itemList.addAll(results.items[2].items.map { item ->
//                    ItemMedia(item.id, item.title, item.images.poster_wide[0][0].source)
//                })
//            }
//
//            // TODO add episodes heading
//            if (results.items.size >= 4) {
//                itemList.addAll(results.items[3].items.map { item ->
//                    ItemMedia(item.id, item.title, item.images.poster_wide[0][0].source)
//                })
//            }

            adapter.notifyDataSetChanged()
            //adapter.notifyItemRangeInserted(0, itemList.size)

            // after successfully searching the query term, add it as old query, to make sure we
            // don't search again if the query hasn't changed
            oldSearchQuery = query
        }
    }
}