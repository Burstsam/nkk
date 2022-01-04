package org.mosad.teapod.util

import android.widget.TextView
import org.mosad.teapod.parser.crunchyroll.Collection
import org.mosad.teapod.parser.crunchyroll.ContinueWatchingList

fun TextView.setDrawableTop(drawable: Int) {
    this.setCompoundDrawablesWithIntrinsicBounds(0, drawable, 0, 0)
}

fun <T> concatenate(vararg lists: List<T>): List<T> {
    return listOf(*lists).flatten()
}

// TODO move to correct location
fun Collection.toItemMediaList(): List<ItemMedia> {
    return this.items.map {
        ItemMedia(it.id, it.title, it.images.poster_wide[0][0].source)
    }
}

fun ContinueWatchingList.toItemMediaList(): List<ItemMedia> {
    return this.items.map {
        // TODO add season and episode to title
        ItemMedia(it.panel.episodeMetadata.seriesId, it.panel.title, it.panel.images.thumbnail[0][0].source)

    }
}
