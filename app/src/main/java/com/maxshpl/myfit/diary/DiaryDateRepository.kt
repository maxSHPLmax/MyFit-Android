package com.maxshpl.myfit.diary

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Чистый read/write для последней просмотренной даты Diary. Без бизнес-логики.
 *
 * Раньше здесь применялся 3-day fallback при чтении: если сохранённая дата
 * старше 3 дней, возвращался today. Это удобно при cold start (открыл app
 * после долгого перерыва — открывается сегодня), но ломало click-through
 * из History: явный переход на день 15 дней назад превращался в today,
 * потому что fallback применялся к каждому emit, не только при init.
 *
 * Теперь репозиторий тупой: возвращает то, что в DataStore, или today если
 * ключа нет. Логику stale-fallback применяет только DiaryViewModel.init —
 * однократно при cold start, см. там.
 */
class DiaryDateRepository(private val dataStore: DataStore<Preferences>) {

    val currentDate: Flow<LocalDate> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_VIEWED_DATE]?.let(::parseOrNull) ?: LocalDate.now()
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
}
