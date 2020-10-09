package org.mosad.teapod.parser

import kotlinx.coroutines.*
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.mosad.teapod.util.GUIMedia

class AoDParser {

    private val baseURL = "https://www.anime-on-demand.de"
    private val loginPath = "/users/sign_in"

    // TODO
    private val login = ""
    private val pwd = ""

    private var sessionCookies = mutableMapOf<String, String>()
    private var loginSuccess = false

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
                Pair("user[login]", login),
                Pair("user[password]", pwd),
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
            val res = Jsoup.connect("$baseURL/animes")
                .cookies(sessionCookies)
                .get()

            //println(res)

            val animes = arrayListOf<GUIMedia>()
            res.select("div.animebox").forEach {
                val media = GUIMedia(
                    it.select("h3.animebox-title").text(),
                    it.select("p.animebox-image").select("img").attr("src"),
                    it.select("p.animebox-shorttext").text(),
                    it.select("p.animebox-link").select("a").attr("href")
                )

                animes.add(media)
            }

            println("got ${animes.size} anime")

            return@withContext animes
        }
    }

    fun loadDetails(mediaPath: String) = runBlocking {
        if (sessionCookies.isEmpty()) login()

        if (!loginSuccess) {
            println("please log in")
            return@runBlocking
        }

        withContext(Dispatchers.Default) {
            println(baseURL + mediaPath)

            val res = Jsoup.connect(baseURL + mediaPath)
                .cookies(sessionCookies)
                .get()

            //println(res)

            val playlists = res.select("input.streamstarter_html5").eachAttr("data-playlist")
            println(playlists.first())

            val csrfToken = res.select("meta[name=csrf-token]").attr("content")
            println("csrf token is: $csrfToken")

            loadStreamInfo(playlists.first(), csrfToken)
        }
    }

    private fun loadStreamInfo(playlistPath: String, csrfToken: String) = runBlocking {
        withContext(Dispatchers.Default) {
            println(baseURL + playlistPath)

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

            // TODO replace with gson
            val jsonObject = JSONObject(res.body())
            val sourcesObject = jsonObject.getJSONArray("playlist").get(0).toString()

            val sourcesArray = JSONObject(sourcesObject).getJSONArray("sources")

            for (i in 0 until sourcesArray.length()) {
                println(sourcesArray[i].toString())
            }
        }
    }

}