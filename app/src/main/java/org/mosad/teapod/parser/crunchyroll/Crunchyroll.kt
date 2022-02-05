package org.mosad.teapod.parser.crunchyroll

import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Parameters
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.json.FuelJson
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.util.concatenate

private val json = Json { ignoreUnknownKeys = true }

object Crunchyroll {

    private const val baseUrl = "https://beta-api.crunchyroll.com"

    private var accessToken = ""
    private var tokenType = ""
    private var tokenValidUntil: Long = 0

    private var accountID = ""

    private var policy = ""
    private var signature = ""
    private var keyPairID = ""

    // TODO temp helper vary
    private var locale: String = Preferences.preferredLocal.toLanguageTag()
    private var country: String = Preferences.preferredLocal.country

    private val browsingCache = arrayListOf<Item>()

    /**
     * Login to the crunchyroll API.
     *
     * @param username The Username/Email of the user to log in
     * @param password The Accounts Password
     *
     * @return Boolean: True if login was successful, else false
     */
    fun login(username: String, password: String): Boolean = runBlocking {
        val tokenEndpoint = "/auth/v1/token"
        val formData = listOf(
            "username" to username,
            "password" to password,
            "grant_type" to "password",
            "scope" to "offline_access"
        )

        var success: Boolean // is false
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

                // token will be invalid 1 sec
                val expiresIn = (it.get("expires_in").toString().toLong() - 1)
                tokenValidUntil = System.currentTimeMillis() + (expiresIn * 1000)
            }

//            println("request: $request")
//            println("response: $response")
//            println("response: $result")

