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

package org.mosad.teapod.util.metadb

import android.util.Log
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object MetaDBController {
    private val TAG = javaClass.name

    private const val repoUrl = "https://gitlab.com/Seil0/teapodmetadb/-/raw/main/crunchy/"

    private val client = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(Json)
        }
    }

    private var mediaList = MediaList(listOf())
    private var metaCacheList = arrayListOf<Meta>()

    suspend fun list() = withContext(Dispatchers.IO) {
        val raw: String = client.get("$repoUrl/list.json")
        mediaList = Json.decodeFromString(raw)
    }

    /**
     * Get the meta data for a movie from MetaDB
     * @param crSeriesId The crunchyroll media id
     * @return A meta object, or null if not found
     */
    suspend fun getTVShowMetadata(crSeriesId: String): TVShowMeta? {
        return if (mediaList.media.contains(crSeriesId)) {
            metaCacheList.firstOrNull {
                it.crSeriesId == crSeriesId
            } as TVShowMeta? ?: getTVShowMetadataFromDB(crSeriesId)
        } else {
            null
        }
    }

    private suspend fun getTVShowMetadataFromDB(crSeriesId: String): TVShowMeta? = withContext(Dispatchers.IO) {
        return@withContext try {
            val raw: String = client.get("$repoUrl/tv/$crSeriesId/media.json")
            val meta: TVShowMeta = Json.decodeFromString(raw)
            metaCacheList.add(meta)

            meta
        } catch (ex: ClientRequestException) {
            when (ex.response.status) {
                HttpStatusCode.NotFound -> Log.w(TAG, "The requested file was not found. Series ID: $crSeriesId", ex)
                else -> Log.e(TAG, "Error while requesting meta data. Series ID: $crSeriesId", ex)
            }

            null // todo return none object
        }
    }

}
