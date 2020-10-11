package org.mosad.teapod.util

class DataTypes {
    enum class MediaType {
        OTHER,
        MOVIE,
        TVSHOW
    }
}

data class GUIMedia(val title: String, val posterLink: String, val shortDesc : String, val link: String) {
    override fun toString(): String {
        return title
    }
}

data class StreamMedia(val type: DataTypes.MediaType, val streams: ArrayList<String> = arrayListOf())