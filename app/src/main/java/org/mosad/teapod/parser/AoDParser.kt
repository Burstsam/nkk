package org.mosad.teapod.parser

import com.google.gson.JsonParser
import kotlinx.coroutines.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.util.GUIMedia

class AoDParser {

    private val baseURL = "https://www.anime-on-demand.de"
    private val loginPath = "/users/sign_in"
    private val libraryPath = "/animes"

    companion object {
        private var sessionCookies = mutableMapOf<String, String>()
        private var loginSuccess = false

        val mediaList = arrayListOf<GUIMedia>()
    }

    private fun login() = runBlocking {

        val userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:80.0) Gecko/20100101 Firefox/80.0"

        withContext(Dispatchers.Default) {
            // get the authenticity token
            val resAuth = Jsoup.connect(baseURL + loginPath)
                .header("User-Agent", userAgent)
                .execute()

            val authenticityToken = resAuth.parse().select("meta[name=csrf-token]").attr("content")
            println("Authenticity token is: $authenticityToken")

            val cookies = resAuth.cookies()
            println("cookies: $cookies")

            val data = mapOf(
                Pair("user[login]", EncryptedPreferences.login),
                Pair("user[password]", EncryptedPreferences.password),
                Pair("user[remember_me]", "1"),
                Pair("commit", "Einloggen"),
                Pair("authenticity_token", authenticityToken)
            )

            val resLogin = Jsoup.connect(baseURL + loginPath)
                .method(Connection.Method.POST)
                .data(data)
                .postDataCharset("UTF-8")
                .cookies(cookies)
                .execute()

            //println(resLogin.body())

            loginSuccess = resLogin.body().contains("Hallo, du bist jetzt angemeldet.")
            println("Status: ${resLogin.statusCode()} (${resLogin.statusMessage()}), login successful: $loginSuccess")

            sessionCookies = resLogin.cookies()
        }
    }

    // https://www.anime-on-demand.de/animes
    fun listAnimes(): ArrayList<GUIMedia>  = runBlocking {
        if (sessionCookies.isEmpty()) login()

        withContext(Dispatchers.Default) {
            val resAnimes = Jsoup.connect(baseURL + libraryPath)
                .cookies(sessionCookies)
                .get()

            //println(resAnimes)

            mediaList.clear()
            resAnimes.select("div.animebox").forEach {
                val media = GUIMedia(
                    it.select("h3.animebox-title").text(),
                    it.select("p.animebox-image").select("img").attr("src"),
                    it.select("p.animebox-shorttext").text(),
                    it.select("p.animebox-link").select("a").attr("href")
                )

                mediaList.add(media)
            }

            println("got ${mediaList.size} anime")

            return@withContext mediaList
        }
    }

    /**
     * load streams for the media path
     */
    fun loadStreams(mediaPath: String): List<String> = runBlocking {
        if (sessionCookies.isEmpty()) login()

        if (!loginSuccess) {
            println("please log in") // TODO
            return@runBlocking listOf()
        }

        withContext(Dispatchers.Default) {

            val res = Jsoup.connect(baseURL + mediaPath)
                .cookies(sessionCookies)
                .get()

            //println(res)

            val playlists = res.select("input.streamstarter_html5").eachAttr("data-playlist")
            val csrfToken = res.select("meta[name=csrf-token]").attr("content")

            //println("first entry: ${playlists.first()}")
            //println("csrf token is: $csrfToken")

            return@withContext loadStreamInfo(playlists.first(), csrfToken)
        }
    }

    /**
     * load the playlist path and parse it, read the stream info from json
     */
    private fun loadStreamInfo(playlistPath: String, csrfToken: String): List<String> = runBlocking {
        withContext(Dispatchers.Default) {
            val headers = mutableMapOf(
                Pair("Accept", "application/json, text/javascript, */*; q=0.01"),
                Pair("Accept-Language", "de,en-US;q=0.7,en;q=0.3"),
                Pair("Accept-Encoding", "gzip, deflate, br"),
                Pair("X-CSRF-Token", csrfToken),
                Pair("X-Requested-With", "XMLHttpRequest"),
            )

            val res = Jsoup.connect(baseURL + playlistPath)
                .ignoreContentType(true)
                .cookies(sessionCookies)
                .headers(headers)
                .execute()

            //println(res.body())

            // TODO if it's a series there sources for each episode
            val sources = JsonParser.parseString(res.body()).asJsonObject
                .get("playlist").asJsonArray.first().asJsonObject
                .get("sources").asJsonArray

            return@withContext sources.toList().map {
                it.asJsonObject.get("file").asString
            }
        }
    }

}
