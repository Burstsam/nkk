/**
 * Teapod
 *
 * Copyright 2020-2022  <seil0@mosad.xyz>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 *
 */

package org.mosad.teapod.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import java.io.FileNotFoundException
import java.net.URL

class MetaDBController {

    companion object {
        private const val repoUrl = "https://gitlab.com/Seil0/teapodmetadb/-/raw/main/aod/"

        var mediaList = MediaList(listOf())
        private var metaCacheList = arrayListOf<Meta>()

        @Suppress("BlockingMethodInNonBlockingContext")
        suspend fun list() = withContext(Dispatchers.IO) {
            val url = URL("$repoUrl/list.json")
            val json = url.readText()

            mediaList = Gson().fromJson(json, MediaList::class.java)
        }
    }

    /**
     * Get the meta data for a movie from MetaDB
     * @param aodId The AoD id of the media
     * @return A meta movie object, or null if not found
     */
    suspend fun getMovieMetadata(aodId: Int): MovieMeta? {
        return metaCacheList.firstOrNull {
            it.aodId == aodId
        } as MovieMeta? ?: getMovieMetadataFromDB(aodId)
    }

    /**
     * Get the meta data for a tv show from MetaDB
     * @param aodId The AoD id of the media
     * @return A meta tv show object, or null if not found
     */
    suspend fun getTVShowMetadata(aodId: Int): TVShowMeta? {
        return metaCacheList.firstOrNull {
            it.aodId == aodId
        } as TVShowMeta? ?: getTVShowMetadataFromDB(aodId)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun getMovieMetadataFromDB(aodId: Int): MovieMeta? = withContext(Dispatchers.IO) {
        val url = URL("$repoUrl/movie/$aodId/media.json")
        return@withContext try {
            val json = url.readText()
            val meta = Gson().fromJson(json, MovieMeta::class.java)
            metaCacheList.add(meta)

            meta
        } catch (ex: FileNotFoundException) {
            Log.w(javaClass.name, "Waring: The requested file was not found. Requested ID: $aodId", ex)
            null
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun getTVShowMetadataFromDB(aodId: Int): TVShowMeta? = withContext(Dispatchers.IO) {
        val url = URL("$repoUrl/tv/$aodId/media.json")
        return@withContext try {
            val json = url.readText()
            val meta = Gson().fromJson(json, TVShowMeta::class.java)
            metaCacheList.add(meta)

            meta
        } catch (ex: FileNotFoundException) {
            Log.w(javaClass.name, "Waring: The requested file was not found. Requested ID: $aodId", ex)
            null
        }
    }

}

// class representing the media list json object
data class MediaList(
    val media: List<Int>
)

// abstract class used for meta data objects (tv, movie)
abstract class Meta {
    abstract val id: Int
    abstract val aodId: Int
    abstract val tmdbId: Int
}

// class representing the movie json object
data class MovieMeta(
    override val id: Int,
    @SerializedName("aod_id")
    override val aodId: Int,
    @SerializedName("tmdb_id")
    override val tmdbId: Int
): Meta()

// class representing the tv show json object
data class TVShowMeta(
    override val id: Int,
    @SerializedName("aod_id")
    override val aodId: Int,
    @SerializedName("tmdb_id")
    override val tmdbId: Int,
    @SerializedName("tmdb_season_id")
    val tmdbSeasonId: Int,
    @SerializedName("tmdb_season_number")
    val tmdbSeasonNumber: Int,
    @SerializedName("episodes")
    val episodes: List<EpisodeMeta>
): Meta()

// class used in TVShowMeta, part of the tv show json object
data class EpisodeMeta(
    val id: Int,
    @SerializedName("aod_media_id")
    val aodMediaId: Int,
    @SerializedName("tmdb_id")
    val tmdbId: Int,
    @SerializedName("tmdb_number")
    val tmdbNumber: Int,
    @SerializedName("opening_start")
    val openingStart: Long,
    @SerializedName("opening_duration")
    val openingDuration: Long,
    @SerializedName("ending_start")
    val endingStart: Long,
    @SerializedName("ending_duration")
    val endingDuration: Long
)
