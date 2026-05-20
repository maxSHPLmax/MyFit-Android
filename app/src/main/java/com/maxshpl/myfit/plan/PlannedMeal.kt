package com.maxshpl.myfit.plan

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(
    tableName = "planned_meals",
    indices = [Index("day_of_week")],
)
data class PlannedMeal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: DayOfWeek,
    val name: String,
    val time: String? = null,
)
