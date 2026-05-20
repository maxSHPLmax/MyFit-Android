package com.maxshpl.myfit.data

import androidx.room.TypeConverter
import com.maxshpl.myfit.diary.MealType
import java.time.DayOfWeek

class Converters {

    @TypeConverter
    fun mealTypeToString(value: MealType?): String? = value?.name

    @TypeConverter
    fun mealTypeFromString(value: String?): MealType? = value?.let { MealType.valueOf(it) }

    @TypeConverter
    fun dayOfWeekToString(value: DayOfWeek): String = value.name

    @TypeConverter
    fun dayOfWeekFromString(value: String): DayOfWeek = DayOfWeek.valueOf(value)
}
