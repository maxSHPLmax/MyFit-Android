package com.maxshpl.myfit.health

/**
 * Суммарные данные за один день из Health Connect.
 *
 * activeKcal — ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL за день (без BMR).
 * steps — StepsRecord.COUNT_TOTAL за день. В B-5a UI не использует, читаем для будущего тикета.
 */
data class BurnedKcalSummary(
    val activeKcal: Double,
    val steps: Long,
) {
    companion object {
        val Empty = BurnedKcalSummary(activeKcal = 0.0, steps = 0L)
    }
}
