package com.maxshpl.myfit.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maxshpl.myfit.core.formatKcal
import com.maxshpl.myfit.core.formatMacro
import com.maxshpl.myfit.settings.DailyTargets

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
    val isOverTarget = balance > targets.kcal
    val errorColor = MaterialTheme.colorScheme.error
    val balanceColor = if (isOverTarget) errorColor else MaterialTheme.colorScheme.onSurface
    val barColor = if (isOverTarget) errorColor else MaterialTheme.colorScheme.primary
    val balanceBarProgress = (balance.toFloat() / targets.kcal.coerceAtLeast(1).toFloat())
        .coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Цель: ${targets.kcal} ккал/день",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = formatKcal(balance),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = balanceColor,
                )
                Text(
                    text = "Баланс, ккал",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LinearProgressIndicator(
                progress = { balanceBarProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {},
            )

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
    val progress = (eaten.toFloat() / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
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
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                drawStopIndicator = {},
            )
        }
    }
}

/**
 * Три режима отображения:
 * 1) hcBurnedKcal == null (HC выключен или нет permissions): показываем
 *    только ручное "Y ккал" — legacy формат до B-5a.
 * 2) hcBurnedKcal != null, manual == 0: "X ккал" большим + подпись "из Health Connect".
 * 3) hcBurnedKcal != null, manual > 0: "Z ккал" (=X+Y) большим + breakdown
 *    "X (часы) + Y (вручную)" мелким.
 *
 * Случай (hcBurnedKcal == 0.0, manual == 0) попадает в (2): показывается "0 ккал из HC".
 * Это намеренно — отличает "HC включён, но сегодня данных нет" от "HC выключен".
 */
@Composable
private fun BurnedTile(
    manualBurnedKcal: Double,
    hcBurnedKcal: Double?,
    modifier: Modifier = Modifier,
) {
    val totalKcal = manualBurnedKcal + (hcBurnedKcal ?: 0.0)
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
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
            Spacer(Modifier.height(6.dp))
            when {
                hcBurnedKcal == null -> {
                    Spacer(Modifier.height(4.dp))
                }
                manualBurnedKcal == 0.0 -> {
                    Text(
                        "из Health Connect",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    Text(
                        "${formatKcal(hcBurnedKcal)} (часы) + " +
                            "${formatKcal(manualBurnedKcal)} (вручную)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun NutrientChip(
    label: String,
    value: Double,
    target: Int,
    modifier: Modifier = Modifier,
) {
    val progress = (value.toFloat() / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${formatMacro(value)} / $target г",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                drawStopIndicator = {},
            )
        }
    }
}
