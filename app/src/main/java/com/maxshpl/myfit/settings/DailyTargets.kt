package com.maxshpl.myfit.settings

data class DailyTargets(
    val kcal: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int,
) {
    companion object {
        val Default = DailyTargets(
            kcal = 2000,
            proteinG = 120,
            fatG = 60,
            carbsG = 250,
        )
    }
}
