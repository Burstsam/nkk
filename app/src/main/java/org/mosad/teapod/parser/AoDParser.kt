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
import java.net.CookieStore
import java.util.*
import kotlin.random.Random
import kotlin.reflect.jvm.jvmName

object AoDParser {

    private const val baseUrl = "https://www.anime-on-demand.de"
    private const val loginPath = "/users/sign_in"
    private const val libraryPath = "/animes"
    private const val subscriptionPath = "/mypools"

    private const val userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:84.0) Gecko/20100101 Firefox/84.0"

    private lateinit var cookieStore: CookieStore
    private var csrfToken: String = ""
    private var loginSuccess = false

    private val aodMediaList = arrayListOf<AoDMedia>() // actual media (data)

    // gui media
    val guiMediaList = arrayListOf<ItemMedia>()
    val highlightsList = arrayListOf<ItemMedia>()
    val newEpisodesList = arrayListOf<ItemMedia>()
    val newSimulcastsList = arrayListOf<ItemMedia>()
    val newTitlesList = arrayListOf<ItemMedia>()
    val topTenList = arrayListOf<ItemMedia>()

    fun login(): Boolean = runBlocking {

        withContext(Dispatchers.IO) {
            // get the authenticity token and cookies
            val conAuth = Jsoup.connect(baseUrl + loginPath)
                .header("User-Agent", userAgent)

            cookieStore = conAuth.cookieStore()
            csrfToken = conAuth.execute().parse().select("meta[name=csrf-token]").attr("content")

            Log.d(AoDParser::class.jvmName, "Received authenticity token: $csrfToken")
            Log.d(AoDParser::class.jvmName, "Received authenticity cookies: $cookieStore")

            val data = mapOf(
                Pair("user[login]", EncryptedPreferences.login),
                Pair("user[password]", EncryptedPreferences.password),
                Pair("user[remember_me]", "1"),
                Pair("commit", "Einloggen"),
                Pair("authenticity_token", csrfToken)
            )

            val resLogin = Jsoup.connect(baseUrl + loginPath)
                .method(Connection.Method.POST)
                .timeout(60000) // login can take some time default is 60000 (60 sec)
                .data(data)
                .postDataCharset("UTF-8")
                .cookieStore(cookieStore)
                .execute()
            //println(resLogin.body())

            loginSuccess = resLogin.body().contains("Hallo, du bist jetzt angemeldet.")
            Log.i(AoDParser::class.jvmName, "Status: ${resLogin.statusCode()} (${resLogin.statusMessage()}), login successful: $loginSuccess")

            loginSuccess
        }
    }

    /**
     * initially load all media and home screen data
     */
    suspend fun initialLoading() {
        coroutineScope {
            launch { loadHome() }
            launch { listAnimes() }
        }
    }

    /**
     * get a media by it's ID (int)
     * @param aodId The AoD ID of the requested media
     * @return returns a AoDMedia of type Movie or TVShow if found, else return AoDMediaNone
     */
    suspend fun getMediaById(aodId: Int): AoDMedia {
        return aodMediaList.firstOrNull { it.aodId == aodId } ?:
        try {
            loadMediaAsync(aodId).await().apply {
                aodMediaList.add(this)
            }
        } catch (exn:NullPointerException) {
            Log.e(AoDParser::class.jvmName, "Error while loading media $aodId", exn)
            AoDMediaNone
        }
    }

    /**
     * get subscription info from aod website, remove "Anime-Abo" Prefix and trim
     */
    suspend fun getSubscriptionInfoAsync(): Deferred<String> {
        return coroutineScope {
            async(Dispatchers.IO) {
                val res = Jsoup.connect(baseUrl + subscriptionPath)
                    .cookieStore(cookieStore)
                    .get()

                return@async res.select("a:contains(Anime-Abo)").text()
                    .removePrefix("Anime-Abo").trim()
            }
        }
    }

    fun getSubscriptionUrl(): String {
        return baseUrl + subscriptionPath
    }

    suspend fun markAsWatched(aodId: Int, episodeId: Int) {
        val episode = getMediaById(aodId).getEpisodeById(episodeId)
        episode.watched = true
        sendCallback(episode.watchedCallback)

        Log.d(AoDParser::class.jvmName, "Marked episode ${episode.mediaId} as watched")
    }

    // TODO don't use jsoup here
    private suspend fun sendCallback(callbackPath: String) = coroutineScope {
        launch(Dispatchers.IO) {
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
                    .cookieStore(cookieStore)
                    .headers(headers)
                    .execute()
            } catch (ex: IOException) {
                Log.e(AoDParser::class.jvmName, "Callback for $callbackPath failed.", ex)
            }
        }
    }

    /**
     * load all media from aod into itemMediaList and mediaList
     * TODO private suspend fun listAnimes() = withContext(Dispatchers.IO) should also work, maybe a bug in android studio?
     */
    private suspend fun listAnimes() = withContext(Dispatchers.IO) {
        launch(Dispatchers.IO) {
            val resAnimes = Jsoup.connect(baseUrl + libraryPath).get()
            //println(resAnimes)

            guiMediaList.clear()
            val animes = resAnimes.select("div.animebox")

            guiMediaList.addAll(
                animes.map {
                    ItemMedia(
                        id = it.select("p.animebox-link").select("a")
                            .attr("href").substringAfterLast("/").toInt(),
                        title = it.select("h3.animebox-title").text(),
                        posterUrl = it.select("p.animebox-image").select("img")
                            .attr("src")
                    )
                }
            )

            Log.i(AoDParser::class.jvmName, "Total library size is: ${guiMediaList.size}")
        }
    }

    /**
     * load new episodes, titles and highlights
     */
    private suspend fun loadHome() = withContext(Dispatchers.IO) {
        launch(Dispatchers.IO) {
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

            Log.i(AoDParser::class.jvmName, "loaded home")
        }
    }

    /**
     * TODO catch SocketTimeoutException from loading to show a waring dialog
     * Load media async. Every media has a playlist.
     * @param aodId The AoD ID of the requested media
     */
    private suspend fun loadMediaAsync(aodId: Int): Deferred<AoDMedia> = coroutineScope {
        return@coroutineScope async (Dispatchers.IO) {
            if (cookieStore.cookies.isEmpty()) login() // TODO is this needed?

            // return none object, if login wasn't successful
            if (!loginSuccess) {
                Log.w(AoDParser::class.jvmName, "Login was not successful")
                return@async AoDMediaNone
            }

            // get the media page
            val res = Jsoup.connect("$baseUrl/anime/$aodId")
                .cookieStore(cookieStore)
                .get()
            // println(res)

            if (csrfToken.isEmpty()) {
                csrfToken = res.select("meta[name=csrf-token]").attr("content")
                Log.d(AoDParser::class.jvmName, "New csrf token is $csrfToken")
            }

            // playlist parsing TODO can this be async to the general info parsing?
            val besides = res.select("div.besides").first()!!
            val aodPlaylists = besides.select("input.streamstarter_html5").map { streamstarter ->
                parsePlaylistAsync(
                    streamstarter.attr("data-playlist"),
                    streamstarter.attr("data-lang")
                )
            }

            /**
             * generic aod media data
             */
            val title = res.select("h1[itemprop=name]").text()
            val description = res.select("div[itemprop=description]").text()
            val posterURL = res.select("img.fullwidth-image").attr("src")
            val type = when {
                posterURL.contains("films") -> MediaType.MOVIE
                posterURL.contains("series") -> MediaType.TVSHOW
                else -> MediaType.OTHER
            }

            var year = 0
            var age = 0
            res.select("table.vertical-table").select("tr").forEach { row ->
                when (row.select("th").text().lowercase(Locale.ROOT)) {
                    "produktionsjahr" -> year = row.select("td").text().toInt()
                    "fsk" -> age = row.select("td").text().toInt()
                }
            }

            // similar titles from media page
            val similar = res.select("h2:contains(Ähnliche Animes)").next().select("li").mapNotNull {
                val mediaId = it.select("a.thumbs").attr("href")
                    .substringAfterLast("/").toIntOrNull()
                val mediaImage = it.select("a.thumbs > img").attr("src")
                val mediaTitle = it.select("a").text()

                if (mediaId != null) {
                    ItemMedia(mediaId, mediaTitle, mediaImage)
                } else {
                    Log.i(AoDParser::class.jvmName, "MediaId for similar to $aodId was null")
                    null
                }
            }

            /**
             * additional information for episodes:
             *  description: a short description of the episode
             *  watched: indicates if the episodes has been watched
             *  watched callback: url to set watched in aod
             */
            val episodesInfo: Map<Int, AoDEpisodeInfo> = if (type == MediaType.TVSHOW) {
                res.select("div.three-box-container > div.episodebox").mapNotNull { episodeBox ->
                    // make sure the episode has a streaming link
                    if (episodeBox.select("input.streamstarter_html5").isNotEmpty()) {
                        val mediaId = episodeBox.select("div.flip-front").attr("id").substringAfter("-").toInt()
                        val episodeShortDesc = episodeBox.select("p.episodebox-shorttext").text()
                        val episodeWatched = episodeBox.select("div.episodebox-icons > div").hasClass("status-icon-orange")
                        val episodeWatchedCallback = episodeBox.select("input.streamstarter_html5").eachAttr("data-playlist").first()

                        AoDEpisodeInfo(mediaId, episodeShortDesc, episodeWatched, episodeWatchedCallback)
                    } else {
                        Log.i(AoDParser::class.jvmName, "Episode info for $aodId has empty streamstarter_html5 ")
                        null
                    }
                }.associateBy { it.aodMediaId }
            } else {
                mapOf()
            }

            // map the aod api playlist to a teapod playlist
            val playlist: List<AoDEpisode> = aodPlaylists.awaitAll().flatMap { aodPlaylist ->
                aodPlaylist.list.mapIndexed { index, episode ->
                    AoDEpisode(
                        mediaId = episode.mediaid,
                        title = episode.title,
                        description = episode.description,
                        shortDesc = episodesInfo[episode.mediaid]?.shortDesc ?: "",
                        imageURL = episode.image,
                        numberStr = episode.title.substringAfter(", Ep. ", ""), // TODO move to parsePalylist
                        index = index,
                        watched = episodesInfo[episode.mediaid]?.watched ?: false,
                        watchedCallback = episodesInfo[episode.mediaid]?.watchedCallback ?: "",
                        streams = mutableListOf(Stream(episode.sources.first().file, aodPlaylist.language))
                    )
                }
            }.groupingBy { it.mediaId }.reduce{ _, accumulator, element ->
                accumulator.copy().also {
                    it.streams.addAll(element.streams)
                }
            }.values.toList()

            return@async AoDMedia(
                aodId = aodId,
                type = type,
                title = title,
                shortText = description,
                posterURL = posterURL,
                year = year,
                age = age,
                similar = similar,
                playlist = playlist
            )
        }
    }

    /**
     * don't use Gson().fromJson() as we don't have any control over the api and it may change
     */
    private fun parsePlaylistAsync(playlistPath: String, language: String): Deferred<AoDPlaylist> {
        if (playlistPath == "[]") {
            return CompletableDeferred(AoDPlaylist(listOf(), Locale.ROOT))
        }

        return CoroutineScope(Dispatchers.IO).async(Dispatchers.IO) {
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
                .cookieStore(cookieStore)
                .headers(headers)
                .timeout(120000) // loading the playlist can take some time
                .execute()

            //Gson().fromJson(res.body(), AoDObject::class.java)

            return@async AoDPlaylist(JsonParser.parseString(res.body()).asJsonObject
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
                // TODO improve language handling (via display language etc.)
                language = when (language) {
                    "ger" -> Locale.GERMAN
                    "jap" -> Locale.JAPANESE
                    else -> Locale.ROOT
                }
            )
        }
    }

}
