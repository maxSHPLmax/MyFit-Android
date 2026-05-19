package com.maxshpl.myfit.diary

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ActivityLogRowDb(
    val id: Long,
    @ColumnInfo(name = "activity_id")
    val activityId: Long,
    @ColumnInfo(name = "activity_name")
    val activityName: String,
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Double,
    @ColumnInfo(name = "kcal_per_min")
    val kcalPerMin: Double,
)

@Dao
interface ActivityLogDao {

    @Query(
        """
        SELECT
            l.id              AS id,
            l.activity_id     AS activity_id,
            l.duration_minutes AS duration_minutes,
            a.name            AS activity_name,
            a.kcal_per_min    AS kcal_per_min
        FROM activity_log l
        JOIN activities a ON a.id = l.activity_id
        WHERE l.date = :date
        ORDER BY l.id ASC
        """,
    )
    fun observeRowsByDate(date: String): Flow<List<ActivityLogRowDb>>

    @Query(
        """
        SELECT IFNULL(SUM(a.kcal_per_min * l.duration_minutes), 0.0)
        FROM activity_log l
        JOIN activities a ON a.id = l.activity_id
        WHERE l.date = :date
        """,
    )
    fun observeSumKcalByDate(date: String): Flow<Double>

    @Query("SELECT COUNT(*) FROM activity_log WHERE activity_id = :activityId")
    suspend fun countByActivityId(activityId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(log: ActivityLog): Long

    @Query("DELETE FROM activity_log WHERE id = :id")
    suspend fun deleteById(id: Long)
}
