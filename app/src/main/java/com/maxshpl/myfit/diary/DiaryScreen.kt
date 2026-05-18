package com.maxshpl.myfit.diary

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale.forLanguageTag("ru"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onAddClick: (MealType) -> Unit,
    viewModel: DiaryViewModel = viewModel(factory = DiaryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<DiaryRow?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Дневник")
                        Text(
                            text = state.date.format(DateFormatter)
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = { DayTotalsBar(totals = state.totals) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MealType.entries.forEach { meal ->
                item(key = "meal-${meal.name}") {
                    MealSection(
                        meal = meal,
                        rows = state.rowsByMeal[meal].orEmpty(),
                        onAddClick = { onAddClick(meal) },
                        onSwipeRow = { row -> pendingDelete = row },
                    )
                }
            }
        }
    }

    pendingDelete?.let { row ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить запись?") },
            text = {
                Text(
                    "«${row.productName}» — ${formatGrams(row.grams)} г, " +
                        "${formatKcal(row.kcal)} ккал. Будет удалена без возможности восстановления.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(row.entryId)
                    pendingDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun MealSection(
    meal: MealType,
    rows: List<DiaryRow>,
    onAddClick: () -> Unit,
    onSwipeRow: (DiaryRow) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = meal.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (rows.isNotEmpty()) {
                    Text(
                        text = "${formatKcal(rows.sumOf { it.kcal })} ккал",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            if (rows.isEmpty()) {
                Text(
                    text = "Пока пусто",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            } else {
                Column {
                    rows.forEach { row ->
                        SwipeableDiaryRow(
                            row = row,
                            onSwipe = { onSwipeRow(row) },
                        )
                    }
                }
            }
            TextButton(
                onClick = onAddClick,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Добавить продукт")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableDiaryRow(
    row: DiaryRow,
    onSwipe: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onSwipe()
            }
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground() },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
    ) {
        DiaryRowItem(row = row)
    }
}

@Composable
private fun SwipeBackground() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.errorContainer),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = "Удалить",
            color = scheme.onErrorContainer,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 16.dp),
        )
    }
}

@Composable
private fun DiaryRowItem(row: DiaryRow) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.productName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${formatGrams(row.grams)} г",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${formatKcal(row.kcal)} ккал · " +
                "Б ${formatMacro(row.protein)} · " +
                "Ж ${formatMacro(row.fat)} · " +
                "У ${formatMacro(row.carbs)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayTotalsBar(totals: DayTotals) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "Итого за день",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TotalCell(value = formatKcal(totals.kcal), label = "ккал")
                TotalCell(value = formatMacro(totals.protein), label = "Б, г")
                TotalCell(value = formatMacro(totals.fat), label = "Ж, г")
                TotalCell(value = formatMacro(totals.carbs), label = "У, г")
            }
        }
    }
}

@Composable
private fun TotalCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun MealType.displayName(): String = when (this) {
    MealType.Breakfast -> "Завтрак"
    MealType.Lunch -> "Обед"
    MealType.Dinner -> "Ужин"
    MealType.Snack -> "Перекус"
}

private fun formatKcal(value: Double): String = value.toInt().toString()

private fun formatMacro(value: Double): String {
    val rounded = (value * 10).toInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "%.1f".format(rounded)
}

private fun formatGrams(value: Double): String = formatMacro(value)
