package com.maxshpl.myfit.diary

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.maxshpl.myfit.activities.Activity

@Entity(
    tableName = "activity_log",
    foreignKeys = [
        ForeignKey(
            entity = Activity::class,
            parentColumns = ["id"],
            childColumns = ["activity_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("activity_id"),
        Index("date"),
    ],
)
data class ActivityLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: String,
    @ColumnInfo(name = "activity_id")
    val activityId: Long,
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Double,
)
