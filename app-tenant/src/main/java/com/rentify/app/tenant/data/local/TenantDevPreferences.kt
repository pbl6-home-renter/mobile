package com.rentify.app.tenant.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tenantDevDataStore: DataStore<Preferences> by preferencesDataStore(name = "rentify_tenant_dev_prefs")

class TenantDevPreferences(private val context: Context) {

    val devTweakHasRoom: Flow<Boolean?> = context.tenantDevDataStore.data.map { preferences ->
        preferences[KEY_DEV_TWEAK_HAS_ROOM]
    }

    suspend fun setDevTweakHasRoom(hasRoom: Boolean?) {
        context.tenantDevDataStore.edit { preferences ->
            if (hasRoom == null) {
                preferences.remove(KEY_DEV_TWEAK_HAS_ROOM)
            } else {
                preferences[KEY_DEV_TWEAK_HAS_ROOM] = hasRoom
            }
        }
    }

    companion object {
        private val KEY_DEV_TWEAK_HAS_ROOM = booleanPreferencesKey("dev_tweak_has_room")
    }
}

