package org.mosad.teapod.parser.crunchyroll

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.json.FuelJson
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

class Cruncyroll {

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

            println("login complete with code ${response.statusCode}")

            return@withContext response.statusCode == 200
        }

        return@runBlocking false
    }

    // TODO get/post difference
    private suspend fun requestA(endpoint: String): Result<FuelJson, FuelError> = coroutineScope {
        return@coroutineScope (Dispatchers.IO) {
            val (request, response, result) = Fuel.get("$baseUrl$endpoint")
                .header("Authorization", "$tokenType $accessToken")
                .responseJson()

//            println("request request: $request")
//            println("request response: $response")
//            println("request result: $result")

            result
        }
    }

    // TESTING
    @Serializable
    data class Test(val total: Int, val items: List<Item>)

    @Serializable
    data class Item(val channel_id: String, val description: String)

    // TODO sort_by, default alphabetical, n, locale de-DE
    suspend fun browse() {
        val browseEndpoint = "/content/v1/browse"

        val result = requestA(browseEndpoint)

        println("${result.component1()?.obj()?.get("total")}")

        val test = json.decodeFromString<Test>(result.component1()?.obj()?.toString()!!)
        println(test)

    }

    suspend fun search() {
        val searchEndpoint = "/content/v1/search"

        val result = requestA(searchEndpoint)

        println("${result.component1()?.obj()?.get("total")}")

        val test = json.decodeFromString<Test>(result.component1()?.obj()?.toString()!!)
        println(test)

    }

}