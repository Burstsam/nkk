package org.mosad.teapod.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.*
import org.mosad.teapod.MainActivity
import org.mosad.teapod.R
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.CustomAdapter
import org.mosad.teapod.util.Media

class SearchFragment : Fragment() {

    private var adapter : CustomAdapter? = null

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
                    adapter = CustomAdapter(it, AoDParser.mediaList)
                    list_search.adapter = adapter
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

        list_search.setOnItemClickListener { _, _, position, _ ->
            search_text.clearFocus() // remove focus from the SearchView

            runBlocking {
                val media = adapter?.getItem(position) as Media
                println("selected item is: ${media.title}")

                (activity as MainActivity).showDetailFragment(media).join()
            }
        }
    }
}