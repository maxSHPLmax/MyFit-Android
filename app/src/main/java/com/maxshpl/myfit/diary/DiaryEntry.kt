package com.maxshpl.myfit.diary

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.maxshpl.myfit.plan.PlannedMeal
import com.maxshpl.myfit.products.Product

@Entity(
    tableName = "diary_entries",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.RESTRICT,
        ),
        // ON DELETE SET NULL — намеренное отклонение от ADR-004 (RESTRICT).
        // План не справочник: удаление приёма не должно блокироваться имеющимися
        // записями в дневнике и не должно их каскадно стирать. Запись остаётся,
        // просто теряет связь с источником-планом.
        ForeignKey(
            entity = PlannedMeal::class,
            parentColumns = ["id"],
            childColumns = ["from_meal_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("product_id"),
        Index("date"),
        Index("from_meal_id"),
    ],
)
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: String,
    @ColumnInfo(name = "meal_type")
    val mealType: MealType? = null,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    val grams: Double,
    @ColumnInfo(name = "from_meal_id")
    val fromMealId: Long? = null,
)
