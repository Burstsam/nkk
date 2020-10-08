package org.mosad.teapod.preferences

object Preferences {

    var login = ""
        internal set
    var password = ""
        internal set


    fun saveCredentials(login: String, password: String) {
        this.login = login
        this.password = password

        // TODO save
    }

    fun load() {
        // TODO

    }
}