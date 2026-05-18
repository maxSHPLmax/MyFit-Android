package com.maxshpl.myfit.activities

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Query("SELECT * FROM activities ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Activity>>

    @Query("SELECT * FROM activities WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Activity?

    @Query("SELECT COUNT(*) FROM activities")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(activity: Activity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(activities: List<Activity>): List<Long>

    @Update
    suspend fun update(activity: Activity)

    @Delete
    suspend fun delete(activity: Activity)
}
