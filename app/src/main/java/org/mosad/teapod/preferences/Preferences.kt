package org.mosad.teapod.preferences

import android.content.Context
import org.mosad.teapod.R

object Preferences {

    var preferSecondary = false
        internal set
    var autoplay = true
        internal set

    fun savePreferSecondary(context: Context, preferSecondary: Boolean) {
        val sharedPref = context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )

        with(sharedPref.edit()) {
            putBoolean(context.getString(R.string.save_key_prefer_secondary), preferSecondary)
            apply()
        }

        this.preferSecondary = preferSecondary
    }

    fun saveAutoplay(context: Context, autoplay: Boolean) {
        val sharedPref = context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )

        with(sharedPref.edit()) {
            putBoolean(context.getString(R.string.save_key_autoplay), autoplay)
            apply()
        }

        this.autoplay = autoplay
    }

    /**
     * initially load the stored values
     */
    fun load(context: Context) {
        val sharedPref = context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )

        preferSecondary = sharedPref.getBoolean(
            context.getString(R.string.save_key_prefer_secondary), false
        )
        autoplay = sharedPref.getBoolean(
            context.getString(R.string.save_key_autoplay), true
        )
    }


}