package com.maxshpl.myfit.diary

data class DiaryRow(
    val entryId: Long,
    val productId: Long,
    val productName: String,
    val mealType: MealType?,
    val grams: Double,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
)
