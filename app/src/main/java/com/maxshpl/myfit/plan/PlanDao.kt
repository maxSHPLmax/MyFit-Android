package com.maxshpl.myfit.plan

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

data class PlannedMealRowDb(
    val id: Long,
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: DayOfWeek,
    val name: String,
    val time: String?,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    @ColumnInfo(name = "item_count")
    val itemCount: Int,
)

data class PlannedMealItemRowDb(
    val id: Long,
    @ColumnInfo(name = "meal_id")
    val mealId: Long,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "product_name")
    val productName: String,
    val grams: Double,
    @ColumnInfo(name = "kcal_per_100g")
    val kcalPer100g: Int,
    @ColumnInfo(name = "protein_per_100g")
    val proteinPer100g: Float,
    @ColumnInfo(name = "fat_per_100g")
    val fatPer100g: Float,
    @ColumnInfo(name = "carbs_per_100g")
    val carbsPer100g: Float,
)

@Dao
interface PlanDao {

    @Query(
        """
        SELECT
            m.id           AS id,
            m.day_of_week  AS day_of_week,
            m.name         AS name,
            m.time         AS time,
            COALESCE(SUM(p.kcal_per_100g    * i.grams / 100.0), 0.0) AS kcal,
            COALESCE(SUM(p.protein_per_100g * i.grams / 100.0), 0.0) AS protein,
            COALESCE(SUM(p.fat_per_100g     * i.grams / 100.0), 0.0) AS fat,
            COALESCE(SUM(p.carbs_per_100g   * i.grams / 100.0), 0.0) AS carbs,
            COUNT(i.id) AS item_count
        FROM planned_meals m
        LEFT JOIN planned_meal_items i ON i.meal_id = m.id
        LEFT JOIN products p           ON p.id      = i.product_id
        WHERE m.day_of_week = :dayOfWeek
        GROUP BY m.id
        ORDER BY (m.time IS NULL), m.time ASC, m.id ASC
        """,
    )
    fun observeMealsForDay(dayOfWeek: String): Flow<List<PlannedMealRowDb>>

    @Query("SELECT * FROM planned_meals WHERE id = :id")
    suspend fun getMealById(id: Long): PlannedMeal?

    @Query(
        """
        SELECT
            i.id             AS id,
            i.meal_id        AS meal_id,
            i.product_id     AS product_id,
            i.grams          AS grams,
            p.name           AS product_name,
            p.kcal_per_100g  AS kcal_per_100g,
            p.protein_per_100g AS protein_per_100g,
            p.fat_per_100g     AS fat_per_100g,
            p.carbs_per_100g   AS carbs_per_100g
        FROM planned_meal_items i
        JOIN products p ON p.id = i.product_id
        WHERE i.meal_id = :mealId
        ORDER BY i.id ASC
        """,
    )
    fun observeItemsForMeal(mealId: Long): Flow<List<PlannedMealItemRowDb>>

    @Query("SELECT * FROM planned_meal_items WHERE meal_id = :mealId")
    suspend fun getItemsForMealOnce(mealId: Long): List<PlannedMealItem>

    @Insert
    suspend fun insertMeal(meal: PlannedMeal): Long

    @Update
    suspend fun updateMeal(meal: PlannedMeal)

    @Delete
    suspend fun deleteMeal(meal: PlannedMeal)

    @Query("DELETE FROM planned_meals WHERE id = :id")
    suspend fun deleteMealById(id: Long)

    @Insert
    suspend fun insertItems(items: List<PlannedMealItem>)

    @Query("DELETE FROM planned_meal_items WHERE meal_id = :mealId")
    suspend fun deleteItemsForMeal(mealId: Long)

    /**
     * Атомарно сохраняет приём с его items: insert или update meal,
     * затем replace всех items (старые удаляются, новые вставляются).
     * Возвращает id (для нового — сгенерированный, для существующего — тот же).
     */
    @Transaction
    suspend fun upsertMealWithItems(meal: PlannedMeal, items: List<PlannedMealItem>): Long {
        val mealId = if (meal.id == 0L) {
            insertMeal(meal)
        } else {
            updateMeal(meal)
            meal.id
        }
        deleteItemsForMeal(mealId)
        if (items.isNotEmpty()) {
            insertItems(items.map { it.copy(mealId = mealId) })
        }
        return mealId
    }
}
