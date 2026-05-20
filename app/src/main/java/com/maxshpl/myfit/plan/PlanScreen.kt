package com.maxshpl.myfit.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxshpl.myfit.core.formatKcal
import com.maxshpl.myfit.core.formatMacro
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    onAddMealClick: (DayOfWeek) -> Unit,
    onEditMealClick: (Long) -> Unit,
    viewModel: PlanViewModel = viewModel(factory = PlanViewModel.Factory),
) {
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val meals by viewModel.meals.collectAsStateWithLifecycle()
    var pendingDeleteMeal by remember { mutableStateOf<PlannedMealRow?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("План") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddMealClick(selectedDay) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Приём") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            DayOfWeekSelector(
                selected = selectedDay,
                onSelect = viewModel::setSelectedDay,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            )
            if (meals.isEmpty()) {
                EmptyDayState(day = selectedDay)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(meals, key = { it.id }) { meal ->
                        SwipeableMealCard(
                            meal = meal,
                            onClick = { onEditMealClick(meal.id) },
                            onSwipe = { pendingDeleteMeal = meal },
                        )
                    }
                }
            }
        }
    }

    pendingDeleteMeal?.let { meal ->
        AlertDialog(
            onDismissRequest = { pendingDeleteMeal = null },
            title = { Text("Удалить приём?") },
            text = {
                Text(
                    "«${meal.name}» будет удалён из плана. Записи в дневнике, " +
                        "созданные из этого приёма, останутся — просто потеряют связь.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMeal(meal.id)
                    pendingDeleteMeal = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteMeal = null }) { Text("Отмена") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayOfWeekSelector(
    selected: DayOfWeek,
    onSelect: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = remember { DayOfWeek.entries.toList() }
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        days.forEachIndexed { index, day ->
            SegmentedButton(
                selected = selected == day,
                onClick = { onSelect(day) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = days.size),
            ) {
                Text(day.shortRu())
            }
        }
    }
}

@Composable
private fun EmptyDayState(day: DayOfWeek) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "На ${day.fullRu().lowercase()} плана нет",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Нажмите «+ Приём», чтобы добавить",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableMealCard(
    meal: PlannedMealRow,
    onClick: () -> Unit,
    onSwipe: () -> Unit,
) {
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    val currentOnClick by rememberUpdatedState(onClick)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                currentOnSwipe()
            }
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground() },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        modifier = Modifier.padding(horizontal = 12.dp),
    ) {
        MealCard(meal = meal, onClick = { currentOnClick() })
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealCard(meal: PlannedMealRow, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (!meal.time.isNullOrBlank()) {
                    Text(
                        text = meal.time,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = if (meal.itemCount == 0) {
                    "Продукты не добавлены"
                } else {
                    "${meal.itemCount} прод. · ${formatKcal(meal.kcal)} ккал · " +
                        "Б ${formatMacro(meal.protein)} · " +
                        "Ж ${formatMacro(meal.fat)} · " +
                        "У ${formatMacro(meal.carbs)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
