package org.mosad.teapod.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_library.*
import kotlinx.coroutines.*
import org.mosad.teapod.MainActivity
import org.mosad.teapod.R
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.CustomAdapter
import org.mosad.teapod.util.Media

class LibraryFragment : Fragment() {

    private lateinit var adapter : CustomAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GlobalScope.launch {
            if (AoDParser.mediaList.isEmpty()) {
                AoDParser().listAnimes()
            }

            // create and set the adapter, needs context
            withContext(Dispatchers.Main) {
                adapter = CustomAdapter(requireContext(), AoDParser.mediaList)
                list_library.adapter = adapter
            }
        }

        initActions()
    }

    private fun initActions() {
        list_library.setOnItemClickListener { _, _, position, _ ->
            val media = adapter.getItem(position) as Media
            println("selected item is: ${media.title}")

            val mainActivity = activity as MainActivity
            mainActivity.showDetailFragment(media)
        }
    }

}