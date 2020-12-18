/**
 * Teapod
 *
 * Copyright 2020  <seil0@mosad.xyz>
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
import java.util.*
import kotlin.random.Random

object AoDParser {

    private const val baseUrl = "https://www.anime-on-demand.de"
    private const val loginPath = "/users/sign_in"
    private const val libraryPath = "/animes"

    private const val userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:80.0) Gecko/20100101 Firefox/80.0"

    private var sessionCookies = mutableMapOf<String, String>()
    private var csrfToken: String = ""
    private var loginSuccess = false

    private val mediaList = arrayListOf<Media>() // actual media (data)
    val itemMediaList = arrayListOf<ItemMedia>() // gui media
    val highlightsList = arrayListOf<ItemMedia>()
    val newEpisodesList = arrayListOf<ItemMedia>()
    val newSimulcastsList = arrayListOf<ItemMedia>()
    val newTitlesList = arrayListOf<ItemMedia>()

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
                .timeout(60000) // login can take some time
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
     * -> blocking
     */
    fun initialLoading() = runBlocking {
        val loadHomeJob = GlobalScope.async {
            loadHome()
        }

        val listJob = GlobalScope.async {
            listAnimes()
        }

        loadHomeJob.await()
        listJob.await()
    }

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

    // TODO don't use jsoup here
    fun sendCallback(callbackPath: String) = GlobalScope.launch(Dispatchers.IO) {
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
    private fun listAnimes()  = runBlocking {
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
        }
    }

    /**
     * load new episodes, titles and highlights
     */
    private fun loadHome() = runBlocking {
        if (sessionCookies.isEmpty()) login()

        withContext(Dispatchers.Default) {
            val resHome = Jsoup.connect(baseUrl)
                .cookies(sessionCookies)
                .get()

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

            // if highlights is empty, add a random new title
            if (highlightsList.isEmpty()) {
                highlightsList.add(newTitlesList[Random.nextInt(0, newTitlesList.size)])
            }
        }
    }

    /**
     * load streams for the media path, movies have one episode
     * @param media is used as call ba reference
     */
    private suspend fun loadStreams(media: Media) = GlobalScope.launch(Dispatchers.IO) {
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

        val pl = res.select("input.streamstarter_html5").first()
        val primary = pl.attr("data-playlist")
        val secondary = pl.attr("data-otherplaylist")
        val secondaryIsOmU = secondary.contains("OmU", true)

        // load primary and secondary playlist
        val primaryPlaylist = parsePlaylistAsync(primary)
        val secondaryPlaylist = parsePlaylistAsync(secondary)

        primaryPlaylist.await().playlist.forEach { ep ->
            val epNumber = if (media.type == MediaType.TVSHOW) {
                ep.title.substringAfter(", Ep. ").toInt()
            } else {
                0
            }

            media.episodes.add(
                Episode(
                    id = ep.mediaid,
                    priStreamUrl = ep.sources.first().file,
                    posterUrl = ep.image,
                    title = ep.title,
                    description = ep.description,
                    number = epNumber
                )
            )
        }
        Log.i(javaClass.name, "Loading primary playlist finished")

        secondaryPlaylist.await().playlist.forEach { ep ->
            val episode = media.episodes.firstOrNull { it.id == ep.mediaid }

            if (episode != null) {
                episode.secStreamUrl = ep.sources.first().file
                episode.secStreamOmU = secondaryIsOmU
            } else {
                val epNumber = if (media.type == MediaType.TVSHOW) {
                    ep.title.substringAfter(", Ep. ").toInt()
                } else {
                    0
                }

                media.episodes.add(
                    Episode(
                        id = ep.mediaid,
                        secStreamUrl = ep.sources.first().file,
                        secStreamOmU = secondaryIsOmU,
                        posterUrl = ep.image,
                        title = ep.title,
                        description = ep.description,
                        number = epNumber
                    )
                )
            }
        }
        Log.i(javaClass.name, "Loading secondary playlist finished")

        // parse additional info from the media page
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

        // parse additional information for tv shows the episode title (description) is loaded from the "api"
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
    }

    /**
     * don't use Gson().fromJson() as we don't have any control over the api and it may change
     */
    private fun parsePlaylistAsync(playlistPath: String): Deferred<AoDObject> {
        if (playlistPath == "[]") {
            return CompletableDeferred(AoDObject(listOf()))
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
                })
        }
    }

}
