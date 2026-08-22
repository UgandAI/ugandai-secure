package com.ugandai.ugandai.logbook.data

import android.content.Context
import android.util.Log
import com.ugandai.ugandai.data.api.LogbookEntryRequest
import com.ugandai.ugandai.data.api.UgandAIApiClient
import com.ugandai.ugandai.logbook.domain.model.ActivityType
import com.ugandai.ugandai.logbook.domain.model.FarmActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LogBookRepository(
    private val context: Context,
    // Removed farmActivityDao
) {

    private val _activities = MutableStateFlow<List<FarmActivity>>(emptyList())
    val activities: StateFlow<List<FarmActivity>> = _activities.asStateFlow()

    suspend fun loadActivities(userId: String) {
        val activitiesList = withContext(Dispatchers.IO) {
            try {
                Log.d("LogBookRepository", "loadActivities from network")
                val responseList = UgandAIApiClient.api.getLogbookEntries()
                val list = responseList.map {
                    FarmActivity(
                        id = it.id.toLong(),
                        userId = it.user_id.toString(),
                        activityType = ActivityType.valueOf(it.activity_type),
                        date = it.date,
                        crop = it.crop,
                        field = it.field,
                        note = it.note ?: ""
                    )
                }
                Log.d("LogBookRepository", "Loaded ${list.size} activities from network")
                list
            } catch (e: Exception) {
                Log.e("LogBookRepository", "Error loading activities from network", e)
                e.printStackTrace()
                emptyList()
            }
        }
        withContext(Dispatchers.Main) {
            _activities.value = activitiesList
        }
    }

    suspend fun saveActivity(activity: FarmActivity): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val request = LogbookEntryRequest(
                    activity_type = activity.activityType.name,
                    date = activity.date,
                    crop = activity.crop,
                    field = activity.field,
                    note = activity.note
                )
                val response = UgandAIApiClient.api.createLogbookEntry(request)
                loadActivities(activity.userId)
                return@withContext Result.success(response.id.toLong())
            } catch (e: Exception) {
                Log.e("LogBookRepository", "Exception during network save", e)
                e.printStackTrace()
                return@withContext Result.failure(e)
            }
        }
    }

    suspend fun updateActivity(activity: FarmActivity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = LogbookEntryRequest(
                    activity_type = activity.activityType.name,
                    date = activity.date,
                    crop = activity.crop,
                    field = activity.field,
                    note = activity.note
                )
                UgandAIApiClient.api.updateLogbookEntry(activity.id.toInt(), request)
                loadActivities(activity.userId)
                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.failure(e)
            }
        }
    }

    suspend fun deleteActivity(activityId: Long, userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                UgandAIApiClient.api.deleteLogbookEntry(activityId.toInt())
                loadActivities(userId)
                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.failure(e)
            }
        }
    }
}
