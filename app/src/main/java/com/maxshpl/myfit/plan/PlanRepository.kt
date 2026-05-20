package com.maxshpl.myfit.plan

import com.maxshpl.myfit.diary.DiaryEntry
import com.maxshpl.myfit.diary.DiaryEntryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate

class PlanRepository(
    private val planDao: PlanDao,
    private val diaryEntryDao: DiaryEntryDao,
) {

    fun observeMealsForDay(dayOfWeek: DayOfWeek): Flow<List<PlannedMealRow>> =
        planDao.observeMealsForDay(dayOfWeek.name).map { rows -> rows.map { it.toDomain() } }

    fun observeItemsForMeal(mealId: Long): Flow<List<PlannedMealItemRow>> =
        planDao.observeItemsForMeal(mealId).map { rows -> rows.map { it.toDomain() } }

    suspend fun getMealById(id: Long): PlannedMeal? = planDao.getMealById(id)

    suspend fun saveMeal(meal: PlannedMeal, items: List<PlannedMealItem>): Long =
        planDao.upsertMealWithItems(meal, items)

    suspend fun deleteMealById(id: Long) = planDao.deleteMealById(id)

    /**
     * Копирует продукты планового приёма в diary_entries за указанную дату.
     * Каждая созданная запись помечается from_meal_id для возможности отмены.
     * Если приём пустой — ничего не делаем.
     */
    suspend fun applyMealToDate(mealId: Long, date: LocalDate) {
        val items = planDao.getItemsForMealOnce(mealId)
        if (items.isEmpty()) return
        val dateStr = date.toString()
        diaryEntryDao.insertAll(
            items.map { item ->
                DiaryEntry(
                    date = dateStr,
                    productId = item.productId,
                    grams = item.grams,
                    fromMealId = mealId,
                )
            },
        )
    }

    /**
     * Удаляет из дневника все записи, помеченные этим mealId за указанную дату.
     */
    suspend fun unapplyMealFromDate(mealId: Long, date: LocalDate) {
        diaryEntryDao.deleteByMealAndDate(mealId, date.toString())
    }

    fun observeAppliedMealIds(date: LocalDate): Flow<Set<Long>> =
        diaryEntryDao.observeAppliedMealIds(date.toString()).map { it.toSet() }
}

private fun PlannedMealRowDb.toDomain(): PlannedMealRow = PlannedMealRow(
    id = id,
    dayOfWeek = dayOfWeek,
    name = name,
    time = time,
    kcal = kcal,
    protein = protein,
    fat = fat,
    carbs = carbs,
    itemCount = itemCount,
)

private fun PlannedMealItemRowDb.toDomain(): PlannedMealItemRow {
    val factor = grams / 100.0
    return PlannedMealItemRow(
        id = id,
        mealId = mealId,
        productId = productId,
        productName = productName,
        grams = grams,
        kcal = kcalPer100g * factor,
        protein = proteinPer100g * factor,
        fat = fatPer100g * factor,
        carbs = carbsPer100g * factor,
    )
}
