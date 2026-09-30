package com.ugandai.ugandai.auth.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class AuthTokenStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        "secure_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context.applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun token(): String? = preferences.getString("user_token", null)

    fun save(token: String, username: String) {
        require(token.isNotBlank()) { "Token cannot be blank" }
        preferences.edit().putString("user_token", token).putString("username", username).apply()
    }

    fun clear() {
        preferences.edit().remove("user_token").remove("username").apply()
    }
}
