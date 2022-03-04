package org.mosad.teapod.util

import android.widget.TextView
import org.mosad.teapod.parser.crunchyroll.Collection
import org.mosad.teapod.parser.crunchyroll.ContinueWatchingItem
import org.mosad.teapod.parser.crunchyroll.Item
import java.util.*

fun TextView.setDrawableTop(drawable: Int) {
    this.setCompoundDrawablesWithIntrinsicBounds(0, drawable, 0, 0)
}

fun <T> concatenate(vararg lists: List<T>): List<T> {
    return listOf(*lists).flatten()
}

// TODO move to correct location
fun Collection<Item>.toItemMediaList(): List<ItemMedia> {
    return this.items.map {
        ItemMedia(it.id, it.title, it.images.poster_wide[0][0].source)
    }
}

@JvmName("toItemMediaListContinueWatchingItem")
fun Collection<ContinueWatchingItem>.toItemMediaList(): List<ItemMedia> {
    return this.items.map {
        ItemMedia(it.panel.episodeMetadata.seriesId, it.panel.title, it.panel.images.thumbnail[0][0].source)
    }
}

fun Locale.toDisplayString(fallback: String): String {
    return if (this.displayLanguage.isNotEmpty() && this.displayCountry.isNotEmpty()) {
        "${this.displayLanguage} (${this.displayCountry})"
    } else if (this.displayCountry.isNotEmpty()) {
        this.displayLanguage
    } else {
        fallback
    }
}
