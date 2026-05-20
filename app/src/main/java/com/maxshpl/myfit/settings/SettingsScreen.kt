package com.maxshpl.myfit.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val targetsForm by viewModel.targetsForm.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Настройки") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("theme") {
                ThemeSection(
                    selected = themeMode,
                    onSelect = viewModel::setThemeMode,
                )
            }
            item("targets") {
                TargetsSection(
                    form = targetsForm,
                    onKcalChange = viewModel::setKcal,
                    onProteinChange = viewModel::setProtein,
                    onFatChange = viewModel::setFat,
                    onCarbsChange = viewModel::setCarbs,
                    onSave = viewModel::saveTargets,
                )
            }
        }
    }
}

@Composable
private fun TargetsSection(
    form: TargetsFormState,
    onKcalChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Цели на день",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TargetField(
                label = "Калории, ккал",
                value = form.kcalText,
                error = form.kcalError,
                enabled = form.isReady,
                onChange = onKcalChange,
            )
            TargetField(
                label = "Белки, г",
                value = form.proteinText,
                error = form.proteinError,
                enabled = form.isReady,
                onChange = onProteinChange,
            )
            TargetField(
                label = "Жиры, г",
                value = form.fatText,
                error = form.fatError,
                enabled = form.isReady,
                onChange = onFatChange,
            )
            TargetField(
                label = "Углеводы, г",
                value = form.carbsText,
                error = form.carbsError,
                enabled = form.isReady,
                onChange = onCarbsChange,
            )
            Button(
                onClick = onSave,
                enabled = form.isReady && !form.isSaving && form.isValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetField(
    label: String,
    value: String,
    error: String?,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ThemeSection(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Тема",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ThemeOption(
                label = "Системная",
                hint = "Как в настройках телефона",
                value = ThemeMode.SYSTEM,
                selected = selected,
                onSelect = onSelect,
            )
            ThemeOption(
                label = "Светлая",
                hint = null,
                value = ThemeMode.LIGHT,
                selected = selected,
                onSelect = onSelect,
            )
            ThemeOption(
                label = "Тёмная",
                hint = null,
                value = ThemeMode.DARK,
                selected = selected,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    hint: String?,
    value: ThemeMode,
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = (value == selected),
                onClick = { onSelect(value) },
                role = Role.RadioButton,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = (value == selected),
            onClick = null,
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
