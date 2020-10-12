package org.mosad.teapod.util

import android.util.Log
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
    private val apiKey = "de959cf9c07a08b5ca7cb51cda9a40c2"
    private val language = "de"
    private val preparedParamters = "?api_key=$apiKey&language=$language"

    private val imageUrl = "https://image.tmdb.org/t/p/w500"

    fun search(title: String, type: MediaType): TMDBResponse {
        return when (type) {
            MediaType.MOVIE -> {
                val test = searchMovie(title)
                println("test: $test")
                test
            }
            MediaType.TVSHOW -> {
                val test = searchTVShow(title)
                println("test: $test")
                test
            }
            MediaType.OTHER -> {
                Log.e(javaClass.name, "Error")
                TMDBResponse()
            }
        }

    }

    fun searchTVShow(title: String) = runBlocking {
        val url = URL("$searchTVUrl$preparedParamters&query=${URLEncoder.encode(title, "UTF-8")}")

        GlobalScope.async {
            val response = JsonParser.parseString(url.readText()).asJsonObject
            println(response)

            return@async if (response.get("total_results").asInt > 0) {
                response.get("results").asJsonArray.first().let {
                    val overview = it.asJsonObject.get("overview").asString
                    val posterPath = imageUrl + it.asJsonObject.get("poster_path").asString
                    val backdropPath = imageUrl + it.asJsonObject.get("backdrop_path").asString

                    TMDBResponse("", overview, posterPath, backdropPath)
                }
            } else {
                TMDBResponse()
            }
        }.await()

    }

    fun searchMovie(title: String) = runBlocking {
        val url = URL("$searchMovieUrl$preparedParamters&query=${URLEncoder.encode(title, "UTF-8")}")

        GlobalScope.async {
            val response = JsonParser.parseString(url.readText()).asJsonObject
            println(response)

            return@async if (response.get("total_results").asInt > 0) {
                response.get("results").asJsonArray.first().let {
                    val overview = it.asJsonObject.get("overview").asString
                    val posterPath = imageUrl + it.asJsonObject.get("poster_path").asString
                    val backdropPath = imageUrl + it.asJsonObject.get("backdrop_path").asString

                    TMDBResponse("", overview, posterPath, backdropPath)
                }
            } else {
                TMDBResponse()
            }


        }.await()
    }

}