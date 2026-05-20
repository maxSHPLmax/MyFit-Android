package com.maxshpl.myfit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.maxshpl.myfit.diary.DiaryDateRepository
import com.maxshpl.myfit.navigation.AppNav
import com.maxshpl.myfit.reminders.ReminderReceiver
import com.maxshpl.myfit.settings.ThemeMode
import com.maxshpl.myfit.settings.ThemePreferencesRepository
import com.maxshpl.myfit.settings.settingsDataStore
import com.maxshpl.myfit.ui.theme.MyFitTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        val themeRepository = ThemePreferencesRepository(applicationContext.settingsDataStore)
        setContent {
            val themeMode by themeRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            MyFitTheme(themeMode = themeMode) {
                AppNav()
            }
        }
    }

    /**
     * Сценарий из обсуждения B-4: приложение свёрнуто, но живо. Тап на
     * notification доставит intent через onNewIntent, не через onCreate.
     * Без setIntent() getIntent() продолжит возвращать старый — поэтому
     * первая строка тела.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(ReminderReceiver.EXTRA_OPEN_DIARY_TODAY, false)) {
            // Очищаем сразу, чтобы не сработало повторно при rotate/config-change.
            intent.removeExtra(ReminderReceiver.EXTRA_OPEN_DIARY_TODAY)
            val dateRepository = DiaryDateRepository(applicationContext.settingsDataStore)
            lifecycleScope.launch {
                dateRepository.setLastViewedDate(LocalDate.now())
                // DiaryViewModel подписан на dateRepository.currentDate через
                // collect (B-2b hotfix), эта запись автоматически обновит UI.
            }
        }
    }
}
