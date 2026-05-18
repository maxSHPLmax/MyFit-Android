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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
    onAddClick: () -> Unit,
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
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Добавить продукт")
            }
        },
        bottomBar = { DayTotalsBar(totals = state.totals) },
    ) { padding ->
        if (state.rows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Сегодня пока ничего не записано",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.rows, key = { it.entryId }) { row ->
                    SwipeableDiaryRow(
                        row = row,
                        onSwipe = { pendingDelete = row },
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
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

private fun formatKcal(value: Double): String = value.toInt().toString()

private fun formatMacro(value: Double): String {
    val rounded = (value * 10).toInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "%.1f".format(rounded)
}

private fun formatGrams(value: Double): String = formatMacro(value)
