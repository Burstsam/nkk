package org.mosad.teapod.ui.activity.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.mosad.teapod.databinding.FragmentSearchBinding
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.decoration.MediaItemDecoration
import org.mosad.teapod.util.adapter.MediaItemAdapter
import org.mosad.teapod.util.showFragment

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private var adapter : MediaItemAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            // create and set the adapter, needs context
                context?.let {
                    adapter = MediaItemAdapter(AoDParser.guiMediaList)
                    adapter!!.onItemClick = { mediaId, _ ->
                        binding.searchText.clearFocus()
                        activity?.showFragment(MediaFragment("")) //(mediaId))
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