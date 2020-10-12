package org.mosad.teapod.parser

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.util.DataTypes.MediaType
import org.mosad.teapod.util.Episode
import org.mosad.teapod.util.Media
import java.util.*
import kotlin.collections.ArrayList

class AoDParser {

    private val baseUrl = "https://www.anime-on-demand.de"
    private val loginPath = "/users/sign_in"
    private val libraryPath = "/animes"

    companion object {
        private var sessionCookies = mutableMapOf<String, String>()
        private var loginSuccess = false

        val mediaList = arrayListOf<Media>()
    }

    private fun login() = runBlocking {

        val userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:80.0) Gecko/20100101 Firefox/80.0"

        withContext(Dispatchers.Default) {
            // get the authenticity token
            val resAuth = Jsoup.connect(baseUrl + loginPath)
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

            val resLogin = Jsoup.connect(baseUrl + loginPath)
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

    /**
     * list all animes from the website
     */
    fun listAnimes(): ArrayList<Media>  = runBlocking {
        if (sessionCookies.isEmpty()) login()

        withContext(Dispatchers.Default) {
            val resAnimes = Jsoup.connect(baseUrl + libraryPath)
                .cookies(sessionCookies)
                .get()

            //println(resAnimes)

            mediaList.clear()
            resAnimes.select("div.animebox").forEach {
                val type = if (it.select("p.animebox-link").select("a").text().toLowerCase(Locale.ROOT) == "zur serie") {
                    MediaType.TVSHOW
                } else {
                    MediaType.MOVIE
                }

                val media = Media(
                    it.select("h3.animebox-title").text(),
                    it.select("p.animebox-link").select("a").attr("href"),
                    type,
                    it.select("p.animebox-image").select("img").attr("src"),
                    it.select("p.animebox-shorttext").text()
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
    fun loadStreams(media: Media): List<Episode> = runBlocking {
        if (sessionCookies.isEmpty()) login()

        if (!loginSuccess) {
            println("please log in") // TODO
            return@runBlocking listOf()
        }

        withContext(Dispatchers.Default) {

            val res = Jsoup.connect(baseUrl + media.link)
                .cookies(sessionCookies)
                .get()

            //println(res)

            val playlists = res.select("input.streamstarter_html5").eachAttr("data-playlist")
            val csrfToken = res.select("meta[name=csrf-token]").attr("content")

            //println("first entry: ${playlists.first()}")
            //println("csrf token is: $csrfToken")

            return@withContext loadStreamInfo(playlists.first(), csrfToken, media.type)
        }
    }

    /**
     * load the playlist path and parse it, read the stream info from json
     */
    private fun loadStreamInfo(playlistPath: String, csrfToken: String, type: MediaType): List<Episode> = runBlocking {
        withContext(Dispatchers.Default) {
            val headers = mutableMapOf(
                Pair("Accept", "application/json, text/javascript, */*; q=0.01"),
                Pair("Accept-Language", "de,en-US;q=0.7,en;q=0.3"),
                Pair("Accept-Encoding", "gzip, deflate, br"),
                Pair("X-CSRF-Token", csrfToken),
                Pair("X-Requested-With", "XMLHttpRequest"),
            )

            val res = Jsoup.connect(baseUrl + playlistPath)
                .ignoreContentType(true)
                .cookies(sessionCookies)
                .headers(headers)
                .execute()

            //println(res.body())

            return@withContext when (type) {
                MediaType.MOVIE -> {
                    val movie = JsonParser.parseString(res.body()).asJsonObject
                        .get("playlist").asJsonArray

                    movie.first().asJsonObject.get("sources").asJsonArray.toList().map {
                        Episode(streamUrl = it.asJsonObject.get("file").asString)
                    }
                }
                MediaType.TVSHOW -> {
                    val episodesJson = JsonParser.parseString(res.body()).asJsonObject
                        .get("playlist").asJsonArray


                    episodesJson.map {
                        val episodeStream = it.asJsonObject.get("sources").asJsonArray
                            .first().asJsonObject
                            .get("file").asString
                        val episodeTitle = it.asJsonObject.get("title").asString

                        Episode(
                            episodeTitle,
                            episodeStream
                        )
                    }

                }
                else -> {
                    Log.e(javaClass.name, "Wrong Type, please report this issue.")
                    listOf()
                }
            }
        }
    }

}
