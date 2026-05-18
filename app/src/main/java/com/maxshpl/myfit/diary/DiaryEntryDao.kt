package com.maxshpl.myfit.diary

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryEntryDao {

    @Query("SELECT * FROM diary_entries WHERE date = :date ORDER BY id ASC")
    fun observeByDate(date: String): Flow<List<DiaryEntry>>

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
