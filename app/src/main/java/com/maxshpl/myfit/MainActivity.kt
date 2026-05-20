package com.maxshpl.myfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.maxshpl.myfit.navigation.AppNav
import com.maxshpl.myfit.settings.ThemeMode
import com.maxshpl.myfit.settings.ThemePreferencesRepository
import com.maxshpl.myfit.settings.settingsDataStore
import com.maxshpl.myfit.ui.theme.MyFitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val themeRepository = ThemePreferencesRepository(applicationContext.settingsDataStore)
        setContent {
            val themeMode by themeRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            MyFitTheme(themeMode = themeMode) {
                AppNav()
            }
        }
    }
}
