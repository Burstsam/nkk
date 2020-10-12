package org.mosad.teapod.util

class DataTypes {
    enum class MediaType {
        OTHER,
        MOVIE,
        TVSHOW
    }
}

// TODO rework: add type, episodes list with episode title, if type == MOVIE the first episode will be the movie stream
data class GUIMedia(val title: String, val posterLink: String, val shortDesc : String, val link: String) {
    override fun toString(): String {
        return title
    }
}

data class StreamMedia(val type: DataTypes.MediaType, val streams: ArrayList<String> = arrayListOf())