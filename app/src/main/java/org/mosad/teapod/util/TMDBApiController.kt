package org.mosad.teapod.util

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.net.URL
import java.net.URLEncoder
import org.mosad.teapod.util.DataTypes.MediaType

class TMDBApiController {

    private val apiUrl = "https://api.themoviedb.org/3"
    private val searchMovieUrl = "$apiUrl/search/movie"
    private val searchTVUrl = "$apiUrl/search/tv"
    private val getMovieUrl = "$apiUrl/movie"
    private val apiKey = "de959cf9c07a08b5ca7cb51cda9a40c2"
    private val language = "de"
    private val preparedParameters = "?api_key=$apiKey&language=$language"

    private val imageUrl = "https://image.tmdb.org/t/p/w500"

    fun search(title: String, type: MediaType): TMDBResponse {
        val searchTerm = title.replace("(Sub)", "").trim()

        return when (type) {
            MediaType.MOVIE -> searchMovie(searchTerm)
            MediaType.TVSHOW -> searchTVShow(searchTerm)
            else -> {
                Log.e(javaClass.name, "Wrong Type: $type")
                TMDBResponse()
            }
        }

    }

    fun searchTVShow(title: String) = runBlocking {
        val url = URL("$searchTVUrl$preparedParameters&query=${URLEncoder.encode(title, "UTF-8")}")

        GlobalScope.async {
            val response = JsonParser.parseString(url.readText()).asJsonObject
            //println(response)

            return@async if (response.get("total_results").asInt > 0) {
                response.get("results").asJsonArray.first().asJsonObject.let {
                    val id = getStringNotNull(it,"id").toInt()
                    val overview = getStringNotNull(it,"overview")
                    val posterPath = getStringNotNullPrefix(it, "poster_path", imageUrl)
                    val backdropPath = getStringNotNullPrefix(it, "backdrop_path", imageUrl)

                    TMDBResponse(id, "", overview, posterPath, backdropPath)
                }
            } else {
                TMDBResponse()
            }
        }.await()

    }

    fun searchMovie(title: String) = runBlocking {
        val url = URL("$searchMovieUrl$preparedParameters&query=${URLEncoder.encode(title, "UTF-8")}")

        GlobalScope.async {
            val response = JsonParser.parseString(url.readText()).asJsonObject
            //println(response)

            return@async if (response.get("total_results").asInt > 0) {
                response.get("results").asJsonArray.first().asJsonObject.let {
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


        }.await()
    }

    /**
     * currently only used for runtime, need a rework
     */
    fun getMovieRuntime(id: Int): Int = runBlocking {
        val url = URL("$getMovieUrl/$id?api_key=$apiKey&language=$language")

        GlobalScope.async {
            val response = JsonParser.parseString(url.readText()).asJsonObject

            return@async getStringNotNull(response,"runtime").toInt()
        }.await()
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