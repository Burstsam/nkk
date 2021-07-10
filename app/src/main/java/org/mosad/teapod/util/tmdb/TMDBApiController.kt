package org.mosad.teapod.util.tmdb

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import org.mosad.teapod.util.DataTypes.MediaType
import java.io.FileNotFoundException
import java.net.URL
import java.net.URLEncoder

// TODO use Klaxon?
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

        return@withContext sortedResults.first().asJsonObject?.get("id")?.asInt ?: -1
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getMovieDetails(movieId: Int): Movie = withContext(Dispatchers.IO) {
        val url = URL("$detailsMovieUrl/$movieId?api_key=$apiKey&language=$language")

        val response = try {
            JsonParser.parseString(url.readText()).asJsonObject
        } catch (ex: FileNotFoundException) {
            Log.w(javaClass.name, "The resource you requested could not be found")
            return@withContext Movie(-1)
        }

        return@withContext try {
            Movie(
                id = response.get("id").asInt,
                name = response.get("title")?.asString,
                overview = response.get("overview")?.asString,
                posterPath = response.get("poster_path")?.asString,
                backdropPath = response.get("backdrop_path")?.asString,
                releaseDate = response.get("release_date")?.asString,
                runtime = response.get("runtime")?.asInt
            )
        } catch (ex: Exception) {
            Log.w(javaClass.name, "Error", ex)
            Movie(-1)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getTVShowDetails(tvId: Int): TVShow = withContext(Dispatchers.IO) {
        val url = URL("$detailsTVUrl/$tvId?api_key=$apiKey&language=$language")

        val response = try {
            JsonParser.parseString(url.readText()).asJsonObject
        } catch (ex: FileNotFoundException) {
            Log.w(javaClass.name, "The resource you requested could not be found")
            return@withContext TVShow(-1)
        }

        return@withContext try {
            TVShow(
                id = response.get("id").asInt,
                name = response.get("name")?.asString,
                overview = response.get("overview")?.asString,
                posterPath = response.get("poster_path")?.asString,
                backdropPath = response.get("backdrop_path")?.asString,
                firstAirDate = response.get("first_air_date")?.asString,
                status = response.get("status")?.asString
            )
        } catch (ex: Exception) {
            Log.w(javaClass.name, "Error", ex)
            TVShow(-1)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getTVSeasonDetails(tvId: Int, seasonNumber: Int): TVSeason = withContext(Dispatchers.IO) {
        val url = URL("$detailsTVUrl/$tvId/season/$seasonNumber?api_key=$apiKey&language=$language")

        val response = try {
            JsonParser.parseString(url.readText()).asJsonObject
        } catch (ex: FileNotFoundException) {
            Log.w(javaClass.name, "The resource you requested could not be found")
            return@withContext TVSeason(-1)
        }
        // println(response)

        return@withContext try {
            val episodes = response.get("episodes").asJsonArray.map {
                TVEpisode(
                    id = it.asJsonObject.get("id").asInt,
                    name = it.asJsonObject.get("name")?.asString,
                    overview = it.asJsonObject.get("overview")?.asString,
                    airDate = it.asJsonObject.get("air_date")?.asString,
                    episodeNumber = it.asJsonObject.get("episode_number")?.asInt
                )
            }

            TVSeason(
                id = response.get("id").asInt,
                name = response.asJsonObject.get("name")?.asString,
                overview = response.asJsonObject.get("overview")?.asString,
                posterPath = response.asJsonObject.get("poster_path")?.asString,
                airDate = response.asJsonObject.get("air_date")?.asString,
                episodes = episodes,
                seasonNumber = response.get("season_number")?.asInt
            )
        } catch (ex: Exception) {
            Log.w(javaClass.name, "Error", ex)
            TVSeason(-1)
        }
    }

}