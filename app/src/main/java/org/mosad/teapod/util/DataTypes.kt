package org.mosad.teapod.util

data class GUIMedia(val title: String, val posterLink: String, val shortDesc : String, val link: String) {
    override fun toString(): String {
        return title
    }
}