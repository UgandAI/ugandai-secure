package com.ugandai.ugandai.logbook.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
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
        withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM farm_activities WHERE user_id = ? ORDER BY date DESC, created_at DESC",
                arrayOf(userId)
            )

            val activitiesList = mutableListOf<FarmActivity>()

            if (cursor.moveToFirst()) {
                do {
                    val activity = cursorToFarmActivity(cursor)
                    activitiesList.add(activity)
                } while (cursor.moveToNext())
            }

            cursor.close()
            _activities.value = activitiesList
        }
    }

    suspend fun saveActivity(activity: FarmActivity): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
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
                if (id != -1L) {
                    loadActivities(activity.userId)
                    Result.success(id)
                } else {
                    Result.failure(Exception("Failed to save activity"))
                }
            } catch (e: Exception) {
                Result.failure(e)
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
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to update activity"))
                }
            } catch (e: Exception) {
                Result.failure(e)
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
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete activity"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    @SuppressLint("Range")
    private fun cursorToFarmActivity(cursor: android.database.Cursor): FarmActivity {
        return FarmActivity(
            id = cursor.getLong(cursor.getColumnIndex("id")),
            userId = cursor.getString(cursor.getColumnIndex("user_id")),
            activityType = ActivityType.valueOf(cursor.getString(cursor.getColumnIndex("activity_type"))),
            date = cursor.getString(cursor.getColumnIndex("date")),
            crop = cursor.getString(cursor.getColumnIndex("crop")) ?: "",
            field = cursor.getString(cursor.getColumnIndex("field")) ?: "",
            note = cursor.getString(cursor.getColumnIndex("note")) ?: "",
            createdAt = cursor.getLong(cursor.getColumnIndex("created_at"))
        )
    }
}
