/**
 * Teapod
 *
 * Copyright 2020-2021  <seil0@mosad.xyz>
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

package org.mosad.teapod.parser

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.mosad.teapod.preferences.EncryptedPreferences
import org.mosad.teapod.util.*
import org.mosad.teapod.util.DataTypes.MediaType
import java.io.IOException
import java.lang.NumberFormatException
import java.util.*
import kotlin.random.Random

object AoDParser {

    private const val baseUrl = "https://www.anime-on-demand.de"
    private const val loginPath = "/users/sign_in"
    private const val libraryPath = "/animes"

    private const val userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:84.0) Gecko/20100101 Firefox/84.0"

    private var sessionCookies = mutableMapOf<String, String>()
    private var csrfToken: String = ""
    private var loginSuccess = false

    private val mediaList = arrayListOf<Media>() // actual media (data)
    val itemMediaList = arrayListOf<ItemMedia>() // gui media
    val highlightsList = arrayListOf<ItemMedia>()
    val newEpisodesList = arrayListOf<ItemMedia>()
    val newSimulcastsList = arrayListOf<ItemMedia>()
    val newTitlesList = arrayListOf<ItemMedia>()
    val topTenList = arrayListOf<ItemMedia>()

    fun login(): Boolean = runBlocking {

        withContext(Dispatchers.IO) {
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
                .timeout(60000) // login can take some time default is 60000 (60 sec)
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
     * initially load all media and home screen data
     */
    fun initialLoading() = listOf(
            loadHome(),
            listAnimes()
    )

    /**
     * get a media by it's ID (int)
     * @return Media
     */
    suspend fun getMediaById(mediaId: Int): Media {
        val media = mediaList.first { it.id == mediaId }

        if (media.episodes.isEmpty()) {
            loadStreams(media).join()
        }

        return media
    }

    fun markAsWatched(mediaId: Int, episodeId: Int) = GlobalScope.launch {
        val episode = getMediaById(mediaId).getEpisodeById(episodeId)
        episode.watched = true
        sendCallback(episode.watchedCallback)

        Log.d(javaClass.name, "Marked episode ${episode.id} as watched")
    }

    // TODO don't use jsoup here
    private fun sendCallback(callbackPath: String) = GlobalScope.launch(Dispatchers.IO) {
        val headers = mutableMapOf(
            Pair("Accept", "application/json, text/javascript, */*; q=0.01"),
            Pair("Accept-Language", "de,en-US;q=0.7,en;q=0.3"),
            Pair("Accept-Encoding", "gzip, deflate, br"),
            Pair("X-CSRF-Token", csrfToken),
            Pair("X-Requested-With", "XMLHttpRequest"),
        )

        try {
            Jsoup.connect(baseUrl + callbackPath)
                .ignoreContentType(true)
                .cookies(sessionCookies)
                .headers(headers)
                .execute()
        } catch (ex: IOException) {
            Log.e(javaClass.name, "Callback for $callbackPath failed.", ex)
        }

    }

    /**
     * load all media from aod into itemMediaList and mediaList
     */
    private fun listAnimes() = GlobalScope.launch(Dispatchers.IO) {
        val resAnimes = Jsoup.connect(baseUrl + libraryPath).get()
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
    }

    /**
     * load new episodes, titles and highlights
     */
    private fun loadHome() = GlobalScope.launch(Dispatchers.IO) {
         val resHome = Jsoup.connect(baseUrl).get()

        // get highlights from AoD
        highlightsList.clear()
        resHome.select("#aod-highlights").select("div.news-item").forEach {
            val mediaId = it.select("div.news-item-text").select("a.serienlink")
                .attr("href").substringAfterLast("/").toIntOrNull()
            val mediaTitle = it.select("div.news-title").select("h2").text()
            val mediaImage = it.select("img").attr("src")

            if (mediaId != null) {
                highlightsList.add(ItemMedia(mediaId, mediaTitle, mediaImage))
            }
        }

        // get all new episodes from AoD
        newEpisodesList.clear()
        resHome.select("h2:contains(Neue Episoden)").next().select("li").forEach {
            val mediaId = it.select("a.thumbs").attr("href")
                .substringAfterLast("/").toIntOrNull()
            val mediaImage = it.select("a.thumbs > img").attr("src")
            val mediaTitle = "${it.select("a").text()} - ${it.select("span.neweps").text()}"

            if (mediaId != null) {
                newEpisodesList.add(ItemMedia(mediaId, mediaTitle, mediaImage))
            }
        }

        // get new simulcasts from AoD
        newSimulcastsList.clear()
        resHome.select("h2:contains(Neue Simulcasts)").next().select("li").forEach {
            val mediaId = it.select("a.thumbs").attr("href")
                .substringAfterLast("/").toIntOrNull()
            val mediaImage = it.select("a.thumbs > img").attr("src")
            val mediaTitle = it.select("a").text()

            if (mediaId != null) {
                newSimulcastsList.add(ItemMedia(mediaId, mediaTitle, mediaImage))
            }
        }

        // get new titles from AoD
        newTitlesList.clear()
        resHome.select("h2:contains(Neue Anime-Titel)").next().select("li").forEach {
            val mediaId = it.select("a.thumbs").attr("href")
                .substringAfterLast("/").toIntOrNull()
            val mediaImage = it.select("a.thumbs > img").attr("src")
            val mediaTitle = it.select("a").text()

            if (mediaId != null) {
                newTitlesList.add(ItemMedia(mediaId, mediaTitle, mediaImage))
            }
        }

        // get top ten from AoD
        topTenList.clear()
        resHome.select("h2:contains(Anime Top 10)").next().select("li").forEach {
            val mediaId = it.select("a.thumbs").attr("href")
                .substringAfterLast("/").toIntOrNull()
            val mediaImage = it.select("a.thumbs > img").attr("src")
            val mediaTitle = it.select("a").text()

            if (mediaId != null) {
                topTenList.add(ItemMedia(mediaId, mediaTitle, mediaImage))
            }
        }

        // if highlights is empty, add a random new title
        if (highlightsList.isEmpty()) {
            if (newTitlesList.isNotEmpty()) {
                highlightsList.add(newTitlesList[Random.nextInt(0, newTitlesList.size)])
            } else {
                highlightsList.add(ItemMedia(0,"", ""))
            }
        }
    }

    /**
     * load streams for the media path, movies have one episode
     * @param media is used as call ba reference
     */
    private fun loadStreams(media: Media) = GlobalScope.launch(Dispatchers.IO) {
        if (sessionCookies.isEmpty()) login()

        if (!loginSuccess) {
            Log.w(javaClass.name, "Login, was not successful.")
            return@launch
        }

        // get the media page
        val res = Jsoup.connect(baseUrl + media.link)
            .cookies(sessionCookies)
            .get()

        //println(res)

        if (csrfToken.isEmpty()) {
            csrfToken = res.select("meta[name=csrf-token]").attr("content")
            //Log.i(javaClass.name, "New csrf token is $csrfToken")
        }

        val besides = res.select("div.besides").first()
        val playlists = besides.select("input.streamstarter_html5").map { streamstarter ->
            parsePlaylistAsync(
                streamstarter.attr("data-playlist"),
                streamstarter.attr("data-lang")
            )
        }.awaitAll()

        playlists.forEach { aod ->
            // TODO improve language handling
            val locale = when (aod.extLanguage) {
                "ger" -> Locale.GERMAN
                "jap" -> Locale.JAPANESE
                else -> Locale.ROOT
            }

            aod.playlist.forEach { ep ->
                try {
                    if (media.hasEpisode(ep.mediaid)) {
                        media.getEpisodeById(ep.mediaid).streams.add(
                            Stream(ep.sources.first().file, locale)
                        )
                    } else {
                        media.episodes.add(Episode(
                            id = ep.mediaid,
                            streams = mutableListOf(Stream(ep.sources.first().file, locale)),
                            posterUrl = ep.image,
                            title = ep.title,
                            description = ep.description,
                            number = getNumberFromTitle(ep.title, media.type)
                        ))
                    }
                } catch (ex: Exception) {
                    Log.w(javaClass.name, "Could not parse episode information.", ex)
                }
            }
        }
        Log.i(javaClass.name, "Loaded playlists successfully")

        // additional info from the media page
        res.select("table.vertical-table").select("tr").forEach { row ->
            when (row.select("th").text().toLowerCase(Locale.ROOT)) {
                "produktionsjahr" -> media.info.year = row.select("td").text().toInt()
                "fsk" -> media.info.age = row.select("td").text().toInt()
                "episodenanzahl" -> {
                    media.info.episodesCount = row.select("td").text()
                        .substringBefore("/")
                        .filter { it.isDigit() }
                        .toInt()
                }
            }
        }

        // similar titles from media page
        media.info.similar = res.select("h2:contains(Ähnliche Animes)").next().select("li").mapNotNull {
            val mediaId = it.select("a.thumbs").attr("href")
                .substringAfterLast("/").toIntOrNull()
            val mediaImage = it.select("a.thumbs > img").attr("src")
            val mediaTitle = it.select("a").text()

            if (mediaId != null) {
                ItemMedia(mediaId, mediaTitle, mediaImage)
            } else {
                null
            }
        }

        // additional information for tv shows the episode title (description) is loaded from the "api"
        if (media.type == MediaType.TVSHOW) {
            res.select("div.three-box-container > div.episodebox").forEach { episodebox ->
                // make sure the episode has a streaming link
                if (episodebox.select("input.streamstarter_html5").isNotEmpty()) {
                    val episodeId = episodebox.select("div.flip-front").attr("id").substringAfter("-").toInt()
                    val episodeShortDesc = episodebox.select("p.episodebox-shorttext").text()
                    val episodeWatched = episodebox.select("div.episodebox-icons > div").hasClass("status-icon-orange")
                    val episodeWatchedCallback = episodebox.select("input.streamstarter_html5").eachAttr("data-playlist").first()

                    media.episodes.firstOrNull { it.id == episodeId }?.apply {
                        shortDesc = episodeShortDesc
                        watched = episodeWatched
                        watchedCallback = episodeWatchedCallback
                    }
                }
            }
        }
        Log.i(javaClass.name, "media loaded successfully")
    }

    /**
     * don't use Gson().fromJson() as we don't have any control over the api and it may change
     */
    private fun parsePlaylistAsync(playlistPath: String, language: String): Deferred<AoDObject> {
        if (playlistPath == "[]") {
            return CompletableDeferred(AoDObject(listOf(), language))
        }

        return GlobalScope.async(Dispatchers.IO) {
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

            //Gson().fromJson(res.body(), AoDObject::class.java)

            return@async AoDObject(JsonParser.parseString(res.body()).asJsonObject
                .get("playlist").asJsonArray.map {
                    Playlist(
                        sources = it.asJsonObject.get("sources").asJsonArray.map { source ->
                            Source(source.asJsonObject.get("file").asString)
                        },
                        image = it.asJsonObject.get("image").asString,
                        title = it.asJsonObject.get("title").asString,
                        description = it.asJsonObject.get("description").asString,
                        mediaid = it.asJsonObject.get("mediaid").asInt
                    )
                },
                language
            )
        }
    }

    /**
     * get the episode number from the title
     * @param title the episode title, containing a number after "Ep."
     * @param type the media type, if not TVSHOW, return 0
     * @return the episode number, on NumberFormatException return 0
     */
    private fun getNumberFromTitle(title: String, type: MediaType): Int {
        return if (type == MediaType.TVSHOW) {
            try {
                title.substringAfter(", Ep. ").toInt()
            } catch (nex: NumberFormatException) {
                0
            }
        } else {
            0
        }
    }

}
