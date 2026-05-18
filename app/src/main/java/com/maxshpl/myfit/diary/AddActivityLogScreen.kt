package com.maxshpl.myfit.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxshpl.myfit.activities.Activity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityLogScreen(
    onBack: () -> Unit,
    onNavigateToActivities: () -> Unit,
    viewModel: AddActivityLogViewModel = viewModel(factory = AddActivityLogViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить активность") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.selectedActivity != null) {
                                viewModel.clearSelection()
                            } else {
                                onBack()
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.activities.isEmpty() && state.query.isBlank() && state.selectedActivity == null ->
                    EmptyCatalog(onNavigateToActivities = onNavigateToActivities)

                state.selectedActivity == null -> ActivityPicker(
                    query = state.query,
                    activities = state.activities,
                    onQueryChange = viewModel::setQuery,
                    onActivityClick = viewModel::selectActivity,
                )

                else -> DurationEntry(
                    activity = state.selectedActivity!!,
                    durationText = state.durationText,
                    durationError = state.durationError,
                    isSaving = state.isSaving,
                    onDurationChange = viewModel::setDurationText,
                    onSave = viewModel::save,
                )
            }
        }
    }
}

@Composable
private fun EmptyCatalog(onNavigateToActivities: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = "Сначала добавьте активность в справочник",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onNavigateToActivities) {
            Text("Перейти в справочник")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityPicker(
    query: String,
    activities: List<Activity>,
    onQueryChange: (String) -> Unit,
    onActivityClick: (Activity) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Поиск по названию") },
            singleLine = true,
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (activities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Ничего не найдено",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(activities, key = { it.id }) { activity ->
                    ActivityPickerRow(activity = activity, onClick = { onActivityClick(activity) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityPickerRow(activity: Activity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = activity.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${formatDouble(activity.kcalPerMin)} ккал/мин",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DurationEntry(
    activity: Activity,
    durationText: String,
    durationError: String?,
    isSaving: Boolean,
    onDurationChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = activity.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${formatDouble(activity.kcalPerMin)} ккал/мин",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            value = durationText,
            onValueChange = onDurationChange,
            label = { Text("Длительность, мин") },
            singleLine = true,
            isError = durationError != null,
            supportingText = { durationError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        val preview = computeBurnPreview(activity, durationText)
        if (preview != null) {
            HorizontalDivider()
            Text(
                text = "Будет сожжено",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${formatKcal(preview)} ккал",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onSave,
            enabled = !isSaving && durationText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить")
        }
    }
}

private fun computeBurnPreview(activity: Activity, durationText: String): Double? {
    val duration = durationText.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (duration <= 0.0) return null
    return activity.kcalPerMin * duration
}

private fun formatDouble(value: Double): String {
    val rounded = (value * 10).toInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "%.1f".format(rounded)
}
