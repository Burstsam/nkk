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

package org.mosad.teapod.parser.crunchyroll

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.preferences.Preferences
import org.mosad.teapod.util.concatenate

private val json = Json { ignoreUnknownKeys = true }

object Crunchyroll {
    private val TAG = javaClass.name

    private val client = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
        }
    }
    private const val baseUrl = "https://beta-api.crunchyroll.com"
    private const val basicApiTokenUrl = "https://gitlab.com/-/snippets/2274956/raw/main/snippetfile1.txt"
    private var basicApiToken: String = ""

    private lateinit var token: Token
    private var tokenValidUntil: Long = 0
    private val tokeRefreshMutex = Mutex()

    private var accountID = ""

    private var policy = ""
    private var signature = ""
    private var keyPairID = ""

    private val browsingCache = arrayListOf<Item>()

    /**
     * Load the pai token, see:
     * https://git.mosad.xyz/mosad/NonePublicIssues/issues/1
     *
     * TODO handle empty file
     */
    fun initBasicApiToken() = runBlocking {
        withContext(Dispatchers.IO) {
            basicApiToken = (client.get(basicApiTokenUrl) as HttpResponse).readText()
            Log.i(TAG, "basic auth token: $basicApiToken")
        }
    }

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
        val formData = Parameters.build {
            append("username", username)
            append("password", password)
            append("grant_type", "password")
            append("scope", "offline_access")
        }

        var success = false// is false
        withContext(Dispatchers.IO) {
            // TODO handle exceptions
            Log.i(TAG, "getting token ...")

            val status = try {
                val response: HttpResponse = client.submitForm("$baseUrl$tokenEndpoint", formParameters = formData) {
                    header("Authorization", "Basic $basicApiToken")
                }
                token = response.receive()
                tokenValidUntil = System.currentTimeMillis() + (token.expiresIn * 1000)
                response.status
            } catch (ex: ClientRequestException) {
                val status = ex.response.status
                if (status == HttpStatusCode.Unauthorized) {
                    Log.e(TAG, "Could not complete login: " +
                            "${status.value} ${status.description}. " +
                            "Propably wrong username or password")
                }

                status
            }
            Log.i(TAG, "Login complete with code $status")
            success = (status == HttpStatusCode.OK)
        }

        return@runBlocking success
    }

    private fun refreshToken() {
        login(EncryptedPreferences.login, EncryptedPreferences.password)
    }

    /**
     * Requests: get, post, delete
     */

    private suspend inline fun <reified T> request(
        url: String,
        httpMethod: HttpMethod,
        params: List<Pair<String, Any?>> = listOf(),
        bodyObject: Any = Any()
    ): T = coroutineScope {
        // TODO find a better way to make token refresh thread safe, currently it's blocking
        tokeRefreshMutex.withLock {
            if (System.currentTimeMillis() > tokenValidUntil) refreshToken()
        }

        return@coroutineScope (Dispatchers.IO) {
            val response: T = client.request(url) {
                method = httpMethod
                header("Authorization", "${token.tokenType} ${token.accessToken}")
                params.forEach {
                    parameter(it.first, it.second)
                }

                // for json set body and content type
                if (bodyObject is JsonObject) {
                    body = bodyObject
                    contentType(ContentType.Application.Json)
                }
            }

            response
        }
    }

    private suspend inline fun <reified T> requestGet(
        endpoint: String,
        params: List<Pair<String, Any?>> = listOf(),
        url: String = ""
    ): T {
        val path = url.ifEmpty { "$baseUrl$endpoint" }

        return request(path, HttpMethod.Get, params)
    }

    private suspend fun requestPost(
        endpoint: String,
        params: List<Pair<String, Any?>> = listOf(),
        bodyObject: JsonObject
    ) {
        val path = "$baseUrl$endpoint"

        val response: HttpResponse = request(path, HttpMethod.Post, params, bodyObject)
        Log.i(TAG, "Response: $response")
    }

    private suspend fun requestPatch(
        endpoint: String,
        params: List<Pair<String, Any?>> = listOf(),
        bodyObject: JsonObject
    ) {
        val path = "$baseUrl$endpoint"

        val response: HttpResponse = request(path, HttpMethod.Patch, params, bodyObject)
        Log.i(TAG, "Response: $response")
    }

    private suspend fun requestDelete(
        endpoint: String,
        params: List<Pair<String, Any?>> = listOf(),
        url: String = ""
    ) = coroutineScope {
        val path = url.ifEmpty { "$baseUrl$endpoint" }

        val response: HttpResponse = request(path, HttpMethod.Delete, params)
        Log.i(TAG, "Response: $response")
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

        val index: Index = requestGet(indexEndpoint)
        policy = index.cms.policy
        signature = index.cms.signature
        keyPairID = index.cms.keyPairId

        Log.i(TAG, "Policy : $policy")
        Log.i(TAG, "Signature : $signature")
        Log.i(TAG, "Key Pair ID : $keyPairID")
    }

    /**
     * Retrieve the account id and set the corresponding global var.
     * The account id is needed for other calls.
     *
     * This must be execute on every start for teapod to work properly!
     */
    suspend fun account() {
        val indexEndpoint = "/accounts/v1/me"

        val account: Account = try {
            requestGet(indexEndpoint)
        } catch (ex: SerializationException) {
            Log.e(TAG, "SerializationException in account(). This is bad!", ex)
            NoneAccount
        }

        accountID = account.accountId
    }

    /**
     * General element/media functions: browse, search, objects, season_list
     */

    // TODO categories
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
        val noneOptParams = listOf(
            "locale" to Preferences.preferredLocale.toLanguageTag(),
            "sort_by" to sortBy.str,
            "start" to start,
            "n" to n
        )

        // if a season tag is present add it to the parameters
        val parameters = if (seasonTag.isNotEmpty()) {
            concatenate(noneOptParams, listOf("season_tag" to seasonTag))
        } else {
            noneOptParams
        }

        val browseResult: BrowseResult = try {
            requestGet(browseEndpoint, parameters)
        }catch (ex: SerializationException) {
            Log.e(TAG, "SerializationException in browse().", ex)
            NoneBrowseResult
        }

        // add results to cache TODO improve
        browsingCache.clear()
        browsingCache.addAll(browseResult.items)

        return browseResult
    }

    /**
     * Search fo a query term.
     * Note: currently this function only supports series/tv shows.
     *
     * @param query The query term as String
     * @param n The maximum number of results to return, default = 10
     * @return A **[SearchResult]** object
     */
    suspend fun search(query: String, n: Int = 10): SearchResult {
        val searchEndpoint = "/content/v1/search"
        val parameters = listOf(
            "locale" to Preferences.preferredLocale.toLanguageTag(),
            "q" to query,
            "n" to n,
            "type" to "series"
        )

        // TODO episodes have thumbnails as image, and not poster_tall/poster_tall,
        // to work around this, for now only tv shows are supported

        return try {
            requestGet(searchEndpoint, parameters)
        }catch (ex: SerializationException) {
            Log.e(TAG, "SerializationException in search(), with query = \"$query\".", ex)
            NoneSearchResult
        }
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
            "locale" to Preferences.preferredLocale.toLanguageTag(),
            "Signature" to signature,
            "Policy" to policy,
            "Key-Pair-Id" to keyPairID
        )

        return try {
            requestGet(episodesEndpoint, parameters)
        }catch (ex: SerializationException) {
            Log.e(TAG, "SerializationException in objects().", ex)
            NoneCollection
        }
    }

    /**
     * List all available seasons as **[SeasonListItem]**.
     */
    @Suppress("unused")
    suspend fun seasonList(): DiscSeasonList {
        val seasonListEndpoint = "/content/v1/season_list"
        val parameters = listOf("locale" to Preferences.preferredLocale.toLanguageTag())

        return try {
            requestGet(seasonListEndpoint, parameters)
        }catch (ex: SerializationException) {
            Log.e(TAG, "SerializationException in seasonList().", ex)
            NoneDiscSeasonList
        }
    }

    /**
     * Main media functions: series, season, episodes, playback
     */

    /**
     * series id == crunchyroll id?
     */
    suspend fun series(seriesId: String): Series {
        val seriesEndpoint = "/cms/v2/${token.country}/M3/crunchyroll/series/$seriesId"
        val parameters = listOf(
            "locale" to Preferences.preferredLocale.toLanguageTag(),
            "Signature" to signature,
            "Policy" to policy,
            "Key-Pair-Id" to keyPairID
        )

        return try {
            requestGet(seriesEndpoint, parameters)
        }catch (ex: SerializationException) {
            Log.e(TAG, "SerializationException in series().", ex)
            NoneSeries
        }
    }

    /**
     * Get the next episode for a series.
     *
     * @param seriesId The series id for which to call up next
     * @return A **[UpNextSeriesItem]** with a Panel representing the up next episode
     */
    suspend fun upNextSeries(seriesId: String): UpNextSeriesItem {
        val upNextSeriesEndpoint = "/content/v1/up_next_series"
        val parameters = listOf(
            "series_id" to seriesId,
            "locale" to Preferences.preferredLocale.toLanguageTag()
        )

        return try {
            requestGet(upNextSeriesEndpoint, parameters)
        }catch (ex: SerializationException) {
            Log.e(TAG, "SerializationException in upNextSeries().", ex)
            NoneUpNextSeriesItem
        }
    }

    suspend fun seasons(seriesId: String): Seasons {
        val seasonsEndpoint = "/cms/v2/${token.country}/M3/crunchyroll/seasons"
        val parameters = listOf(
            "series_id" to seriesId,
            "locale" to Preferences.preferredLocale.toLanguageTag(),
            "Signature" to signature,
            "Policy" to policy,
            "Key-Pair-Id" to keyPairID
        )

        return try {
            requestGet(seasonsEndpoint, parameters)
        }catch (ex: SerializationException) {
            Log.e(TAG, "SerializationException in seasons().", ex)
            NoneSeasons
        }
    }

    suspend fun episodes(seasonId: String): Episodes {
        val episodesEndpoint = "/cms/v2/${token.country}/M3/crunchyroll/episodes"
        val parameters = listOf(
            "season_id" to seasonId,
            "locale" to Preferences.preferredLocale.toLanguageTag(),
            "Signature" to signature,
            "Policy" to policy,
            "Key-Pair-Id" to keyPairID
        )

        return try {
            requestGet(episodesEndpoint, parameters)
        }catch (ex: SerializationException) {
            Log.e(TAG, "SerializationException in episodes().", ex)
            NoneEpisodes
        }
    }

    suspend fun playback(url: String): Playback {
        return try {
            requestGet("", url = url)
        }catch (ex: SerializationException) {
            Log.e(TAG, "SerializationException in playback(), with url = $url.", ex)
            NonePlayback
        }
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
        val parameters = listOf("locale" to Preferences.preferredLocale.toLanguageTag())

        return try {
            (requestGet(watchlistSeriesEndpoint, parameters) as JsonObject)
                .containsKey(seriesId)
        }catch (ex: SerializationException) {
            Log.e(TAG, "SerializationException in isWatchlist() with seriesId = $seriesId", ex)
            false
        }
    }

    /**
     * Add a media to the user's watchlist.
     *
     * @param seriesId The crunchyroll series id of the media to check
     */
    suspend fun postWatchlist(seriesId: String) {
        val watchlistPostEndpoint = "/content/v1/watchlist/$accountID"
        val parameters = listOf("locale" to Preferences.preferredLocale.toLanguageTag())

        val json = buildJsonObject {
            put("content_id", seriesId)
        }

        requestPost(watchlistPostEndpoint, parameters, json)
    }

    /**
     * Remove a media from the user's watchlist.
     *
     * @param seriesId The crunchyroll series id of the media to check
     */
    suspend fun deleteWatchlist(seriesId: String) {
        val watchlistDeleteEndpoint = "/content/v1/watchlist/$accountID/$seriesId"
        val parameters = listOf("locale" to Preferences.preferredLocale.toLanguageTag())

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
        val parameters = listOf("locale" to Preferences.preferredLocale.toLanguageTag())

        return try {
            requestGet(playheadsEndpoint, parameters)
        }catch (ex: SerializationException) {
            Log.e(TAG, "SerializationException in upNextSeries().", ex)
            emptyMap()
        }
    }

    /**
     * Post the playhead to crunchy (playhead position,watched state)
     *
     * @param episodeId A episode ID as strings.
     * @param playhead The episodes playhead in seconds.
     */
    suspend fun postPlayheads(episodeId: String, playhead: Int) {
        val playheadsEndpoint = "/content/v1/playheads/$accountID"
        val parameters = listOf("locale" to Preferences.preferredLocale.toLanguageTag())

        val json = buildJsonObject {
            put("content_id", episodeId)
            put("playhead", playhead)
        }

        requestPost(playheadsEndpoint, parameters, json)
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
        val parameters = listOf(
            "locale" to Preferences.preferredLocale.toLanguageTag(),
            "n" to n
        )

        val list: ContinueWatchingList = try {
            requestGet(watchlistEndpoint, parameters)
        }catch (ex: SerializationException) {
            Log.e(TAG, "SerializationException in watchlist().", ex)
            NoneContinueWatchingList
        }

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
        val parameters = listOf(
            "locale" to Preferences.preferredLocale.toLanguageTag(),
            "n" to n
        )

        return try {
            requestGet(watchlistEndpoint, parameters)
        }catch (ex: SerializationException) {
            Log.e(TAG, "SerializationException in upNextAccount().", ex)
            NoneContinueWatchingList
        }
    }

    /**
     * Account/Profile functions
     */

    suspend fun profile(): Profile {
        val profileEndpoint = "/accounts/v1/me/profile"

        return try {
            requestGet(profileEndpoint)
        }catch (ex: SerializationException) {
            Log.e(TAG, "SerializationException in profile().", ex)
            NoneProfile
        }
    }

    suspend fun postPrefSubLanguage(languageTag: String) {
        val profileEndpoint = "/accounts/v1/me/profile"
        val json = buildJsonObject {
            put("preferred_content_subtitle_language", languageTag)
        }

        requestPatch(profileEndpoint, bodyObject = json)
    }

}
