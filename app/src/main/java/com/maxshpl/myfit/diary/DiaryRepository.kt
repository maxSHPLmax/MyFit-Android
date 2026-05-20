package com.maxshpl.myfit.diary

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class DiaryRepository(private val dao: DiaryEntryDao) {

    fun entriesForDate(date: LocalDate): Flow<List<DiaryEntry>> =
        dao.observeByDate(date.toString())

    fun rowsForDate(date: LocalDate): Flow<List<DiaryRow>> =
        dao.observeRowsByDate(date.toString()).map { rows -> rows.map { it.toDomain() } }

    fun totalsForDate(date: LocalDate): Flow<DayTotals> =
        dao.observeTotalsByDate(date.toString())

    suspend fun add(
        date: LocalDate,
        productId: Long,
        grams: Double,
        mealType: MealType? = null,
    ): Long = dao.insert(
        DiaryEntry(
            date = date.toString(),
            mealType = mealType,
            productId = productId,
            grams = grams,
        ),
    )

    suspend fun update(entry: DiaryEntry) = dao.update(entry)

    suspend fun getById(id: Long): DiaryEntry? = dao.getById(id)

    suspend fun delete(entry: DiaryEntry) = dao.delete(entry)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun countByProduct(productId: Long): Int = dao.countByProduct(productId)
}

private fun DiaryRowDb.toDomain(): DiaryRow {
    val factor = grams / 100.0
    return DiaryRow(
        entryId = id,
        productId = productId,
        productName = productName,
        mealType = mealType,
        grams = grams,
        kcal = kcalPer100g * factor,
        protein = proteinPer100g * factor,
        fat = fatPer100g * factor,
        carbs = carbsPer100g * factor,
    )
}
