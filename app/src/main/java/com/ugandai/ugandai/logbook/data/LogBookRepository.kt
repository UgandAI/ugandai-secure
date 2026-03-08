package com.ugandai.ugandai.logbook.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.donatienthorez.ugandai.chat.ui.DatabaseHelper
import com.ugandai.ugandai.logbook.domain.model.ActivityType
import com.ugandai.ugandai.logbook.domain.model.FarmActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LogBookRepository(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)

    private val _activities = MutableStateFlow<List<FarmActivity>>(emptyList())
    val activities: StateFlow<List<FarmActivity>> = _activities.asStateFlow()

    suspend fun loadActivities(userId: String) {
        val activitiesList = withContext(Dispatchers.IO) {
            try {
                Log.d("LogBookRepository", "loadActivities called for userId: $userId")
                val db = dbHelper.readableDatabase
                val cursor = db.rawQuery(
                    "SELECT * FROM farm_activities WHERE user_id = ? ORDER BY date DESC, created_at DESC",
                    arrayOf(userId)
                )

                val list = mutableListOf<FarmActivity>()

                if (cursor.moveToFirst()) {
                    do {
                        val activity = cursorToFarmActivity(cursor)
                        list.add(activity)
                    } while (cursor.moveToNext())
                }

                cursor.close()
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
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put("user_id", activity.userId)
                    put("activity_type", activity.activityType.name)
                    put("date", activity.date)
                    put("crop", activity.crop)
                    put("field", activity.field)
                    put("note", activity.note)
                    put("created_at", activity.createdAt)
                }

                val id = db.insert("farm_activities", null, values)
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
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put("activity_type", activity.activityType.name)
                    put("date", activity.date)
                    put("crop", activity.crop)
                    put("field", activity.field)
                    put("note", activity.note)
                }

                val rowsAffected = db.update(
                    "farm_activities",
                    values,
                    "id = ?",
                    arrayOf(activity.id.toString())
                )

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
                val db = dbHelper.writableDatabase
                val rowsDeleted = db.delete(
                    "farm_activities",
                    "id = ?",
                    arrayOf(activityId.toString())
                )

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

    @SuppressLint("Range")
    private fun cursorToFarmActivity(cursor: android.database.Cursor): FarmActivity {
        return FarmActivity(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            userId = cursor.getString(cursor.getColumnIndexOrThrow("user_id")) ?: "",
            activityType = ActivityType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("activity_type"))),
            date = cursor.getString(cursor.getColumnIndexOrThrow("date")) ?: "",
            crop = cursor.getString(cursor.getColumnIndexOrThrow("crop")) ?: "",
            field = cursor.getString(cursor.getColumnIndexOrThrow("field")) ?: "",
            note = cursor.getString(cursor.getColumnIndexOrThrow("note")) ?: "",
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
        )
    }
}
