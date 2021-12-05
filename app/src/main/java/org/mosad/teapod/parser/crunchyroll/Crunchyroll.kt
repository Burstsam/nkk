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

private val json = Json { ignoreUnknownKeys = true }

object Crunchyroll {

    private val baseUrl = "https://beta-api.crunchyroll.com"

    private var accessToken = ""
    private var tokenType = ""

    private var policy = ""
    private var signature = ""
    private var keyPairID = ""

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
    private suspend fun request(endpoint: String, params: Parameters = listOf()): Result<FuelJson, FuelError> = coroutineScope {
        return@coroutineScope (Dispatchers.IO) {
            val (request, response, result) = Fuel.get("$baseUrl$endpoint", params)
                .header("Authorization", "$tokenType $accessToken")
                .responseJson()

//            println("request request: $request")
//            println("request response: $response")
//            println("request result: $result")

            result
        }
    }

    // TESTING


    // TODO sort_by, default alphabetical, n, locale de-DE, categories
    /**
     * Browse the media available on crunchyroll.
     *
     * @param sortBy
     * @param n Number of items to return, defaults to 10
     *
     * @return A **[BrowseResult]** object is returned.
     */
    suspend fun browse(sortBy: SortBy = SortBy.ALPHABETICAL, n: Int = 10): BrowseResult {
        val browseEndpoint = "/content/v1/browse"
        val parameters = listOf("sort_by" to sortBy.str, "n" to n)

        val result = request(browseEndpoint, parameters)

//        val browseResult = json.decodeFromString<BrowseResult>(result.component1()?.obj()?.toString()!!)
//        println(browseResult.items.size)

        return json.decodeFromString(result.component1()?.obj()?.toString()!!)
    }

    // TODO
    suspend fun search() {
        val searchEndpoint = "/content/v1/search"
        val result = request(searchEndpoint)

        println("${result.component1()?.obj()?.get("total")}")

        val test = json.decodeFromString<BrowseResult>(result.component1()?.obj()?.toString()!!)
        println(test.items.size)

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

}