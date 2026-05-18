package com.maxshpl.myfit.activities

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.flow.Flow

sealed interface SaveActivityResult {
    data class Success(val id: Long) : SaveActivityResult
    data object DuplicateName : SaveActivityResult
}

sealed interface DeleteActivityResult {
    data object Success : DeleteActivityResult
    data object InUse : DeleteActivityResult
}

class ActivityRepository(private val dao: ActivityDao) {

    val activities: Flow<List<Activity>> = dao.observeAll()

    suspend fun getById(id: Long): Activity? = dao.getById(id)

    suspend fun insert(activity: Activity): SaveActivityResult = try {
        SaveActivityResult.Success(dao.insert(activity))
    } catch (e: SQLiteConstraintException) {
        SaveActivityResult.DuplicateName
    }

    suspend fun update(activity: Activity): SaveActivityResult = try {
        dao.update(activity)
        SaveActivityResult.Success(activity.id)
    } catch (e: SQLiteConstraintException) {
        SaveActivityResult.DuplicateName
    }

    suspend fun delete(activity: Activity): DeleteActivityResult = try {
        dao.delete(activity)
        DeleteActivityResult.Success
    } catch (e: SQLiteConstraintException) {
        DeleteActivityResult.InUse
    }
}
