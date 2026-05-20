package com.maxshpl.myfit.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HeaderDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale.forLanguageTag("ru"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onAddProductClick: (LocalDate) -> Unit,
    onAddActivityClick: (LocalDate) -> Unit,
    onEditEntryClick: (Long) -> Unit,
    viewModel: DiaryViewModel = viewModel(factory = DiaryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDeleteRow by remember { mutableStateOf<DiaryRow?>(null) }
    var pendingDeleteLog by remember { mutableStateOf<ActivityLogRow?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val isToday = state.date == today

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    DateSwitcherTitle(
                        date = state.date,
                        onPrev = viewModel::goPrev,
                        onNext = viewModel::goNext,
                        onDateClick = { showDatePicker = true },
                    )
                },
                actions = {
                    if (!isToday) {
                        TextButton(onClick = viewModel::goToday) {
                            Text("Сегодня")
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("dashboard") {
                DashboardSection(
                    totals = state.totals,
                    targets = state.targets,
                    burnedKcal = state.burnedKcal,
                )
            }
            item("plan") { PlanSection() }
            item("food") {
                FoodSection(
                    rows = state.rows,
                    onAddClick = { onAddProductClick(state.date) },
                    onRowClick = { row -> onEditEntryClick(row.entryId) },
                    onSwipeRow = { row -> pendingDeleteRow = row },
                )
            }
            item("activities") {
                ActivitiesSection(
                    logs = state.activityLogs,
                    onAddClick = { onAddActivityClick(state.date) },
                    onSwipeLog = { log -> pendingDeleteLog = log },
                )
            }
        }
    }

    if (showDatePicker) {
        DiaryDatePickerDialog(
            initialDate = state.date,
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                viewModel.setDate(picked)
                showDatePicker = false
            },
        )
    }

    pendingDeleteRow?.let { row ->
        AlertDialog(
            onDismissRequest = { pendingDeleteRow = null },
            title = { Text("Удалить запись?") },
            text = {
                Text(
                    "«${row.productName}» — ${formatGrams(row.grams)} г, " +
                        "${formatKcal(row.kcal)} ккал. Будет удалена без " +
                        "возможности восстановления.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(row.entryId)
                    pendingDeleteRow = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRow = null }) { Text("Отмена") }
            },
        )
    }

    pendingDeleteLog?.let { log ->
        AlertDialog(
            onDismissRequest = { pendingDeleteLog = null },
            title = { Text("Удалить активность?") },
            text = {
                Text(
                    "«${log.activityName}» — ${formatGrams(log.durationMinutes)} мин, " +
                        "${formatKcal(log.kcalBurned)} ккал. Будет удалена без " +
                        "возможности восстановления.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteActivityLog(log.logId)
                    pendingDeleteLog = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteLog = null }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun DateSwitcherTitle(
    date: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDateClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Предыдущий день",
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onDateClick)
                .padding(vertical = 4.dp),
        ) {
            Text(
                text = "Дневник",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = date.format(HeaderDateFormatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Следующий день",
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val initialMillis = initialDate
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onConfirm(picked)
                    } else {
                        onDismiss()
                    }
                },
            ) { Text("Ок") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun PlanSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "План на сегодня",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Плана на сегодня нет",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
