/**
 * Teapod
 *
 * Copyright 2020-2022  <seil0@mosad.xyz>
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

package org.mosad.teapod.util.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * These data classes represent the tmdb api json objects.
 * Fields which are nullable in the tmdb api are also nullable here.
 */

interface TMDBResult {
    val id: Int
    val name: String
    val overview: String? // for movies tmdb return string or null
    val posterPath: String?
    val backdropPath: String?
}

data class TMDBBase(
    override val id: Int,
    override val name: String,
    override val overview: String?,
    override val posterPath: String?,
    override val backdropPath: String?
) : TMDBResult

/**
 * search results for movie and tv show
 */

@Serializable
data class TMDBSearch<T>(
    val page: Int,
    val results: List<T>
)

@Serializable
data class TMDBSearchResultMovie(
    @SerialName("id") override val id: Int,
    @SerialName("title") override val name: String,
    @SerialName("overview") override val overview: String?,
    @SerialName("poster_path") override val posterPath: String?,
    @SerialName("backdrop_path") override val backdropPath: String?,
) : TMDBResult

@Serializable
data class TMDBSearchResultTVShow(
    @SerialName("id") override val id: Int,
    @SerialName("name") override val name: String,
    @SerialName("overview") override val overview: String?,
    @SerialName("poster_path") override val posterPath: String?,
    @SerialName("backdrop_path") override val backdropPath: String?,
) : TMDBResult

val NoneTMDBSearch = TMDBSearch<TMDBBase>(0, emptyList())
val NoneTMDBSearchMovie = TMDBSearch<TMDBSearchResultMovie>(0, emptyList())
val NoneTMDBSearchTVShow = TMDBSearch<TMDBSearchResultTVShow>(0, emptyList())

/**
 * detail return data types
 */

@Serializable
data class TMDBMovie(
    @SerialName("id") override val id: Int,
    @SerialName("title") override val name: String, // for movies the name is in the field title
    @SerialName("overview") override val overview: String?,
    @SerialName("poster_path") override val posterPath: String?,
    @SerialName("backdrop_path") override val backdropPath: String?,
    @SerialName("release_date") val releaseDate: String,
    @SerialName("runtime") val runtime: Int?,
    @SerialName("status") val status: String,
    // TODO generes
) : TMDBResult

@Serializable
data class TMDBTVShow(
    @SerialName("id")override val id: Int,
    @SerialName("name")override val name: String,
    @SerialName("overview")override val overview: String,
    @SerialName("poster_path") override val posterPath: String?,
    @SerialName("backdrop_path") override val backdropPath: String?,
    @SerialName("first_air_date") val firstAirDate: String,
    @SerialName("last_air_date") val lastAirDate: String,
    @SerialName("status") val status: String,
    // TODO generes
) : TMDBResult

// use null for nullable types, the gui needs to handle/implement a fallback for null values
val NoneTMDB = TMDBBase(0, "", "", null, null)
val NoneTMDBMovie = TMDBMovie(0, "", "", null, null, "", null, "")
val NoneTMDBTVShow = TMDBTVShow(0, "", "", null, null, "", "", "")

@Serializable
data class TMDBTVSeason(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("overview") val overview: String,
    @SerialName("poster_path") val posterPath: String?,
    @SerialName("air_date") val airDate: String,
    @SerialName("episodes") val episodes: List<TMDBTVEpisode>,
    @SerialName("season_number") val seasonNumber: Int
)

@Serializable
data class TMDBTVEpisode(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("overview") val overview: String,
    @SerialName("air_date") val airDate: String,
    @SerialName("episode_number") val episodeNumber: Int
)

// use null for nullable types, the gui needs to handle/implement a fallback for null values
val NoneTMDBTVSeason = TMDBTVSeason(0, "", "", null, "", emptyList(), 0)
