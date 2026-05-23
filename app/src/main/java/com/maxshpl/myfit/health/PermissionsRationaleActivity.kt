package com.maxshpl.myfit.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maxshpl.myfit.settings.ThemeMode
import com.maxshpl.myfit.settings.ThemePreferencesRepository
import com.maxshpl.myfit.settings.settingsDataStore
import com.maxshpl.myfit.ui.theme.MyFitTheme

/**
 * Health Connect требует Activity, обрабатывающую ACTION_SHOW_PERMISSIONS_RATIONALE.
 * На неё ведёт ссылка "Privacy policy" в системном диалоге HC permissions.
 *
 * B-5a — заглушка. Реальный текст политики добавляется в B-5b вместе с активити-алиасом
 * VIEW_PERMISSION_USAGE для Android 14+ и хостингом markdown-копии на GitHub Pages.
 */
@OptIn(ExperimentalMaterial3Api::class)
class PermissionsRationaleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val themeRepository = ThemePreferencesRepository(applicationContext.settingsDataStore)
        setContent {
            val themeMode by themeRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            MyFitTheme(themeMode = themeMode) {
                Scaffold(
                    topBar = { TopAppBar(title = { Text("Конфиденциальность") }) },
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                    ) {
                        Text(
                            text = "Полная политика конфиденциальности появится в следующем " +
                                "обновлении приложения.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}
