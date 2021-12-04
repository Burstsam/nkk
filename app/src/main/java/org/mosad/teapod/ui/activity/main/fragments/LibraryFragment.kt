package org.mosad.teapod.ui.activity.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.mosad.teapod.databinding.FragmentLibraryBinding
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.parser.crunchyroll.Crunchyroll
import org.mosad.teapod.util.ItemMedia
import org.mosad.teapod.util.adapter.MediaItemAdapter
import org.mosad.teapod.util.decoration.MediaItemDecoration
import org.mosad.teapod.util.showFragment

class LibraryFragment : Fragment() {

    private lateinit var binding: FragmentLibraryBinding
    private lateinit var adapter: MediaItemAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // init async
        lifecycleScope.launch {
            // create and set the adapter, needs context
            context?.let {
                // crunchy testing TODO implement lazy loading
                val results = Crunchyroll.browse(n = 50)
                val list = results.items.mapIndexed { index, item ->
                    ItemMedia(index, item.title, item.images.poster_wide[0][0].source)
                }


                adapter = MediaItemAdapter(list)
                adapter.onItemClick = { mediaId, _ ->
                    activity?.showFragment(MediaFragment(mediaId))
                }

                binding.recyclerMediaLibrary.adapter = adapter
                binding.recyclerMediaLibrary.addItemDecoration(MediaItemDecoration(9))
            }

        }
    }
}