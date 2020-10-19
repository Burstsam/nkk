package org.mosad.teapod.parser

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.util.DataTypes.MediaType
import org.mosad.teapod.util.Episode
import org.mosad.teapod.util.ItemMedia
import org.mosad.teapod.util.Media
import java.io.IOException
import java.util.*

/**
 * maybe AoDParser as object would be useful
 */
class AoDParser {

    private val baseUrl = "https://www.anime-on-demand.de"
    private val loginPath = "/users/sign_in"
    private val libraryPath = "/animes"

    private val userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:80.0) Gecko/20100101 Firefox/80.0"

    companion object {
        private var csrfToken: String = ""
        private var sessionCookies = mutableMapOf<String, String>()
        private var loginSuccess = false

        val mediaList = arrayListOf<Media>()
        val itemMediaList = arrayListOf<ItemMedia>()
        val newEpisodesList = arrayListOf<ItemMedia>()
    }

    fun login(): Boolean = runBlocking {

        withContext(Dispatchers.Default) {
            // get the authenticity token
            val resAuth = Jsoup.connect(baseUrl + loginPath)
                .header("User-Agent", userAgent)
                .execute()

            val authenticityToken = resAuth.parse().select("meta[name=csrf-token]").attr("content")
            val authCookies = resAuth.cookies()

            //Log.d(javaClass.name, "Received authenticity token: $authenticityToken")
            //Log.d(javaClass.name, "Received authenticity cookies: $authCookies")

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

            itemMediaList.clear()
            mediaList.clear()
            resAnimes.select("div.animebox").forEach {
                val type = if (it.select("p.animebox-link").select("a").text().toLowerCase(Locale.ROOT) == "zur serie") {
                    MediaType.TVSHOW
                } else {
                    MediaType.MOVIE
                }
                val mediaTitle = it.select("h3.animebox-title").text()
                val mediaLink = it.select("p.animebox-link").select("a").attr("href")
                val mediaImage = it.select("p.animebox-image").select("img").attr("src")
                val mediaShortText = it.select("p.animebox-shorttext").text()
                val mediaId = mediaLink.substringAfterLast("/").toInt()

                itemMediaList.add(ItemMedia(mediaId, mediaTitle, mediaImage))
                mediaList.add(Media(mediaId, mediaLink, type).apply {
                    info.title = mediaTitle
                    info.posterUrl = mediaImage
                    info.shortDesc = mediaShortText
                })
            }

            Log.i(javaClass.name, "Total library size is: ${mediaList.size}")

            return@withContext mediaList
        }
    }

    fun listNewEpisodes() = runBlocking {
        if (sessionCookies.isEmpty()) login()

        withContext(Dispatchers.Default) {
            val resHome = Jsoup.connect(baseUrl)
                .cookies(sessionCookies)
                .get()

            newEpisodesList.clear()
            resHome.select("div.jcarousel-container-new").select("li").forEach {
                if (it.select("span").hasClass("neweps")) {
                    val mediaId = it.select("a.thumbs").attr("href")
                        .substringAfterLast("/").toInt()
                    val mediaImage = it.select("a.thumbs > img").attr("src")
                    val mediaTitle = "${it.select("a").text()} - ${it.select("span.neweps").text()}"

                    newEpisodesList.add(ItemMedia(mediaId, mediaTitle, mediaImage))
                }
            }

        }
    }

    fun getMediaById(mediaId: Int): Media {
        val media = mediaList.first { it.id == mediaId }

        if (media.episodes.isEmpty()) {
            loadStreams(media)
        }

        return media
    }

    /**
     * load streams for the media path, movies have one episode
     * @param media is used as call ba reference
     */
    private fun loadStreams(media: Media) = runBlocking {
        if (sessionCookies.isEmpty()) login()

        if (!loginSuccess) {
            Log.w(javaClass.name, "Login, was not successful.")
            return@runBlocking
        }

        withContext(Dispatchers.Default) {

            val res = Jsoup.connect(baseUrl + media.link)
                .cookies(sessionCookies)
                .get()

            //println(res)

            // parse additional info from the media page
            res.select("table.vertical-table").select("tr").forEach { row ->
                when (row.select("th").text().toLowerCase(Locale.ROOT)) {
                    "produktionsjahr" -> media.info.year = row.select("td").text().toInt()
                    "fsk" -> media.info.age = row.select("td").text().toInt()
                    "episodenanzahl" -> {
                        media.info.episodesCount = row.select("td").text()
                            .substringBefore("/")
                            .filter{ it.isDigit() }
                            .toInt()
                    }
                }
            }

            // parse additional information for tv shows
            media.episodes = when (media.type) {
                MediaType.MOVIE -> listOf(Episode())
                MediaType.TVSHOW -> {
                    res.select("div.three-box-container > div.episodebox").map { episodebox ->
                        val episodeId = episodebox.select("div.flip-front").attr("id").substringAfter("-").toInt()
                        val episodeShortDesc = episodebox.select("p.episodebox-shorttext").text()
                        val episodeWatched = episodebox.select("div.episodebox-icons > div").hasClass("status-icon-orange")
                        val episodeWatchedCallback = episodebox.select("input.streamstarter_html5").eachAttr("data-playlist").first()

                        Episode(
                            id = episodeId,
                            shortDesc = episodeShortDesc,
                            watched = episodeWatched,
                            watchedCallback = episodeWatchedCallback
                        )
                    }
                }
                MediaType.OTHER -> listOf()
            }

            if (csrfToken.isEmpty()) {
                csrfToken = res.select("meta[name=csrf-token]").attr("content")
                //Log.i(javaClass.name, "New csrf token is $csrfToken")
            }

            // TODO has attr data-lag (ger or jap)
            val playlists = res.select("input.streamstarter_html5").eachAttr("data-playlist")

            if (playlists.size > 0) {
                loadPlaylist(playlists.first(), csrfToken, media.type, media.episodes)
            }
        }
    }

    /**
     * load the playlist path and parse it, read the stream info from json
     * @param episodes is used as call ba reference
     */
    private fun loadPlaylist(playlistPath: String, csrfToken: String, type: MediaType, episodes: List<Episode>) = runBlocking {
        withContext(Dispatchers.Default) {
            val headers = mutableMapOf(
                Pair("Accept", "application/json, text/javascript, */*; q=0.01"),
                Pair("Accept-Language", "de,en-US;q=0.7,en;q=0.3"),
                Pair("Accept-Encoding", "gzip, deflate, br"),
                Pair("X-CSRF-Token", csrfToken),
                Pair("X-Requested-With", "XMLHttpRequest"),
            )

            //println("loading streaminfo with cstf: $csrfToken")

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
                        .first().asJsonObject

                    movie.get("sources").asJsonArray.first().apply {
                        episodes.first().streamUrl = this.asJsonObject.get("file").asString
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
                            this.posterUrl = episodePoster
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
        }
    }

    fun sendCallback(callbackPath: String) = GlobalScope.launch {
        val headers = mutableMapOf(
            Pair("Accept", "application/json, text/javascript, */*; q=0.01"),
            Pair("Accept-Language", "de,en-US;q=0.7,en;q=0.3"),
            Pair("Accept-Encoding", "gzip, deflate, br"),
            Pair("X-CSRF-Token", csrfToken),
            Pair("X-Requested-With", "XMLHttpRequest"),
        )

        try {
            withContext(Dispatchers.IO) {
                Jsoup.connect(baseUrl + callbackPath)
                    .ignoreContentType(true)
                    .cookies(sessionCookies)
                    .headers(headers)
                    .execute()
            }
        } catch (ex: IOException) {
            Log.e(javaClass.name, "Callback for $callbackPath failed.", ex)
        }

    }

}
