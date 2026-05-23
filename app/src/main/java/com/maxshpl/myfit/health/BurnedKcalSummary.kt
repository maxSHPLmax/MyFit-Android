package com.maxshpl.myfit.health

/**
 * Суммарные данные за один день из Health Connect.
 *
 * burnedKcal — TotalCaloriesBurnedRecord.ENERGY_TOTAL за день. ВКЛЮЧАЕТ базовый
 * метаболизм (BMR), не только активность. Изначально планировали ActiveCalories,
 * но Samsung Health не пишет их в HC — только Total. См. диагностику в B-5a PR #12.
 * steps — StepsRecord.COUNT_TOTAL за день. В B-5a UI не использует, читаем для будущего тикета.
 */
data class BurnedKcalSummary(
    val burnedKcal: Double,
    val steps: Long,
) {
    companion object {
        val Empty = BurnedKcalSummary(burnedKcal = 0.0, steps = 0L)
    }
}
