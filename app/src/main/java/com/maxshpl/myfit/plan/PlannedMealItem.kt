package com.maxshpl.myfit.plan

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.maxshpl.myfit.products.Product

@Entity(
    tableName = "planned_meal_items",
    foreignKeys = [
        ForeignKey(
            entity = PlannedMeal::class,
            parentColumns = ["id"],
            childColumns = ["meal_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("meal_id"),
        Index("product_id"),
    ],
)
data class PlannedMealItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "meal_id")
    val mealId: Long,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    val grams: Double,
)
