package org.mosad.teapod.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mosad.teapod.MainActivity
import org.mosad.teapod.databinding.FragmentLibraryBinding
import org.mosad.teapod.parser.AoDParser
import org.mosad.teapod.util.adapter.MediaItemAdapter
import org.mosad.teapod.util.decoration.MediaItemDecoration

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
        GlobalScope.launch {
            // create and set the adapter, needs context
            withContext(Dispatchers.Main) {
                context?.let {
                    adapter = MediaItemAdapter(AoDParser.itemMediaList)
                    adapter.onItemClick = { mediaId, _ ->
                        (activity as MainActivity).showFragment(MediaFragment(mediaId))
                    }

                    binding.recyclerMediaLibrary.adapter = adapter
                    binding.recyclerMediaLibrary.addItemDecoration(MediaItemDecoration(9))
                }
            }

        }
    }
}