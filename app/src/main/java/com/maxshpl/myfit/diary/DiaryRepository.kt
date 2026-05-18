package com.maxshpl.myfit.diary

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DiaryRepository(private val dao: DiaryEntryDao) {

    fun entriesForDate(date: LocalDate): Flow<List<DiaryEntry>> =
        dao.observeByDate(date.toString())

    fun totalsForDate(date: LocalDate): Flow<DayTotals> =
        dao.observeTotalsByDate(date.toString())

    suspend fun add(date: LocalDate, mealType: MealType, productId: Long, grams: Double): Long =
        dao.insert(
            DiaryEntry(
                date = date.toString(),
                mealType = mealType,
                productId = productId,
                grams = grams,
            ),
        )

    suspend fun delete(entry: DiaryEntry) = dao.delete(entry)

    suspend fun countByProduct(productId: Long): Int = dao.countByProduct(productId)
}
