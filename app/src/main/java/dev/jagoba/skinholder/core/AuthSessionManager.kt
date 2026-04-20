package dev.jagoba.skinholder.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSessionManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSession(token: String, username: String, userId: Int) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .putInt(KEY_USER_ID, userId)
            .apply()
    }

    fun saveSessionWithPassword(token: String, username: String, userId: Int, password: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_SAVED_PASSWORD, password)
            .apply()
    }

    fun getSavedPassword(): String? = prefs.getString(KEY_SAVED_PASSWORD, null)

    fun clearSavedPassword() {
        prefs.edit().remove(KEY_SAVED_PASSWORD).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, 0)

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun clearSession() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USERNAME).remove(KEY_USER_ID).remove(KEY_SAVED_PASSWORD).apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USERNAME = "auth_username"
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_SAVED_PASSWORD = "auth_saved_password"
    }
}
