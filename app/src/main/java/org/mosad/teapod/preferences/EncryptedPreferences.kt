package org.mosad.teapod.preferences

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.mosad.teapod.R

object EncryptedPreferences {

    var login = ""
        internal set
    var password = ""
        internal set

    fun saveCredentials(login: String, password: String, context: Context) {
        this.login = login
        this.password = password

        with(getEncryptedPreferences(context)?.edit()) {
            this?.putString(context.getString(R.string.save_key_user_login), login)
            this?.putString(context.getString(R.string.save_key_user_password), password)
            this?.apply()
        }
    }

    fun readCredentials(context: Context) {
        with(getEncryptedPreferences(context)) {
            login = this?.getString(context.getString(R.string.save_key_user_login), "").toString()
            password = this?.getString(context.getString(R.string.save_key_user_password), "").toString()
        }
    }

    /**
     * create a encrypted shared preference
     */
    private fun getEncryptedPreferences(context: Context): SharedPreferences? {
        return try {
            val spec = KeyGenParameterSpec.Builder(
                MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE)
                .build()

            val masterKey = MasterKey.Builder(context)
                .setKeyGenParameterSpec(spec)
                .build()

            EncryptedSharedPreferences.create(
                context,
                context.getString(R.string.encrypted_preference_file_key),
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (ex: Exception) {
            Log.e(javaClass.name, "Could not create encrypted shared preference.", ex)
            null
        }
    }
}