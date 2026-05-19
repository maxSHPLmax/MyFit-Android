package com.maxshpl.myfit.diary

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class ActivityLogRepository(private val dao: ActivityLogDao) {

    fun rowsForDate(date: LocalDate): Flow<List<ActivityLogRow>> =
        dao.observeRowsByDate(date.toString()).map { rows -> rows.map { it.toDomain() } }

    fun sumKcalForDate(date: LocalDate): Flow<Double> =
        dao.observeSumKcalByDate(date.toString())

    suspend fun add(date: LocalDate, activityId: Long, durationMinutes: Double): Long =
        dao.insert(
            ActivityLog(
                date = date.toString(),
                activityId = activityId,
                durationMinutes = durationMinutes,
            ),
        )

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun countByActivity(activityId: Long): Int = dao.countByActivityId(activityId)
}

private fun ActivityLogRowDb.toDomain(): ActivityLogRow = ActivityLogRow(
    logId = id,
    activityId = activityId,
    activityName = activityName,
    durationMinutes = durationMinutes,
    kcalBurned = kcalPerMin * durationMinutes,
)
