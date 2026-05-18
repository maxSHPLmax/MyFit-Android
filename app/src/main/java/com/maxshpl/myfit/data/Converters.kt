package com.maxshpl.myfit.data

import androidx.room.TypeConverter
import com.maxshpl.myfit.diary.MealType

class Converters {

    @TypeConverter
    fun mealTypeToString(value: MealType): String = value.name

    @TypeConverter
    fun mealTypeFromString(value: String): MealType = MealType.valueOf(value)
}
