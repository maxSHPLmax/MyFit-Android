package com.maxshpl.myfit.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TargetsRepository(private val dataStore: DataStore<Preferences>) {

    val targets: Flow<DailyTargets> = dataStore.data.map { prefs ->
        DailyTargets(
            kcal = prefs[Keys.KCAL] ?: DailyTargets.Default.kcal,
            proteinG = prefs[Keys.PROTEIN_G] ?: DailyTargets.Default.proteinG,
            fatG = prefs[Keys.FAT_G] ?: DailyTargets.Default.fatG,
            carbsG = prefs[Keys.CARBS_G] ?: DailyTargets.Default.carbsG,
        )
    }

    private object Keys {
        val KCAL = intPreferencesKey("daily_target_kcal")
        val PROTEIN_G = intPreferencesKey("daily_target_protein_g")
        val FAT_G = intPreferencesKey("daily_target_fat_g")
        val CARBS_G = intPreferencesKey("daily_target_carbs_g")
    }
}
