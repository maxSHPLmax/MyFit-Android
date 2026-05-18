package com.maxshpl.myfit.diary

data class DayTotals(
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
) {
    companion object {
        val Empty = DayTotals(kcal = 0.0, protein = 0.0, fat = 0.0, carbs = 0.0)
    }
}
