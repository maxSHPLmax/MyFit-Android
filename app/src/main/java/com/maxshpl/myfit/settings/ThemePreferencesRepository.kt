package com.maxshpl.myfit.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemePreferencesRepository(private val dataStore: DataStore<Preferences>) {

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let(::parseOrNull) ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode.name }
    }

    private fun parseOrNull(raw: String): ThemeMode? = try {
        ThemeMode.valueOf(raw)
    } catch (_: IllegalArgumentException) {
        null
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
