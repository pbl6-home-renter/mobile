package com.rentify.app.core.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rentify_token_prefs")

class TokenDataStore(private val context: Context) {

    val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACCESS_TOKEN]
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_REFRESH_TOKEN]
    }

    val userId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_ID]
    }

    val userRole: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_ROLE]
    }

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String? = null,
        userId: String? = null,
        userRole: String? = null
    ) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = accessToken
            refreshToken?.let { preferences[KEY_REFRESH_TOKEN] = it }
            userId?.let { preferences[KEY_USER_ID] = it }
            userRole?.let { preferences[KEY_USER_ROLE] = it }
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_ACCESS_TOKEN)
            preferences.remove(KEY_REFRESH_TOKEN)
            preferences.remove(KEY_USER_ID)
            preferences.remove(KEY_USER_ROLE)
        }
    }

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
    }
}
