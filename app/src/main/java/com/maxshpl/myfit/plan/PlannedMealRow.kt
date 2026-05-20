package com.maxshpl.myfit.plan

import java.time.DayOfWeek

data class PlannedMealRow(
    val id: Long,
    val dayOfWeek: DayOfWeek,
    val name: String,
    val time: String?,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val itemCount: Int,
)

data class PlannedMealItemRow(
    val id: Long,
    val mealId: Long,
    val productId: Long,
    val productName: String,
    val grams: Double,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
)
