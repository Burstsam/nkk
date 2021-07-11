package org.mosad.teapod.util

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

            Thread.sleep(5000)

            mediaList = Gson().fromJson(json, MediaList::class.java)
        }
    }

    suspend fun getMovieMetadata(aodId: Int): MovieMeta? {
        return metaCacheList.firstOrNull {
            it.aodId == aodId
        } as MovieMeta? ?: getMovieMetadata2(aodId)
    }

    suspend fun getTVShowMetadata(aodId: Int): TVShowMeta? {
        return metaCacheList.firstOrNull {
            it.aodId == aodId
        } as TVShowMeta? ?: getTVShowMetadata2(aodId)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun getMovieMetadata2(aodId: Int): MovieMeta? = withContext(Dispatchers.IO) {
        val url = URL("$repoUrl/movie/$aodId/media.json")
        return@withContext try {
            val json = url.readText()
            val meta = Gson().fromJson(json, MovieMeta::class.java)
            metaCacheList.add(meta)

            meta
        } catch (ex: FileNotFoundException) {
            null
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun getTVShowMetadata2(aodId: Int): TVShowMeta? = withContext(Dispatchers.IO) {
        val url = URL("$repoUrl/tv/$aodId/media.json")
        return@withContext try {
            val json = url.readText()
            val meta = Gson().fromJson(json, TVShowMeta::class.java)
            metaCacheList.add(meta)

            meta
        } catch (ex: FileNotFoundException) {
            null
        }
    }

}

// TODO move data classes
data class MediaList(
    val media: List<Int>
)

abstract class Meta {
    abstract val id: Int
    abstract val aodId: Int
    abstract val tmdbId: Int
}

data class MovieMeta(
    override val id: Int,
    @SerializedName("aod_id")
    override val aodId: Int,
    @SerializedName("tmdb_id")
    override val tmdbId: Int
): Meta()

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

data class EpisodeMeta(
    val id: Int,
    @SerializedName("aod_media_id")
    val aodMediaId: Int,
    @SerializedName("tmdb_id")
    val tmdbId: Int,
    @SerializedName("tmdb_number")
    val tmdbNumber: Int,
    @SerializedName("opening_start")
    val openingStart: Int,
    @SerializedName("opening_duration")
    val openingDuration: Int,
    @SerializedName("ending_start")
    val endingStart: Int,
    @SerializedName("ending_duration")
    val endingDuration: Int
)
