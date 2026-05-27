package com.maxshpl.myfit.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maxshpl.myfit.R
import com.maxshpl.myfit.settings.ThemeMode
import com.maxshpl.myfit.settings.ThemePreferencesRepository
import com.maxshpl.myfit.settings.settingsDataStore
import com.maxshpl.myfit.ui.theme.MyFitTheme

/**
 * Health Connect требует Activity, обрабатывающую ACTION_SHOW_PERMISSIONS_RATIONALE
 * (Android 13-) и activity-alias VIEW_PERMISSION_USAGE + HEALTH_PERMISSIONS
 * (Android 14+, см. AndroidManifest). На неё ведёт ссылка "Privacy policy" в
 * системном диалоге HC permissions. Тот же экран открывается из Settings → About.
 *
 * Текст политики в strings.xml (privacy_policy_body), RU локаль — основная,
 * EN — fallback с ссылкой на GitHub Pages копию (docs/privacy-policy.md).
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
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.privacy_policy_title)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Назад",
                                    )
                                }
                            },
                        )
                    },
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.privacy_policy_body),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
