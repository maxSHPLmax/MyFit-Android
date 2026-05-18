package com.maxshpl.myfit.diary

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
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
    ],
    indices = [
        Index("product_id"),
        Index("date"),
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
)
