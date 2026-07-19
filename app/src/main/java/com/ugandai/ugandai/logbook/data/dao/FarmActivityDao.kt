package com.ugandai.ugandai.logbook.data.dao

import androidx.room.*
import com.ugandai.ugandai.logbook.data.entity.FarmActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmActivityDao {
    @Query("SELECT * FROM farm_activities WHERE user_id = :userId ORDER BY date DESC, created_at DESC")
    suspend fun getActivities(userId: String): List<FarmActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: FarmActivityEntity): Long

    @Update
    suspend fun updateActivity(activity: FarmActivityEntity): Int

    @Query("DELETE FROM farm_activities WHERE id = :activityId AND user_id = :userId")
    suspend fun deleteActivity(activityId: Long, userId: String): Int
}
