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

    fun login(): Boolean = runBlocking {

        val userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:80.0) Gecko/20100101 Firefox/80.0"

        withContext(Dispatchers.Default) {
            // get the authenticity token
            val resAuth = Jsoup.connect(baseUrl + loginPath)
                .header("User-Agent", userAgent)
                .execute()

            val authenticityToken = resAuth.parse().select("meta[name=csrf-token]").attr("content")
            val authCookies = resAuth.cookies()

            Log.i(javaClass.name, "Received authenticity token: $authenticityToken")
            Log.i(javaClass.name, "Received authenticity cookies: $authCookies")

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
                .cookies(authCookies)
                .execute()

            //println(resLogin.body())
            sessionCookies = resLogin.cookies()
            loginSuccess = resLogin.body().contains("Hallo, du bist jetzt angemeldet.")

            Log.i(javaClass.name, "Status: ${resLogin.statusCode()} (${resLogin.statusMessage()}), login successful: $loginSuccess")

            loginSuccess
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
                    type
                )
                media.info.posterLink = it.select("p.animebox-image").select("img").attr("src")
                media.info.shortDesc = it.select("p.animebox-shorttext").text()

                mediaList.add(media)
            }

            Log.i(javaClass.name, "Total library size is: ${mediaList.size}")

            return@withContext mediaList
        }
    }

    /**
     * load streams for the media path
     */
    fun loadStreams(media: Media): List<Episode> = runBlocking {
        if (sessionCookies.isEmpty()) login()

        if (!loginSuccess) {
            Log.w(javaClass.name, "Login, was not successful.")
            return@runBlocking listOf()
        }

        withContext(Dispatchers.Default) {

            val res = Jsoup.connect(baseUrl + media.link)
                .cookies(sessionCookies)
                .get()

            //println(res)

            // parse additional info from the media page
            res.select("table.vertical-table").select("tr").forEach {
                when (it.select("th").text().toLowerCase(Locale.ROOT)) {
                    "produktionsjahr" -> media.info.year = it.select("td").text().toInt()
                    "fsk" -> media.info.age = it.select("td").text().toInt()
                    "episodenanzahl" -> media.info.episodesCount = it.select("td").text().toInt()
                }
            }

            /**
             * TODO tv show specific for each episode (div.episodebox)
             *  * watchedCallback
             */
            val episodes = if (media.type == MediaType.TVSHOW) {
                res.select("div.three-box-container > div.episodebox").map { episodebox ->
                    val episodeId = episodebox.select("div.flip-front").attr("id").substringAfter("-").toInt()
                    val episodeWatched = episodebox.select("div.episodebox-icons > div").hasClass("status-icon-orange")
                    val episodeShortDesc = episodebox.select("p.episodebox-shorttext").text()

                    Episode(id = episodeId, watched = episodeWatched, shortDesc = episodeShortDesc)
                }
            } else {
                listOf(Episode())
            }

            // has attr data-lag (ger or jap)
            val playlists = res.select("input.streamstarter_html5").eachAttr("data-playlist")
            val csrfToken = res.select("meta[name=csrf-token]").attr("content")

            //println("first entry: ${playlists.first()}")
            //println("csrf token is: $csrfToken")

            return@withContext if (playlists.size > 0) {
                loadStreamInfo(playlists.first(), csrfToken, media.type, episodes)
            } else {
                listOf()
            }
        }
    }

    /**
     * load the playlist path and parse it, read the stream info from json
     * @param episodes is used as call ba reference, additionally it is passed a return value
     */
    private fun loadStreamInfo(playlistPath: String, csrfToken: String, type: MediaType, episodes: List<Episode>): List<Episode> = runBlocking {
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

            when (type) {
                MediaType.MOVIE -> {
                    val movie = JsonParser.parseString(res.body()).asJsonObject
                        .get("playlist").asJsonArray

                    movie.first().asJsonObject.get("sources").asJsonArray.toList().forEach {
                        episodes.first().streamUrl = it.asJsonObject.get("file").asString
                    }
                }

                MediaType.TVSHOW -> {
                    val episodesJson = JsonParser.parseString(res.body()).asJsonObject
                        .get("playlist").asJsonArray


                    episodesJson.forEach { jsonElement ->
                        val episodeId = jsonElement.asJsonObject.get("mediaid")
                        val episodeStream = jsonElement.asJsonObject.get("sources").asJsonArray
                            .first().asJsonObject
                            .get("file").asString
                        val episodeTitle = jsonElement.asJsonObject.get("title").asString
                        val episodePoster = jsonElement.asJsonObject.get("image").asString
                        val episodeDescription = jsonElement.asJsonObject.get("description").asString
                        val episodeNumber = episodeTitle.substringAfter(", Ep. ").toInt()

                        episodes.first { it.id == episodeId.asInt }.apply {
                            this.title = episodeTitle
                            this.posterLink = episodePoster
                            this.streamUrl = episodeStream
                            this.description = episodeDescription
                            this.number = episodeNumber
                        }
                    }

                }

                else -> {
                    Log.e(javaClass.name, "Wrong Type, please report this issue.")
                }
            }

            return@withContext episodes
        }
    }

}
