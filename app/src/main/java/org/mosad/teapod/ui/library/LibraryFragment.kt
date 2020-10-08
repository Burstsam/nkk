package org.mosad.teapod.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_library.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mosad.teapod.R
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.ui.components.MediaLinearLayout
import org.mosad.teapod.util.CustomAdapter
import org.mosad.teapod.util.GUIMedia

class LibraryFragment : Fragment() {

    private var mediaList = arrayListOf<GUIMedia>()
    private lateinit var adapter : CustomAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root =  inflater.inflate(R.layout.fragment_library, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        adapter = CustomAdapter(requireContext(),layoutInflater, mediaList)//ArrayAdapter(requireContext(), R.layout.linear_media, R.id.text_title, mediaList)

        list_library.adapter = adapter

        text_dashboard.text = "Loading Animes ..."

        loadAnimeList()
    }

    private fun loadAnimeList() = GlobalScope.launch {

        val parser = AoDParser()
        mediaList = parser.listAnime()

        text_dashboard.text = "got ${mediaList.size} animes"

        withContext(Dispatchers.Main) {
            adapter.notifyDataSetChanged()
            println("notifiyed")
        }

    }

}