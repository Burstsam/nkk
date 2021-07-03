package org.mosad.teapod.util

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import org.mosad.teapod.util.DataTypes.MediaType
import java.net.URL
import java.net.URLEncoder

class TMDBApiController {

    private val apiUrl = "https://api.themoviedb.org/3"
    private val searchMovieUrl = "$apiUrl/search/movie"
    private val searchTVUrl = "$apiUrl/search/tv"
    private val getMovieUrl = "$apiUrl/movie"
    private val apiKey = "de959cf9c07a08b5ca7cb51cda9a40c2"
    private val language = "de"
    private val preparedParameters = "?api_key=$apiKey&language=$language"

    private val imageUrl = "https://image.tmdb.org/t/p/w500"

    suspend fun search(title: String, type: MediaType): TMDBResponse {
        // remove unneeded text from the media title before searching
        val searchTerm = title.replace("(Sub)", "")
            .replace(Regex("-?\\s?[0-9]+.\\s?(Staffel|Season)"), "")
            .replace(Regex("(Staffel|Season)\\s?[0-9]+"), "")
            .trim()

        return when (type) {
            MediaType.MOVIE -> searchMovie(searchTerm)
            MediaType.TVSHOW -> searchTVShow(searchTerm)
            else -> {
                Log.e(javaClass.name, "Wrong Type: $type")
                TMDBResponse()
            }
        }

    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun searchTVShow(title: String): TMDBResponse = withContext(Dispatchers.IO) {
        val url = URL("$searchTVUrl$preparedParameters&query=${URLEncoder.encode(title, "UTF-8")}")
        val response = JsonParser.parseString(url.readText()).asJsonObject
//        println(response)

        val sortedResults = response.get("results").asJsonArray.toList().sortedBy {
            getStringNotNull(it.asJsonObject, "name")
        }

        return@withContext if (sortedResults.isNotEmpty()) {
            sortedResults.first().asJsonObject.let {
                val id = getStringNotNull(it, "id").toInt()
                val overview = getStringNotNull(it, "overview")
                val posterPath = getStringNotNullPrefix(it, "poster_path", imageUrl)
                val backdropPath = getStringNotNullPrefix(it, "backdrop_path", imageUrl)

                TMDBResponse(id, "", overview, posterPath, backdropPath)
            }
        } else {
            TMDBResponse()
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun searchMovie(title: String): TMDBResponse = withContext(Dispatchers.IO) {
        val url = URL("$searchMovieUrl$preparedParameters&query=${URLEncoder.encode(title, "UTF-8")}")
        val response = JsonParser.parseString(url.readText()).asJsonObject
//        println(response)

        val sortedResults = response.get("results").asJsonArray.toList().sortedBy {
            getStringNotNull(it.asJsonObject, "name")
        }

        return@withContext if (sortedResults.isNotEmpty()) {
            sortedResults.first().asJsonObject.let {
                val id = getStringNotNull(it,"id").toInt()
                val overview = getStringNotNull(it,"overview")
                val posterPath = getStringNotNullPrefix(it, "poster_path", imageUrl)
                val backdropPath = getStringNotNullPrefix(it, "backdrop_path", imageUrl)
                val runtime = getMovieRuntime(id)

                TMDBResponse(id, "", overview, posterPath, backdropPath, runtime)
            }
        } else {
            TMDBResponse()
        }
    }

    /**
     * currently only used for runtime, need a rework
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getMovieRuntime(id: Int): Int = withContext(Dispatchers.IO) {
        val url = URL("$getMovieUrl/$id?api_key=$apiKey&language=$language")

        val response = JsonParser.parseString(url.readText()).asJsonObject
        return@withContext getStringNotNull(response,"runtime").toInt()
    }

    /**
     * return memberName as string if it's not JsonNull,
     * else return an empty string
     */
    private fun getStringNotNull(jsonObject: JsonObject, memberName: String): String {
        return getStringNotNullPrefix(jsonObject, memberName, "")
    }

    /**
     * return memberName as string with a prefix if it's not JsonNull,
     * else return an empty string
     */
    private fun getStringNotNullPrefix(jsonObject: JsonObject, memberName: String, prefix: String): String {
        return if (!jsonObject.get(memberName).isJsonNull) {
            prefix + jsonObject.get(memberName).asString
        } else {
            ""
        }
    }

}