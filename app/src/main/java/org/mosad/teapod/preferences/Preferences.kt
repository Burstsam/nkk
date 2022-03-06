package org.mosad.teapod.preferences

import android.content.Context
import android.content.SharedPreferences
import org.mosad.teapod.R
import org.mosad.teapod.util.DataTypes
import java.util.*

object Preferences {

    var preferredLocale: Locale = Locale.forLanguageTag("en-US") // TODO this should be saved (potential offline usage) but fetched on start
        internal set
    var preferSubbed = false
        internal set
    var autoplay = true
        internal set
    var devSettings = false
        internal set
    var theme = DataTypes.Theme.DARK
        internal set

    private fun getSharedPref(context: Context): SharedPreferences {
        return context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )
    }

    fun savePreferredLocal(context: Context, preferredLocale: Locale) {
        with(getSharedPref(context).edit()) {
            putString(context.getString(R.string.save_key_preferred_local), preferredLocale.toLanguageTag())
            apply()
        }

        this.preferredLocale = preferredLocale
    }

    fun savePreferSecondary(context: Context, preferSubbed: Boolean) {
        with(getSharedPref(context).edit()) {
            putBoolean(context.getString(R.string.save_key_prefer_secondary), preferSubbed)
            apply()
        }

        this.preferSubbed = preferSubbed
    }

    fun saveAutoplay(context: Context, autoplay: Boolean) {
        with(getSharedPref(context).edit()) {
            putBoolean(context.getString(R.string.save_key_autoplay), autoplay)
            apply()
        }

        this.autoplay = autoplay
    }

    fun saveDevSettings(context: Context, devSettings: Boolean) {
        with(getSharedPref(context).edit()) {
            putBoolean(context.getString(R.string.save_key_dev_settings), devSettings)
            apply()
        }

        this.devSettings = devSettings
    }

    fun saveTheme(context: Context, theme: DataTypes.Theme) {
        with(getSharedPref(context).edit()) {
            putString(context.getString(R.string.save_key_theme), theme.toString())
            apply()
        }

        this.theme = theme
    }

    /**
     * initially load the stored values
     */
    fun load(context: Context) {
        val sharedPref = getSharedPref(context)

        preferredLocale = Locale.forLanguageTag(
            sharedPref.getString(
                context.getString(R.string.save_key_preferred_local), "en-US"
            ) ?: "en-US"
        )
        preferSubbed = sharedPref.getBoolean(
            context.getString(R.string.save_key_prefer_secondary), false
        )
        autoplay = sharedPref.getBoolean(
            context.getString(R.string.save_key_autoplay), true
        )
        devSettings = sharedPref.getBoolean(
            context.getString(R.string.save_key_dev_settings), false
        )
        theme = DataTypes.Theme.valueOf(
            sharedPref.getString(
                context.getString(R.string.save_key_theme), DataTypes.Theme.DARK.toString()
            ) ?:  DataTypes.Theme.DARK.toString()
        )
    }


}