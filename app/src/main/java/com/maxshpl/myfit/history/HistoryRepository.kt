package com.maxshpl.myfit.history

import com.maxshpl.myfit.diary.ActivityLogDao
import com.maxshpl.myfit.diary.DiaryEntryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

data class DayHistoryItem(
    val date: LocalDate,
    val kcalIn: Double,
    val proteinG: Double,
    val fatG: Double,
    val carbsG: Double,
    val kcalBurned: Double,
) {
    val balanceKcal: Double get() = kcalIn - kcalBurned
}

class HistoryRepository(
    private val diaryEntryDao: DiaryEntryDao,
    private val activityLogDao: ActivityLogDao,
) {

    /**
     * Один Flow, который держит ряд за каждый день в диапазоне [start..end] включительно.
     * Дни без записей возвращаются с нулями (а не пропускаются) — это важно для графиков,
     * чтобы ширина бара была одинакова для каждого дня.
     */
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DayHistoryItem>> =
        combine(
            diaryEntryDao.observeDailyTotalsByRange(start.toString(), end.toString()),
            activityLogDao.observeDailyKcalByRange(start.toString(), end.toString()),
        ) { diaryRows, burnedRows ->
            val diaryByDate = diaryRows.associateBy { it.date }
            val burnedByDate = burnedRows.associateBy { it.date }
            buildList(capacity = daysBetween(start, end)) {
                var cursor = start
                while (!cursor.isAfter(end)) {
                    val key = cursor.toString()
                    val d = diaryByDate[key]
                    val b = burnedByDate[key]
                    add(
                        DayHistoryItem(
                            date = cursor,
                            kcalIn = d?.kcal ?: 0.0,
                            proteinG = d?.protein ?: 0.0,
                            fatG = d?.fat ?: 0.0,
                            carbsG = d?.carbs ?: 0.0,
                            kcalBurned = b?.kcalBurned ?: 0.0,
                        ),
                    )
                    cursor = cursor.plusDays(1)
                }
            }
        }

    private fun daysBetween(start: LocalDate, end: LocalDate): Int =
        (end.toEpochDay() - start.toEpochDay() + 1).toInt().coerceAtLeast(1)
}
