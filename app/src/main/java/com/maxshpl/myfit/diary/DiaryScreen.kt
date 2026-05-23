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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxshpl.myfit.core.formatGrams
import com.maxshpl.myfit.core.formatKcal
import com.maxshpl.myfit.core.formatMacro
import com.maxshpl.myfit.core.formatMinutes
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
    onEditActivityLogClick: (Long) -> Unit,
    onOpenPlanClick: () -> Unit,
    viewModel: DiaryViewModel = viewModel(factory = DiaryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // ID-based pendingDelete: храним только entry/log id, объект ищем в state.
    // rememberSaveable переживает rotation и process death (Long имеет встроенный Saver).
    // Если запись удалена параллельно (другим источником), find вернёт null →
    // диалог автоматически закрывается, что корректнее текущего поведения.
    var pendingDeleteRowId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDeleteLogId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val isToday = state.date == today

    // ON_RESUME → перечитываем HC данные за сегодня. Сценарий: юзер пробежался,
    // часы синкнулись в HC, юзер вернулся в MyFit — без этого триггера BurnedTile
    // показывал бы кешированное (устаревшее) значение из repository.
    //
    // rememberUpdatedState — selectedDate берётся свежий на каждом emit, иначе
    // observer закроет старое значение через захват lambda. Также: проверяем
    // LocalDate.now() в момент resume, не закэшированный `today` — приложение
    // могло пролежать свёрнутым через полночь.
    val viewModelState = rememberUpdatedState(viewModel)
    val selectedDateState = rememberUpdatedState(state.date)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (selectedDateState.value == LocalDate.now()) {
                    viewModelState.value.refreshTodayHealthData()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                    manualBurnedKcal = state.manualBurnedKcal,
                    hcBurnedKcal = state.hcBurnedKcal,
                )
            }
            item("plan") {
                PlanSection(
                    plannedMeals = state.plannedMeals,
                    onApplyMeal = viewModel::applyPlannedMeal,
                    onUnapplyMeal = viewModel::unapplyPlannedMeal,
                    onOpenPlanClick = onOpenPlanClick,
                )
            }
            item("food") {
                FoodSection(
                    rows = state.rows,
                    onAddClick = { onAddProductClick(state.date) },
                    onRowClick = { row -> onEditEntryClick(row.entryId) },
                    onSwipeRow = { row -> pendingDeleteRowId = row.entryId },
                )
            }
            item("activities") {
                ActivitiesSection(
                    logs = state.activityLogs,
                    onAddClick = { onAddActivityClick(state.date) },
                    onRowClick = { log -> onEditActivityLogClick(log.logId) },
                    onSwipeLog = { log -> pendingDeleteLogId = log.logId },
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

    pendingDeleteRowId?.let { id ->
        val row = state.rows.find { it.entryId == id }
        if (row == null) {
            pendingDeleteRowId = null
        } else {
            AlertDialog(
                onDismissRequest = { pendingDeleteRowId = null },
                title = { Text("Удалить запись?") },
                text = {
                    Text(
                        "«${row.productName}» — ${formatGrams(row.grams)}, " +
                            "${formatKcal(row.kcal)} ккал. Будет удалена без " +
                            "возможности восстановления.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.delete(row.entryId)
                        pendingDeleteRowId = null
                    }) { Text("Удалить") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteRowId = null }) { Text("Отмена") }
                },
            )
        }
    }

    pendingDeleteLogId?.let { id ->
        val log = state.activityLogs.find { it.logId == id }
        if (log == null) {
            pendingDeleteLogId = null
        } else {
            AlertDialog(
                onDismissRequest = { pendingDeleteLogId = null },
                title = { Text("Удалить активность?") },
                text = {
                    Text(
                        "«${log.activityName}» — ${formatMinutes(log.durationMinutes)}, " +
                            "${formatKcal(log.kcalBurned)} ккал. Будет удалена без " +
                            "возможности восстановления.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteActivityLog(log.logId)
                        pendingDeleteLogId = null
                    }) { Text("Удалить") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteLogId = null }) { Text("Отмена") }
                },
            )
        }
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
private fun PlanSection(
    plannedMeals: List<PlannedMealOnDiary>,
    onApplyMeal: (Long) -> Unit,
    onUnapplyMeal: (Long) -> Unit,
    onOpenPlanClick: () -> Unit,
) {
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
            if (plannedMeals.isEmpty()) {
                Text(
                    text = "Плана на сегодня нет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                TextButton(
                    onClick = onOpenPlanClick,
                    modifier = Modifier.padding(top = 4.dp),
                ) { Text("Открыть план") }
            } else {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    plannedMeals.forEach { meal ->
                        PlannedMealRowOnDiary(
                            meal = meal,
                            onApply = { onApplyMeal(meal.id) },
                            onUnapply = { onUnapplyMeal(meal.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannedMealRowOnDiary(
    meal: PlannedMealOnDiary,
    onApply: () -> Unit,
    onUnapply: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (meal.isApplied) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Съедено",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (!meal.time.isNullOrBlank()) {
                    Text(
                        text = meal.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "${formatKcal(meal.kcal)} ккал · " +
                    "Б ${formatMacro(meal.protein)} · " +
                    "Ж ${formatMacro(meal.fat)} · " +
                    "У ${formatMacro(meal.carbs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = if (meal.isApplied) onUnapply else onApply,
        ) {
            Text(if (meal.isApplied) "Убрать" else "Съел")
        }
    }
}
