package com.maxshpl.myfit.reminders

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Конфиг напоминаний в DataStore: 13 примитивных ключей (1 main + 4×3 для слотов).
 * Без новых dependencies (kotlinx-serialization не используем).
 */
class RemindersRepository(private val dataStore: DataStore<Preferences>) {

    val currentConfig: Flow<RemindersConfig> = dataStore.data.map { prefs ->
        RemindersConfig(
            enabled = prefs[Keys.MAIN_ENABLED] ?: RemindersConfig.Default.enabled,
            breakfast = readSlot(prefs, MealKind.BREAKFAST),
            lunch = readSlot(prefs, MealKind.LUNCH),
            dinner = readSlot(prefs, MealKind.DINNER),
            snack = readSlot(prefs, MealKind.SNACK),
        )
    }

    suspend fun setMainEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.MAIN_ENABLED] = enabled }
    }

    suspend fun setSlotEnabled(kind: MealKind, enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.slotEnabled(kind)] = enabled }
    }

    suspend fun setSlotTime(kind: MealKind, hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.slotHour(kind)] = hour
            prefs[Keys.slotMinute(kind)] = minute
        }
    }

    private fun readSlot(prefs: Preferences, kind: MealKind): ReminderSlot {
        val default = ReminderSlot.defaultFor(kind)
        return ReminderSlot(
            enabled = prefs[Keys.slotEnabled(kind)] ?: default.enabled,
            hour = prefs[Keys.slotHour(kind)] ?: default.hour,
            minute = prefs[Keys.slotMinute(kind)] ?: default.minute,
        )
    }

    private object Keys {
        val MAIN_ENABLED = booleanPreferencesKey("reminders_main_enabled")

        fun slotEnabled(kind: MealKind) = booleanPreferencesKey("reminder_${kind.name}_enabled")
        fun slotHour(kind: MealKind) = intPreferencesKey("reminder_${kind.name}_hour")
        fun slotMinute(kind: MealKind) = intPreferencesKey("reminder_${kind.name}_minute")
    }
}
