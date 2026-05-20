package com.maxshpl.myfit.diary

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeParseException

class DiaryDateRepository(private val dataStore: DataStore<Preferences>) {

    val lastViewedDate: Flow<LocalDate> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.LAST_VIEWED_DATE]
        val today = LocalDate.now()
        val stored = raw?.let(::parseOrNull) ?: return@map today
        if (stored.isBefore(today.minusDays(STALE_THRESHOLD_DAYS))) today else stored
    }

    suspend fun setLastViewedDate(date: LocalDate) {
        dataStore.edit { prefs -> prefs[Keys.LAST_VIEWED_DATE] = date.toString() }
    }

    private fun parseOrNull(raw: String): LocalDate? = try {
        LocalDate.parse(raw)
    } catch (_: DateTimeParseException) {
        null
    }

    private object Keys {
        val LAST_VIEWED_DATE = stringPreferencesKey("diary_last_viewed_date")
    }

    private companion object {
        // Если последняя сохранённая дата старше 3 дней — открываем сегодня.
        const val STALE_THRESHOLD_DAYS = 3L
    }
}
