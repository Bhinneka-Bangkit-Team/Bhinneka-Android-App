package com.capstone.komunitas.data.db

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

private const val KEY_SAVED_AT = "key_saved_at"
private const val KEY_AUTH_TOKEN = "key_auth_token"

class PreferenceProvider(
    context: Context
) {
    private val appContext = context.applicationContext

    private val preference: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(appContext)

    fun savelastSavedAt(savedAt: String) {
        preference.edit().putString(
            KEY_SAVED_AT,
            savedAt
        ).apply()
    }

    fun clearLastSavedAt(){
        preference.edit().remove(KEY_SAVED_AT).apply()
    }

    fun getLastSavedAt(): String? {
        return preference.getString(KEY_SAVED_AT, null)
    }

    fun saveAuthToken(authToken: String) {
        preference.edit().putString(
            KEY_AUTH_TOKEN,
            authToken
        ).apply()
    }

    fun getAuthToken(): String? {
        return preference.getString(KEY_AUTH_TOKEN, null)
    }

    fun clearAuthToken(){
        preference.edit().remove(KEY_AUTH_TOKEN).apply()
    }

}