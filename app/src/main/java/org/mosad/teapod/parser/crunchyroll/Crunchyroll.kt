package org.mosad.teapod.parser.crunchyroll

import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Parameters
import com.github.kittinunf.fuel.json.FuelJson
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.*

private val json = Json { ignoreUnknownKeys = true }

object Crunchyroll {

    private const val baseUrl = "https://beta-api.crunchyroll.com"

    private var accessToken = ""
    private var tokenType = ""

    private var policy = ""
    private var signature = ""
    private var keyPairID = ""

    // TODO temp helper vary
    var locale = "${Locale.GERMANY.language}-${Locale.GERMANY.country}"
    var country = Locale.GERMANY.country

    val browsingCache = arrayListOf<Item>()

    fun login(username: String, password: String): Boolean = runBlocking {
        val tokenEndpoint = "/auth/v1/token"
        val formData = listOf(
            "username" to username,
            "password" to password,
            "grant_type" to "password",
            "scope" to "offline_access"
        )

        withContext(Dispatchers.IO) {
            val (request, response, result) = Fuel.post("$baseUrl$tokenEndpoint", parameters = formData)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .appendHeader(
                    "Authorization",
                    "Basic "
                )
                .responseJson()

            // TODO fix JSONException: No value for
            result.component1()?.obj()?.let {
                accessToken = it.get("access_token").toString()
                tokenType = it.get("token_type").toString()
            }

//            println("request: $request")
//            println("response: $response")
//            println("response: $result")

            Log.i(javaClass.name, "login complete with code ${response.statusCode}")

            return@withContext response.statusCode == 200
        }

        return@runBlocking false
    }

    // TODO get/post difference
    private suspend fun request(
        endpoint: String,
        params: Parameters = listOf(),
        url: String = ""
    ): Result<FuelJson, FuelError> = coroutineScope {
        val path = if (url.isEmpty()) "$baseUrl$endpoint" else url

        return@coroutineScope (Dispatchers.IO) {
            val (request, response, result) = Fuel.get(path, params)
                .header("Authorization", "$tokenType $accessToken")
                .responseJson()

//            println("request request: $request")
//            println("request response: $response")
//            println("request result: $result")

            result
        }
    }

    /**
     * Retrieve the identifiers necessary for streaming. If the identifiers are
     * retrieved, set the corresponding global var. The identifiers are valid for 24h.
     */
    suspend fun index() {
        val indexEndpoint = "/index/v2"
        val result = request(indexEndpoint)

        result.component1()?.obj()?.getJSONObject("cms")?.let {
            policy = it.get("policy").toString()
            signature = it.get("signature").toString()
            keyPairID = it.get("key_pair_id").toString()
        }

        println("policy: $policy")
        println("signature: $signature")
        println("keyPairID: $keyPairID")
    }


    // TODO locale de-DE, categories
    /**
     * Browse the media available on crunchyroll.
     *
     * @param sortBy
     * @param n Number of items to return, defaults to 10
     *
     * @return A **[BrowseResult]** object is returned.
     */
    suspend fun browse(sortBy: SortBy = SortBy.ALPHABETICAL, start: Int = 0, n: Int = 10): BrowseResult {
        val browseEndpoint = "/content/v1/browse"
        val parameters = listOf("sort_by" to sortBy.str, "start" to start, "n" to n)

        val result = request(browseEndpoint, parameters)
        val browseResult = result.component1()?.obj()?.let {
            json.decodeFromString(it.toString())
        } ?: NoneBrowseResult

        // add results to cache TODO improve
        browsingCache.clear()
        browsingCache.addAll(browseResult.items)

        return browseResult
    }

    /**
     * TODO
     */
    suspend fun search(query: String, n: Int = 10): SearchResult {
        val searchEndpoint = "/content/v1/search"
        val parameters = listOf("q" to query, "n" to n, "locale" to locale, "type" to "series")

        val result = request(searchEndpoint, parameters)
        // TODO episodes have thumbnails as image, and not poster_tall/poster_tall,
        // to work around this, for now only tv shows are supported

        return result.component1()?.obj()?.let {
            json.decodeFromString(it.toString())
        } ?: NoneSearchResult
    }

    /**
     * series id == crunchyroll id?
     */
    suspend fun series(seriesId: String): Series {
        val seriesEndpoint = "/cms/v2/$country/M3/crunchyroll/series/$seriesId"
        val parameters = listOf(
            "locale" to locale,
            "Signature" to signature,
            "Policy" to policy,
            "Key-Pair-Id" to keyPairID
        )

        val result = request(seriesEndpoint, parameters)

        return result.component1()?.obj()?.let {
            json.decodeFromString(it.toString())
        } ?: NoneSeries
    }

    suspend fun seasons(seriesId: String): Seasons {
        val episodesEndpoint = "/cms/v2/$country/M3/crunchyroll/seasons"
        val parameters = listOf(
            "series_id" to seriesId,
            "locale" to locale,
            "Signature" to signature,
            "Policy" to policy,
            "Key-Pair-Id" to keyPairID
        )

        val result = request(episodesEndpoint, parameters)

        return result.component1()?.obj()?.let {
            println(it)
            json.decodeFromString(it.toString())
        } ?: NoneSeasons
    }

    suspend fun episodes(seasonId: String): Episodes {
        val episodesEndpoint = "/cms/v2/$country/M3/crunchyroll/episodes"
        val parameters = listOf(
            "season_id" to seasonId,
            "locale" to locale,
            "Signature" to signature,
            "Policy" to policy,
            "Key-Pair-Id" to keyPairID
        )

        val result = request(episodesEndpoint, parameters)

        return result.component1()?.obj()?.let {
            println(it)
            json.decodeFromString(it.toString())
        } ?: NoneEpisodes
    }

    suspend fun playback(url: String): Playback {
        val result = request("", url = url)

        return result.component1()?.obj()?.let {
            println(it)
            json.decodeFromString(it.toString())
        } ?: NonePlayback
    }

}