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

package org.mosad.teapod.util.tmdb

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.invoke
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.util.concatenate

/**
 * Controller for tmdb api integration.
 * Data types are in TMDBDataTypes. For the type definitions see:
 * https://developers.themoviedb.org/3/getting-started/introduction
 *
 */
class TMDBApiController {
    private val classTag = javaClass.name

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
        }
    }

    private val apiUrl = "https://api.themoviedb.org/3"
    private val apiKey = "de959cf9c07a08b5ca7cb51cda9a40c2"

    companion object{
        const val imageUrl = "https://image.tmdb.org/t/p/w500"
    }

    private suspend inline fun <reified T> request(
        endpoint: String,
        parameters: List<Pair<String, Any?>> = emptyList()
    ): T = coroutineScope {
        val path = "$apiUrl$endpoint"
        val params = concatenate(
            listOf("api_key" to apiKey, "language" to Preferences.preferredLocale.language),
            parameters
        )

        // TODO handle FileNotFoundException
        return@coroutineScope (Dispatchers.IO) {
            val response: HttpResponse = client.get(path) {
                params.forEach {
                    parameter(it.first, it.second)
                }
            }

            response.receive<T>()
        }
    }

    /**
     * Search for a movie in tmdb
     * @param query The query text (movie title)
     * @return A TMDBSearch<TMDBSearchResultMovie> object, or
     * NoneTMDBSearchMovie if nothing was found
     */
    suspend fun searchMovie(query: String): TMDBSearch<TMDBSearchResultMovie> {
        val searchEndpoint = "/search/multi"
        val parameters = listOf("query" to query, "include_adult" to false)

        return try {
            request(searchEndpoint, parameters)
        }catch (ex: SerializationException) {
            Log.e(classTag, "SerializationException in searchMovie(), with query = $query.", ex)
            NoneTMDBSearchMovie
        }
    }

    /**
     * Search for a tv show in tmdb
     * @param query The query text (tv show title)
     * @return A TMDBSearch<TMDBSearchResultTVShow> object, or
     * NoneTMDBSearchTVShow if nothing was found
     */
    suspend fun searchTVShow(query: String): TMDBSearch<TMDBSearchResultTVShow> {
        val searchEndpoint = "/search/tv"
        val parameters = listOf("query" to query, "include_adult" to false)

        return try {
            request(searchEndpoint, parameters)
        }catch (ex: SerializationException) {
            Log.e(classTag, "SerializationException in searchTVShow(), with query = $query.", ex)
            NoneTMDBSearchTVShow
        }
    }

    /**
     * Get details for a movie from tmdb
     * @param movieId The tmdb ID of the movie
     * @return A TMDBMovie object, or NoneTMDBMovie if not found
     */
    suspend fun getMovieDetails(movieId: Int): TMDBMovie {
        val movieEndpoint = "/movie/$movieId"

        // TODO is FileNotFoundException handling needed?
        return try {
            request(movieEndpoint)
        }catch (ex: SerializationException) {
            Log.e(classTag, "SerializationException in getMovieDetails(), with movieId = $movieId.", ex)
            NoneTMDBMovie
        }
    }

    /**
     * Get details for a tv show from tmdb
     * @param tvId The tmdb ID of the tv show
     * @return A TMDBTVShow object, or NoneTMDBTVShow if not found
     */
    suspend fun getTVShowDetails(tvId: Int): TMDBTVShow {
        val tvShowEndpoint = "/tv/$tvId"

        // TODO is FileNotFoundException handling needed?
        return try {
            request(tvShowEndpoint)
        }catch (ex: SerializationException) {
            Log.e(classTag, "SerializationException in getTVShowDetails(), with tvId = $tvId.", ex)
            NoneTMDBTVShow
        }
    }

    @Suppress("unused")
    /**
     * Get details for a tv show season from tmdb
     * @param tvId The tmdb ID of the tv show
     * @param seasonNumber The tmdb season number
     * @return A TMDBTVSeason object, or NoneTMDBTVSeason if not found
     */
    suspend fun getTVSeasonDetails(tvId: Int, seasonNumber: Int): TMDBTVSeason {
        val tvShowSeasonEndpoint = "/tv/$tvId/season/$seasonNumber"

        // TODO is FileNotFoundException handling needed?
        return try {
            request(tvShowSeasonEndpoint)
        }catch (ex: SerializationException) {
            Log.e(classTag, "SerializationException in getTVSeasonDetails(), with tvId = $tvId, seasonNumber = $seasonNumber.", ex)
            NoneTMDBTVSeason
        }
    }

}