            Log.i(javaClass.name, "login complete with code ${response.statusCode}")
            success = (response.statusCode == 200)
        }

        return@runBlocking success
    }

    private fun refreshToken() {
        login(EncryptedPreferences.login, EncryptedPreferences.password)
    }

    /**
     * Requests: get, post, delete
     */

    private suspend fun request(
        endpoint: String,
        params: Parameters = listOf(),
        url: String = ""
    ): Result<FuelJson, FuelError> = coroutineScope {
        val path = url.ifEmpty { "$baseUrl$endpoint" }
        if (System.currentTimeMillis() > tokenValidUntil) refreshToken()

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

    private suspend fun requestPost(
        endpoint: String,
        params: Parameters = listOf(),
        body: String
    ) = coroutineScope {
        val path = "$baseUrl$endpoint"
        if (System.currentTimeMillis() > tokenValidUntil) refreshToken()

        withContext(Dispatchers.IO) {
            Fuel.post(path, params)
                .header("Authorization", "$tokenType $accessToken")
                .jsonBody(body)
                .response() // without a response, crunchy doesn't accept the request
        }
    }

    private suspend fun requestDelete(
        endpoint: String,
        params: Parameters = listOf(),
        url: String = ""
    ) = coroutineScope {
        val path = url.ifEmpty { "$baseUrl$endpoint" }
        if (System.currentTimeMillis() > tokenValidUntil) refreshToken()

        withContext(Dispatchers.IO) {
            Fuel.delete(path, params)
                .header("Authorization", "$tokenType $accessToken")
                .response() // without a response, crunchy doesn't accept the request
        }
    }

    /**
     * Basic functions: index, account
     * Needed for other functions to work properly!
     */

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

    /**
     * Retrieve the account id and set the corresponding global var.
     * The account id is needed for other calls.
     *
     * This must be execute on every start for teapod to work properly!
     */
    suspend fun account() {
        val indexEndpoint = "/accounts/v1/me"
        val result = request(indexEndpoint)

        result.component1()?.obj()?.let {
            accountID = it.get("account_id").toString()
        }
    }

    /**
     * General element/media functions: browse, search, objects, season_list
     */

    // TODO locale de-DE, categories
    /**
     * Browse the media available on crunchyroll.
     *
     * @param sortBy
     * @param n Number of items to return, defaults to 10
     *
     * @return A **[BrowseResult]** object is returned.
     */
    suspend fun browse(
        sortBy: SortBy = SortBy.ALPHABETICAL,
        seasonTag: String = "",
        start: Int = 0,
        n: Int = 10
    ): BrowseResult {
        val browseEndpoint = "/content/v1/browse"
        val noneOptParams = listOf("sort_by" to sortBy.str, "start" to start, "n" to n)

        // if a season tag is present add it to the parameters
        val parameters = if (seasonTag.isNotEmpty()) {
            concatenate(noneOptParams, listOf("season_tag" to seasonTag))
        } else {
            noneOptParams
        }

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
     * Get a collection of series objects.
     * Note: episode objects are currently not supported
     *
     * @param objects The object IDs as list of Strings
     * @return A **[Collection]** of Panels
     */
    suspend fun objects(objects: List<String>): Collection<Item> {
        val episodesEndpoint = "/cms/v2/DE/M3/crunchyroll/objects/${objects.joinToString(",")}"
        val parameters = listOf(
            "locale" to locale,
            "Signature" to signature,
            "Policy" to policy,
            "Key-Pair-Id" to keyPairID
        )

        val result = request(episodesEndpoint, parameters)

        return result.component1()?.obj()?.let {
            json.decodeFromString(it.toString())
        } ?: NoneCollection
    }

    /**
     * List all available seasons as **[SeasonListItem]**.
     */
    @Suppress("unused")
    suspend fun seasonList(): DiscSeasonList {
        val seasonListEndpoint = "/content/v1/season_list"
        val parameters = listOf("locale" to locale)

        val result = request(seasonListEndpoint, parameters)

        return result.component1()?.obj()?.let {
            json.decodeFromString(it.toString())
        } ?: NoneDiscSeasonList
    }

    /**
     * Main media functions: series, season, episodes, playback
     */

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

    /**
     * TODO
     */
    suspend fun upNextSeries(seriesId: String): UpNextSeriesItem {
        val upNextSeriesEndpoint = "/content/v1/up_next_series"
        val parameters = listOf(
            "series_id" to seriesId,
            "locale" to locale
        )

        val result = request(upNextSeriesEndpoint, parameters)

        return result.component1()?.obj()?.let {
            json.decodeFromString(it.toString())
        } ?: NoneUpNextSeriesItem
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
            json.decodeFromString(it.toString())
        } ?: NoneEpisodes
    }

    suspend fun playback(url: String): Playback {
        val result = request("", url = url)

        return result.component1()?.obj()?.let {
            json.decodeFromString(it.toString())
        } ?: NonePlayback
    }

    /**
     * Additional media functions: watchlist (series), playhead
     */

    /**
     * Check if a media is in the user's watchlist.
     *
     * @param seriesId The crunchyroll series id of the media to check
     * @return **[Boolean]**: ture if it was found, else false
     */
    suspend fun isWatchlist(seriesId: String): Boolean {
        val watchlistSeriesEndpoint = "/content/v1/watchlist/$accountID/$seriesId"
        val parameters = listOf("locale" to locale)

        val result = request(watchlistSeriesEndpoint, parameters)
        // if needed implement parsing

        return result.component1()?.obj()?.has(seriesId) ?: false
    }

    /**
     * Add a media to the user's watchlist.
     *
     * @param seriesId The crunchyroll series id of the media to check
     */
    suspend fun postWatchlist(seriesId: String) {
        val watchlistPostEndpoint = "/content/v1/watchlist/$accountID"
        val parameters = listOf("locale" to locale)

        val json = buildJsonObject {
            put("content_id", seriesId)
        }

        requestPost(watchlistPostEndpoint, parameters, json.toString())
    }

    /**
     * Remove a media from the user's watchlist.
     *
     * @param seriesId The crunchyroll series id of the media to check
     */
    suspend fun deleteWatchlist(seriesId: String) {
        val watchlistDeleteEndpoint = "/content/v1/watchlist/$accountID/$seriesId"
        val parameters = listOf("locale" to locale)

        requestDelete(watchlistDeleteEndpoint, parameters)
    }

    /**
     * Get playhead information for all episodes in episodeIDs.
     * The Information returned contains the playhead position, watched state
     * and last modified date.
     *
     * @param episodeIDs A **[List]** of episodes IDs as strings.
     * @return A **[Map]**<String, **[PlayheadObject]**> containing playback info.
     */
    suspend fun playheads(episodeIDs: List<String>): PlayheadsMap {
        val playheadsEndpoint = "/content/v1/playheads/$accountID/${episodeIDs.joinToString(",")}"
        val parameters = listOf("locale" to locale)

        val result = request(playheadsEndpoint, parameters)

        return result.component1()?.obj()?.let {
            json.decodeFromString(it.toString())
        } ?: emptyMap()
    }

    suspend fun postPlayheads(episodeId: String, playhead: Int) {
        val playheadsEndpoint = "/content/v1/playheads/$accountID"
        val parameters = listOf("locale" to locale)

        val json = buildJsonObject {
            put("content_id", episodeId)
            put("playhead", playhead)
        }

        requestPost(playheadsEndpoint, parameters, json.toString())
    }

    /**
     * Listing functions: watchlist (list), up_next_account
     */

    /**
     * List items present in the watchlist.
     *
     * @param n Number of items to return, defaults to 20.
     * @return A **[Watchlist]** containing up to n **[Item]**.
     */
    suspend fun watchlist(n: Int = 20): Watchlist {
        val watchlistEndpoint = "/content/v1/$accountID/watchlist"
        val parameters = listOf("locale" to locale, "n" to n)

        val result = request(watchlistEndpoint, parameters)
        val list: ContinueWatchingList = result.component1()?.obj()?.let {
            json.decodeFromString(it.toString())
        } ?: NoneContinueWatchingList

        val objects = list.items.map{ it.panel.episodeMetadata.seriesId }
        return objects(objects)
    }

    /**
     * List the next up episodes for the logged in account.
     *
     * @param n Number of items to return, defaults to 20.
     * @return A **[ContinueWatchingList]** containing up to n **[ContinueWatchingItem]**.
     */
    suspend fun upNextAccount(n: Int = 20): ContinueWatchingList {
        val watchlistEndpoint = "/content/v1/$accountID/up_next_account"
        val parameters = listOf("locale" to locale, "n" to n)

        val result = request(watchlistEndpoint, parameters)
        return result.component1()?.obj()?.let {
            json.decodeFromString(it.toString())
        } ?: NoneContinueWatchingList
    }

    /**
     * Account/Profile functions
     */

    suspend fun profile(): Profile {
        val profileEndpoint = "/accounts/v1/me/profile"

        val result = request(profileEndpoint)
        return result.component1()?.obj()?.let {
            json.decodeFromString(it.toString())
        } ?: NoneProfile
    }

}
