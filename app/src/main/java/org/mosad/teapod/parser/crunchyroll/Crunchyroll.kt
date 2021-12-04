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
    suspend fun browse(sortBy: SortBy = SortBy.ALPHABETICAL, n: Int = 10): BrowseResult {
        val browseEndpoint = "/content/v1/browse"
        val parameters = listOf("sort_by" to sortBy.str, "n" to n)

        val result = request(browseEndpoint, parameters)

//        val browseResult = json.decodeFromString<BrowseResult>(result.component1()?.obj()?.toString()!!)
//        println(browseResult.items.size)

        return json.decodeFromString<BrowseResult>(result.component1()?.obj()?.toString()!!)
    }

    // TODO
    suspend fun search() {
        val searchEndpoint = "/content/v1/search"
        val result = request(searchEndpoint)

        println("${result.component1()?.obj()?.get("total")}")

        val test = json.decodeFromString<BrowseResult>(result.component1()?.obj()?.toString()!!)
        println(test.items.size)

    }

}