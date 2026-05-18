package com.maxshpl.myfit.products

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "kcal_per_100g")
    val kcalPer100g: Int,
    @ColumnInfo(name = "protein_per_100g")
    val proteinPer100g: Float,
    @ColumnInfo(name = "fat_per_100g")
    val fatPer100g: Float,
    @ColumnInfo(name = "carbs_per_100g")
    val carbsPer100g: Float,
    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean,
    @ColumnInfo(name = "is_hidden", defaultValue = "0")
    val isHidden: Boolean = false,
)
