package com.maxshpl.myfit.diary

data class ActivityLogRow(
    val logId: Long,
    val activityId: Long,
    val activityName: String,
    val durationMinutes: Double,
    val kcalBurned: Double,
)
