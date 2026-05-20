package com.maxshpl.myfit.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxshpl.myfit.core.formatKcal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayCardDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("ru"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToDiary: () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("История") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("period") {
                PeriodSwitcher(
                    selected = state.period,
                    onSelect = viewModel::setPeriod,
                )
            }
            item("kcalChart") {
                ChartCard(title = "Потребление, ккал") {
                    KcalBarChart(days = state.days, targetKcal = state.targets.kcal)
                    KcalLegend(modifier = Modifier.padding(top = 8.dp))
                }
            }
            item("macroChart") {
                ChartCard(title = "Б/Ж/У по дням") {
                    MacroStackChart(days = state.days)
                    MacroLegend(modifier = Modifier.padding(top = 8.dp))
                }
            }
            item("daysHeader") {
                Text(
                    text = "Дни",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(state.days.reversed(), key = { it.date.toEpochDay() }) { day ->
                DayCard(item = day, onClick = {
                    viewModel.openDiary(day.date)
                    onNavigateToDiary()
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSwitcher(
    selected: HistoryPeriod,
    onSelect: (HistoryPeriod) -> Unit,
) {
    val options = remember { HistoryPeriod.entries.toList() }
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, period ->
            SegmentedButton(
                selected = period == selected,
                onClick = { onSelect(period) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(
                    when (period) {
                        HistoryPeriod.WEEK -> "Неделя"
                        HistoryPeriod.MONTH -> "Месяц"
                    },
                )
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayCard(item: DayHistoryItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.date.format(DayCardDateFormatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Stat(label = "Съел", value = "${formatKcal(item.kcalIn)}")
                Stat(label = "Сжёг", value = "${formatKcal(item.kcalBurned)}")
                Stat(
                    label = "Баланс",
                    value = formatBalance(item.balanceKcal),
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatBalance(value: Double): String {
    val rounded = value.toInt()
    val sign = if (rounded > 0) "+" else ""
    return "$sign$rounded"
}
