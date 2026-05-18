package com.maxshpl.myfit.activities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activities",
    indices = [Index(value = ["name"], unique = true)],
)
data class Activity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "kcal_per_min")
    val kcalPerMin: Double,
)
