package org.mosad.teapod.util.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import org.mosad.teapod.R
import org.mosad.teapod.databinding.ItemEpisodeBinding
import org.mosad.teapod.parser.crunchyroll.Episode
import org.mosad.teapod.parser.crunchyroll.PlayheadsMap
import org.mosad.teapod.util.tmdb.TMDBTVEpisode

class EpisodeItemAdapter(
    private val episodes: List<Episode>,
    private val tmdbEpisodes: List<TMDBTVEpisode>?,
    private val playheads: PlayheadsMap
) : RecyclerView.Adapter<EpisodeItemAdapter.EpisodeViewHolder>() {

    var onImageClick: ((seasonId: String, episodeId: String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        return EpisodeViewHolder(ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val context = holder.binding.root.context
        val ep = episodes[position]

        val titleText = if (ep.episodeNumber != null) {
            // for tv shows add ep prefix and episode number
            if (ep.isDubbed) {
                context.getString(R.string.component_episode_title, ep.episode, ep.title)
            } else {
                context.getString(R.string.component_episode_title_sub, ep.episode, ep.title)
            }
        } else {
            ep.title
        }

        holder.binding.textEpisodeTitle.text = titleText
        holder.binding.textEpisodeDesc.text = if (ep.description.isNotEmpty()) {
            ep.description
        } else if (tmdbEpisodes != null && position < tmdbEpisodes.size){
            tmdbEpisodes[position].overview
        } else {
            ""
        }

        // TODO is isNotEmpty() needed? also in PlayerEpisodeItemAdapter
        if (ep.images.thumbnail[0][0].source.isNotEmpty()) {
            Glide.with(context).load(ep.images.thumbnail[0][0].source)
                .apply(RequestOptions.placeholderOf(ColorDrawable(Color.DKGRAY)))
                .apply(RequestOptions.bitmapTransform(RoundedCornersTransformation(10, 0)))
                .into(holder.binding.imageEpisode)
        }

        // add watched icon to episode, if the episode id is present in playheads and fullyWatched
        val watchedImage: Drawable? = if (playheads[ep.id]?.fullyWatched == true) {
            ContextCompat.getDrawable(context, R.drawable.ic_baseline_check_circle_24)
        } else {
            null
        }
        holder.binding.imageWatched.setImageDrawable(watchedImage)
    }

    override fun getItemCount(): Int {
        return episodes.size
    }

    inner class EpisodeViewHolder(val binding: ItemEpisodeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            // on image click return the episode id and index (within the adapter)
            binding.imageEpisode.setOnClickListener {
                onImageClick?.invoke(
                    episodes[bindingAdapterPosition].seasonId,
                    episodes[bindingAdapterPosition].id
                )
            }
        }
    }
}