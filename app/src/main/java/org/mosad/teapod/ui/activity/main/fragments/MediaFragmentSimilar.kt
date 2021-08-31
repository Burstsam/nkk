package org.mosad.teapod.ui.activity.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.mosad.teapod.databinding.FragmentMediaSimilarBinding
import org.mosad.teapod.ui.activity.main.viewmodel.MediaFragmentViewModel
import org.mosad.teapod.util.adapter.MediaItemAdapter
import org.mosad.teapod.util.decoration.MediaItemDecoration
import org.mosad.teapod.util.showFragment

class MediaFragmentSimilar : Fragment()  {

    private lateinit var binding: FragmentMediaSimilarBinding
    private val model: MediaFragmentViewModel by activityViewModels()

    private lateinit var adapterSimilar: MediaItemAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMediaSimilarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapterSimilar = MediaItemAdapter(model.media2.similar)
        binding.recyclerMediaSimilar.adapter = adapterSimilar
        binding.recyclerMediaSimilar.addItemDecoration(MediaItemDecoration(9))

        // set onItemClick only in adapter is initialized
        if (this::adapterSimilar.isInitialized) {
            adapterSimilar.onItemClick = { mediaId, _ ->
                activity?.showFragment(MediaFragment(mediaId))
            }
        }
    }
}