package com.maxshpl.myfit.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Семантические цвета для diet-приложения. Намеренно фиксированные хексы
// вместо ColorScheme — пользователь ожидает зелёный/жёлтый/красный
// независимо от темы (dark/light).
private val KcalGray = Color(0xFFBDBDBD)
private val KcalYellow = Color(0xFFFFA726)
private val KcalGreen = Color(0xFF66BB6A)
private val KcalRed = Color(0xFFEF5350)

private val ProteinColor = Color(0xFF42A5F5)
private val FatColor = Color(0xFFFFCA28)
private val CarbsColor = Color(0xFFAB47BC)

private val ChartHeight = 140.dp
private val BarWidthFraction = 0.65f

@Composable
fun KcalBarChart(
    days: List<DayHistoryItem>,
    targetKcal: Int,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return

    val maxKcal = (days.maxOf { it.kcalIn }).coerceAtLeast(targetKcal.toDouble())
        .coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartHeight),
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val y = (1f - (targetKcal.toFloat() / maxKcal.toFloat())) * size.height
                if (y in 0f..size.height) {
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    drawLine(
                        color = Color(0xFF999999),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2f,
                        pathEffect = dashEffect,
                        cap = StrokeCap.Round,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                days.forEach { day ->
                    KcalBar(
                        kcal = day.kcalIn,
                        maxKcal = maxKcal,
                        targetKcal = targetKcal,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            days.forEach { day ->
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun KcalBar(
    kcal: Double,
    maxKcal: Double,
    targetKcal: Int,
    modifier: Modifier = Modifier,
) {
    val heightFraction = (kcal / maxKcal).toFloat().coerceIn(0f, 1f)
    val color = when {
        kcal <= 0.0 -> KcalGray
        kcal < targetKcal * 0.80 -> KcalYellow
        kcal <= targetKcal * 1.10 -> KcalGreen
        else -> KcalRed
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (heightFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(BarWidthFraction)
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(color),
            )
        } else {
            // Минимальный плоский маркер для пустых дней — чтобы было видно сетку
            Box(
                modifier = Modifier
                    .fillMaxWidth(BarWidthFraction)
                    .height(2.dp)
                    .background(KcalGray),
            )
        }
    }
}

@Composable
fun MacroStackChart(
    days: List<DayHistoryItem>,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return

    val maxTotal = days.maxOf { it.proteinG + it.fatG + it.carbsG }
        .coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartHeight),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            days.forEach { day ->
                MacroStackBar(
                    protein = day.proteinG,
                    fat = day.fatG,
                    carbs = day.carbsG,
                    maxTotal = maxTotal,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            days.forEach { day ->
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MacroStackBar(
    protein: Double,
    fat: Double,
    carbs: Double,
    maxTotal: Double,
    modifier: Modifier = Modifier,
) {
    val total = protein + fat + carbs
    val totalFraction = (total / maxTotal).toFloat().coerceIn(0f, 1f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (totalFraction <= 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(BarWidthFraction)
                    .height(2.dp)
                    .background(KcalGray),
            )
            return@Box
        }
        Column(
            modifier = Modifier
                .fillMaxWidth(BarWidthFraction)
                .fillMaxHeight(totalFraction)
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
        ) {
            // Порядок сверху вниз: У (carbs) - Ж (fat) - Б (protein).
            // У обычно самый большой → удобно показывать как "верх" стека.
            val carbsFraction = (carbs / total).toFloat()
            val fatFraction = (fat / total).toFloat()
            val proteinFraction = (protein / total).toFloat()
            if (carbsFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(carbsFraction)
                        .background(CarbsColor),
                )
            }
            if (fatFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(fatFraction)
                        .background(FatColor),
                )
            }
            if (proteinFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(proteinFraction)
                        .background(ProteinColor),
                )
            }
        }
    }
}

@Composable
fun KcalLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LegendItem(color = KcalYellow, label = "< 80% цели")
        LegendItem(color = KcalGreen, label = "норма")
        LegendItem(color = KcalRed, label = "> 110% цели")
    }
}

@Composable
fun MacroLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LegendItem(color = ProteinColor, label = "Б")
        LegendItem(color = FatColor, label = "Ж")
        LegendItem(color = CarbsColor, label = "У")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
