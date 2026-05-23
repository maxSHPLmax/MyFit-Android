package com.maxshpl.myfit.health

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HealthConnectPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    val enabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.ENABLED] ?: false
    }

    suspend fun setEnabled(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.ENABLED] = value }
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("health_connect_enabled")
    }
}
