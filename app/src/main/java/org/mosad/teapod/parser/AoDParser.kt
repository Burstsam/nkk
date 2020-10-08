package org.mosad.teapod.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.mosad.teapod.util.GUIMedia

class AoDParser {

    private val baseURL = "https://www.anime-on-demand.de"
    private val loginPath = "/users/sign_in"

    private val login = ""
    private val pwd = ""

    private fun login(): MutableMap<String, String> = runBlocking {


        val userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:80.0) Gecko/20100101 Firefox/80.0"

        withContext(Dispatchers.Default) {
            val con = Jsoup.connect(baseURL)


            // get the authenticity token
            val resAuth = con.url(baseURL + loginPath)//Jsoup.connect(baseURL + loginPath)
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

            val loginSuccess = resLogin.body().contains("Hallo, du bist jetzt angemeldet.")
            println("Status: ${resLogin.statusCode()} (${resLogin.statusMessage()}), login successful: $loginSuccess")

            return@withContext resLogin.cookies()
        }
    }

    // https://www.anime-on-demand.de/animes
    fun listAnime(): ArrayList<GUIMedia>  = runBlocking {
        withContext(Dispatchers.Default) {
            val cookies = login()


            val res = Jsoup.connect("$baseURL/animes")
                .cookies(cookies)
                .get()

            //println(res)

            val anime = arrayListOf<GUIMedia>()
            res.select("div.animebox").forEach {
                val media = GUIMedia(
                    it.select("h3.animebox-title").text(),
                    it.select("p.animebox-image").select("img").attr("src"),
                    it.select("p.animebox-link").select("a").attr("href"),
                    it.select("p.animebox-shorttext").text()
                )

                anime.add(media)
            }

            println("got ${anime.size} anime")

            return@withContext anime
        }
    }

}