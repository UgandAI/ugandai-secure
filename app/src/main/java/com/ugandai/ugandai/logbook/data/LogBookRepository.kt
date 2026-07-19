package com.ugandai.ugandai.logbook.data

import android.content.Context
import android.util.Log
import com.ugandai.ugandai.logbook.data.dao.FarmActivityDao
import com.ugandai.ugandai.logbook.data.entity.toDomain
import com.ugandai.ugandai.logbook.data.entity.toEntity
import com.ugandai.ugandai.logbook.domain.model.FarmActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LogBookRepository(
    private val context: Context,
    private val farmActivityDao: FarmActivityDao
) {

    private val _activities = MutableStateFlow<List<FarmActivity>>(emptyList())
    val activities: StateFlow<List<FarmActivity>> = _activities.asStateFlow()

    suspend fun loadActivities(userId: String) {
        val activitiesList = withContext(Dispatchers.IO) {
            try {
                Log.d("LogBookRepository", "loadActivities called for userId: $userId")
                val entities = farmActivityDao.getActivities(userId)
                val list = entities.map { it.toDomain() }
                Log.d("LogBookRepository", "Loaded ${list.size} activities")
                list
            } catch (e: Exception) {
                Log.e("LogBookRepository", "Error loading activities", e)
                e.printStackTrace()
                emptyList()
            }
        }
        // Update StateFlow on Main dispatcher
        withContext(Dispatchers.Main) {
            _activities.value = activitiesList
            Log.d("LogBookRepository", "StateFlow updated with ${activitiesList.size} activities")
        }
    }

    suspend fun saveActivity(activity: FarmActivity): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("LogBookRepository", "saveActivity called: userId=${activity.userId}, type=${activity.activityType}, date=${activity.date}")
                val entity = activity.toEntity()
                val id = farmActivityDao.insertActivity(entity)
                Log.d("LogBookRepository", "Insert returned id: $id")
                if (id != -1L) {
                    loadActivities(activity.userId)
                    Log.d("LogBookRepository", "Save successful, returning success")
                    return@withContext Result.success(id)
                } else {
                    Log.e("LogBookRepository", "Insert failed, id was -1")
                    return@withContext Result.failure(Exception("Failed to save activity"))
                }
            } catch (e: Exception) {
                Log.e("LogBookRepository", "Exception during save", e)
                e.printStackTrace()
                return@withContext Result.failure(e)
            }
        }
    }

    suspend fun updateActivity(activity: FarmActivity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = activity.toEntity()
                val rowsAffected = farmActivityDao.updateActivity(entity)
                if (rowsAffected > 0) {
                    loadActivities(activity.userId)
                    return@withContext Result.success(Unit)
                } else {
                    return@withContext Result.failure(Exception("Failed to update activity"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.failure(e)
            }
        }
    }

    suspend fun deleteActivity(activityId: Long, userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val rowsDeleted = farmActivityDao.deleteActivity(activityId, userId)
                if (rowsDeleted > 0) {
                    loadActivities(userId)
                    return@withContext Result.success(Unit)
                } else {
                    return@withContext Result.failure(Exception("Failed to delete activity"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.failure(e)
            }
        }
    }
}
