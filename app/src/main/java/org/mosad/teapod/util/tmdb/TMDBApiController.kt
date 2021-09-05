/**
 * Teapod
 *
 * Copyright 2020-2021  <seil0@mosad.xyz>
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
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import org.mosad.teapod.util.DataTypes.MediaType
import java.io.FileNotFoundException
import java.net.URL
import java.net.URLEncoder

/**
 * Controller for tmdb api integration.
 * Data types are in TMDBDataTypes. For the type definitions see:
 * https://developers.themoviedb.org/3/getting-started/introduction
 *
 * TODO evaluate Klaxon
 */
class TMDBApiController {

    private val apiUrl = "https://api.themoviedb.org/3"
    private val searchMovieUrl = "$apiUrl/search/movie"
    private val searchTVUrl = "$apiUrl/search/tv"
    private val detailsMovieUrl = "$apiUrl/movie"
    private val detailsTVUrl = "$apiUrl/tv"
    private val apiKey = "de959cf9c07a08b5ca7cb51cda9a40c2"
    private val language = "de"
    private val preparedParameters = "?api_key=$apiKey&language=$language"

    companion object{
        const val imageUrl = "https://image.tmdb.org/t/p/w500"
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    /**
     * Search for a media(movie or tv show) in tmdb
     * @param query The query text
     * @param type The media type (movie or tv show)
     * @return The media tmdb id, or -1 if not found
     */
    suspend fun search(query: String, type: MediaType): Int = withContext(Dispatchers.IO) {
        val searchUrl = when (type) {
            MediaType.MOVIE -> searchMovieUrl
            MediaType.TVSHOW -> searchTVUrl
            else -> {
                Log.e(javaClass.name, "Wrong Type: $type")
                return@withContext -1
            }
        }

        val url = URL("$searchUrl$preparedParameters&query=${URLEncoder.encode(query, "UTF-8")}")
        val response = JsonParser.parseString(url.readText()).asJsonObject
        val sortedResults = response.get("results").asJsonArray.toList().sortedBy {
            it.asJsonObject.get("title")?.asString
        }

        return@withContext sortedResults.firstOrNull()?.asJsonObject?.get("id")?.asInt ?: -1
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    /**
     * Get details for a movie from tmdb
     * @param movieId The tmdb ID of the movie
     * @return A tmdb movie object, or null if not found
     */
    suspend fun getMovieDetails(movieId: Int): TMDBMovie? = withContext(Dispatchers.IO) {
        val url = URL("$detailsMovieUrl/$movieId?api_key=$apiKey&language=$language")

        return@withContext try {
            val json = url.readText()
            Gson().fromJson(json, TMDBMovie::class.java)
        } catch (ex: FileNotFoundException) {
            Log.w(javaClass.name, "Waring: The requested media was not found. Requested ID: $movieId", ex)
            null
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    /**
     * Get details for a tv show from tmdb
     * @param tvId The tmdb ID of the tv show
     * @return A tmdb tv show object, or null if not found
     */
    suspend fun getTVShowDetails(tvId: Int): TMDBTVShow? = withContext(Dispatchers.IO) {
        val url = URL("$detailsTVUrl/$tvId?api_key=$apiKey&language=$language")

        return@withContext try {
            val json = url.readText()
            Gson().fromJson(json, TMDBTVShow::class.java)
        } catch (ex: FileNotFoundException) {
            Log.w(javaClass.name, "Waring: The requested media was not found. Requested ID: $tvId", ex)
            null
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    /**
     * Get details for a tv show season from tmdb
     * @param tvId The tmdb ID of the tv show
     * @param seasonNumber The tmdb season number
     * @return A tmdb tv season object, or null if not found
     */
    suspend fun getTVSeasonDetails(tvId: Int, seasonNumber: Int): TMDBTVSeason? = withContext(Dispatchers.IO) {
        val url = URL("$detailsTVUrl/$tvId/season/$seasonNumber?api_key=$apiKey&language=$language")

        return@withContext try {
            val json = url.readText()
            Gson().fromJson(json, TMDBTVSeason::class.java)
        } catch (ex: FileNotFoundException) {
            Log.w(javaClass.name, "Waring: The requested media was not found. Requested ID: $tvId, Season: $seasonNumber", ex)
            null
        }
    }

}