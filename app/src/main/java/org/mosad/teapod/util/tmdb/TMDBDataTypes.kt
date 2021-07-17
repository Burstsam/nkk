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

package org.mosad.teapod.util.tmdb

import com.google.gson.annotations.SerializedName

/**
 * These data classes represent the tmdb api json objects.
 * Fields which are nullable in the tmdb api are also nullable here.
 */

abstract class TMDBResult{
    abstract val id: Int
    abstract val name: String
    abstract val overview: String? // for movies tmdb return string or null
    abstract val posterPath: String?
    abstract val backdropPath: String?
}

data class Movie(
    override val id: Int,
    override val name: String,
    override val overview: String?,
    @SerializedName("poster_path")
    override val posterPath: String?,
    @SerializedName("backdrop_path")
    override val backdropPath: String?,
    @SerializedName("release_date")
    val releaseDate: String,
    @SerializedName("runtime")
    val runtime: Int?,
    // TODO generes
): TMDBResult()

data class TVShow(
    override val id: Int,
    override val name: String,
    override val overview: String,
    @SerializedName("poster_path")
    override val posterPath: String?,
    @SerializedName("backdrop_path")
    override val backdropPath: String?,
    @SerializedName("first_air_date")
    val firstAirDate: String,
    @SerializedName("status")
    val status: String,
    // TODO generes
): TMDBResult()

data class TVSeason(
    val id: Int,
    val name: String,
    val overview: String,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("air_date")
    val airDate: String,
    @SerializedName("episodes")
    val episodes: List<TVEpisode>,
    @SerializedName("season_number")
    val seasonNumber: Int
)

data class TVEpisode(
    val id: Int,
    val name: String,
    val overview: String,
    @SerializedName("air_date")
    val airDate: String,
    @SerializedName("episode_number")
    val episodeNumber: Int
)