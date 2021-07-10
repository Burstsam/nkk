package org.mosad.teapod.util.tmdb

abstract class TMDBResult{
    abstract val id: Int
    abstract val name: String?
    abstract val overview: String?
    abstract val posterPath: String?
    abstract val backdropPath: String?
}

data class Movie(
    override val id: Int,
    override val name: String? = null,
    override val overview: String? = null,
    override val posterPath: String? = null,
    override val backdropPath: String? = null,
    val releaseDate: String? = null,
    val runtime: Int? = null
    // TODO generes
): TMDBResult()

data class TVShow(
    override val id: Int,
    override val name: String? = null,
    override val overview: String? = null,
    override val posterPath: String? = null,
    override val backdropPath: String? = null,
    val firstAirDate: String? = null,
    val status: String? = null,
    // TODO generes
): TMDBResult()

data class TVSeason(
    val id: Int,
    val name: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val airDate: String? = null,
    val episodes: List<TVEpisode>? = null,
    val seasonNumber: Int? = null
)

// TODO decide whether to use nullable or not
data class TVEpisode(
    val id: Int,
    val name: String? = null,
    val overview: String? = null,
    val airDate: String? = null,
    val episodeNumber: Int? = null
)