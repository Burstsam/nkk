package org.mosad.teapod.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_library.*
import kotlinx.coroutines.*
import org.mosad.teapod.R
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.CustomAdapter
import org.mosad.teapod.util.GUIMedia

class LibraryFragment : Fragment() {

    private val parser = AoDParser()

    private var mediaList = arrayListOf<GUIMedia>()
    private lateinit var adapter : CustomAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GlobalScope.launch {
            mediaList = parser.listAnimes()

            // create and set the adapter, needs context
            withContext(Dispatchers.Main) {
                adapter = CustomAdapter(requireContext(), mediaList)
                list_library.adapter = adapter
                //adapter.notifyDataSetChanged()
            }
        }

        initActions()
    }

    private fun initActions() {
        list_library.setOnItemClickListener { parent, view, position, id ->
            println("selected item is: ${mediaList[position]}")
            showDetailFragment(mediaList[position])
        }
    }

    private fun showDetailFragment(media: GUIMedia) {
        parser.loadDetails(media.link)

        println("done")
    }

}