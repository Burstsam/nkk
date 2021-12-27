package org.mosad.teapod.ui.activity.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.mosad.teapod.databinding.FragmentLibraryBinding
import org.mosad.teapod.parser.crunchyroll.Crunchyroll
import org.mosad.teapod.util.ItemMedia
import org.mosad.teapod.util.adapter.MediaItemAdapter
import org.mosad.teapod.util.decoration.MediaItemDecoration
import org.mosad.teapod.util.showFragment

class LibraryFragment : Fragment() {

    private lateinit var binding: FragmentLibraryBinding
    private lateinit var adapter: MediaItemAdapter

    private val itemList = arrayListOf<ItemMedia>()
    private val pageSize = 30
    private var nextItemIndex = 0

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
                val initialResults = Crunchyroll.browse(n = pageSize)
                itemList.addAll(initialResults.items.map { item ->
                    ItemMedia(item.id, item.title, item.images.poster_wide[0][0].source)
                })
                nextItemIndex += pageSize

                adapter = MediaItemAdapter(itemList)
                adapter.onItemClick = { mediaIdStr, _ ->
                    activity?.showFragment(MediaFragment(mediaIdStr = mediaIdStr))
                }

                binding.recyclerMediaLibrary.adapter = adapter
                binding.recyclerMediaLibrary.addItemDecoration(MediaItemDecoration(9))
                // TODO replace with pagination3
                // https://medium.com/swlh/paging3-recyclerview-pagination-made-easy-333c7dfa8797
                binding.recyclerMediaLibrary.addOnScrollListener(PaginationScrollListener())
            }

        }
    }

    inner class PaginationScrollListener: RecyclerView.OnScrollListener() {
        private var isLoading = false

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager as GridLayoutManager?

            if (!isLoading) layoutManager?.let {
                // itemList.size - 5 to start loading a bit earlier than the actual end
                if (layoutManager.findLastCompletelyVisibleItemPosition() >= (itemList.size - 5)) {
                    // load new browse results async
                    isLoading = true
                    lifecycleScope.launch {
                        val firstNewItemIndex = itemList.lastIndex + 1
                        val results = Crunchyroll.browse(start = nextItemIndex, n = pageSize)
                        itemList.addAll(results.items.map { item ->
                            ItemMedia(item.id, item.title, item.images.poster_wide[0][0].source)
                        })
                        nextItemIndex += pageSize

                        adapter.updateMediaList(itemList)
                        adapter.notifyItemRangeInserted(firstNewItemIndex, pageSize)

                        isLoading = false
                    }
                }
            }
        }
    }

}