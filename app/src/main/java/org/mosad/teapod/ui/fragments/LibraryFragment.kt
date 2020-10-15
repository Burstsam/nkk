package org.mosad.teapod.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.fragment_library.*
import kotlinx.coroutines.*
import org.mosad.teapod.MainActivity
import org.mosad.teapod.R
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.decoration.MediaItemDecoration
import org.mosad.teapod.util.adapter.MediaItemAdapter

class LibraryFragment : Fragment() {

    private lateinit var adapter: MediaItemAdapter
    private lateinit var layoutManager: GridLayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // init async
        GlobalScope.launch {
            if (AoDParser.mediaList.isEmpty()) {
                AoDParser().listAnimes()
            }

            // create and set the adapter, needs context
            withContext(Dispatchers.Main) {
                context?.let {
                    layoutManager = GridLayoutManager(context, 2)
                    adapter = MediaItemAdapter(AoDParser.mediaList)
                    adapter.onItemClick = { media, _ ->
                        (activity as MainActivity).showMediaFragment(media)
                    }

                    recycler_media_library.layoutManager = layoutManager
                    recycler_media_library.adapter = adapter
                    recycler_media_library.addItemDecoration(MediaItemDecoration(9))
                }
            }

        }
    }
}