package com.maxshpl.myfit.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maxshpl.myfit.core.formatKcal
import com.maxshpl.myfit.core.formatMacro
import com.maxshpl.myfit.core.formatSignedKcal
import com.maxshpl.myfit.settings.DailyTargets

/**
 * Иерархия по макету B-5b:
 * - Цель (label small)
 * - БАЛАНС со знаком (display large) — главный фокус. Формула Eaten - Burned:
 *   "+" = перебор еды над сожжённым, "-" = дефицит. Нейтральный цвет
 *   (без emotional кодировки error/primary) — Lead не хочет окрашивать
 *   "плохо/хорошо", пусть юзер сам интерпретирует.
 * - Съедено / Сожжено (title medium) — текущее vs цель и факт.
 * - БЖУ (body/label small) — детализация макро.
 *
 * Все три иерархии в одной Card. LinearProgressIndicator'ы убраны (не на макете).
 */
@Composable
fun DashboardSection(
    totals: DayTotals,
    targets: DailyTargets,
    manualBurnedKcal: Double,
    hcBurnedKcal: Double?,
    modifier: Modifier = Modifier,
) {
    val totalBurnedKcal = manualBurnedKcal + (hcBurnedKcal ?: 0.0)
    val balance = totals.kcal - totalBurnedKcal

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // --- Балансовый блок ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Цель: ${targets.kcal} ккал/день",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatSignedKcal(balance),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Баланс, ккал",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // --- Съедено / Сожжено ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EatenTile(
                    eaten = totals.kcal,
                    target = targets.kcal,
                    modifier = Modifier.weight(1f),
                )
                BurnedTile(
                    manualBurnedKcal = manualBurnedKcal,
                    hcBurnedKcal = hcBurnedKcal,
                    modifier = Modifier.weight(1f),
                )
            }

            // --- БЖУ ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NutrientChip(
                    label = "Белки",
                    value = totals.protein,
                    target = targets.proteinG,
                    modifier = Modifier.weight(1f),
                )
                NutrientChip(
                    label = "Жиры",
                    value = totals.fat,
                    target = targets.fatG,
                    modifier = Modifier.weight(1f),
                )
                NutrientChip(
                    label = "Углеводы",
                    value = totals.carbs,
                    target = targets.carbsG,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun EatenTile(eaten: Double, target: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            "Съедено",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${formatKcal(eaten)} / $target ккал",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * После B-5b упрощено до одной цифры — без breakdown "(часы)+(вручную)" и подписи
 * "(с БМР)". Сумма manual+HC формируется внутри тайла; наружу разбивки не видно.
 * Логика суммирования с HC данными остаётся (см. DiaryViewModel.hcBurnedFlow).
 */
@Composable
private fun BurnedTile(
    manualBurnedKcal: Double,
    hcBurnedKcal: Double?,
    modifier: Modifier = Modifier,
) {
    val totalKcal = manualBurnedKcal + (hcBurnedKcal ?: 0.0)
    Column(modifier = modifier) {
        Text(
            "Сожжено",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${formatKcal(totalKcal)} ккал",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun NutrientChip(
    label: String,
    value: Double,
    target: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "${formatMacro(value)} / $target г",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
