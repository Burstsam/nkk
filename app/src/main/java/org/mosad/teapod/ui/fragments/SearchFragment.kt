package org.mosad.teapod.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import org.mosad.teapod.MainActivity
import org.mosad.teapod.databinding.FragmentSearchBinding
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.decoration.MediaItemDecoration
import org.mosad.teapod.util.adapter.MediaItemAdapter

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private var adapter : MediaItemAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GlobalScope.launch {
            // create and set the adapter, needs context
            withContext(Dispatchers.Main) {
                context?.let {
                    adapter = MediaItemAdapter(AoDParser.itemMediaList)
                    adapter!!.onItemClick = { mediaId, _ ->
                        binding.searchText.clearFocus()
                        (activity as MainActivity).showMediaFragment(mediaId)
                    }

                    binding.recyclerMediaSearch.adapter = adapter
                    binding.recyclerMediaSearch.addItemDecoration(MediaItemDecoration(9))
                }
            }
        }

        initActions()
    }

    private fun initActions() {
        binding.searchText.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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