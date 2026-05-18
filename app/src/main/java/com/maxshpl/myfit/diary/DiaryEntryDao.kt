package com.maxshpl.myfit.diary

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class DiaryRowDb(
    val id: Long,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "meal_type")
    val mealType: MealType,
    val grams: Double,
    @ColumnInfo(name = "product_name")
    val productName: String,
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
interface DiaryEntryDao {

    @Query("SELECT * FROM diary_entries WHERE date = :date ORDER BY id ASC")
    fun observeByDate(date: String): Flow<List<DiaryEntry>>

    @Query(
        """
        SELECT
            e.id          AS id,
            e.product_id  AS product_id,
            e.meal_type   AS meal_type,
            e.grams       AS grams,
            p.name        AS product_name,
            p.kcal_per_100g    AS kcal_per_100g,
            p.protein_per_100g AS protein_per_100g,
            p.fat_per_100g     AS fat_per_100g,
            p.carbs_per_100g   AS carbs_per_100g
        FROM diary_entries e
        JOIN products p ON p.id = e.product_id
        WHERE e.date = :date
        ORDER BY e.id ASC
        """,
    )
    fun observeRowsByDate(date: String): Flow<List<DiaryRowDb>>

    @Query(
        """
        SELECT
            COALESCE(SUM(p.kcal_per_100g     * e.grams / 100.0), 0.0) AS kcal,
            COALESCE(SUM(p.protein_per_100g  * e.grams / 100.0), 0.0) AS protein,
            COALESCE(SUM(p.fat_per_100g      * e.grams / 100.0), 0.0) AS fat,
            COALESCE(SUM(p.carbs_per_100g    * e.grams / 100.0), 0.0) AS carbs
        FROM diary_entries e
        JOIN products p ON p.id = e.product_id
        WHERE e.date = :date
        """,
    )
    fun observeTotalsByDate(date: String): Flow<DayTotals>

    @Query("SELECT COUNT(*) FROM diary_entries WHERE product_id = :productId")
    suspend fun countByProduct(productId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: DiaryEntry): Long

    @Delete
    suspend fun delete(entry: DiaryEntry)
}
