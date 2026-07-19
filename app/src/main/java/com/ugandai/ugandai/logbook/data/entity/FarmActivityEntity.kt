package com.ugandai.ugandai.logbook.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ugandai.ugandai.logbook.domain.model.ActivityType
import com.ugandai.ugandai.logbook.domain.model.FarmActivity

@Entity(tableName = "farm_activities")
data class FarmActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "activity_type") val activityType: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "crop") val crop: String?,
    @ColumnInfo(name = "field") val field: String?,
    @ColumnInfo(name = "note") val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

// Mapping extensions
fun FarmActivityEntity.toDomain(): FarmActivity {
    return FarmActivity(
        id = id,
        userId = userId,
        activityType = runCatching { ActivityType.valueOf(activityType) }.getOrDefault(ActivityType.OTHER),
        date = date,
        crop = crop.orEmpty(),
        field = field.orEmpty(),
        note = note.orEmpty(),
        createdAt = createdAt
    )
}

fun FarmActivity.toEntity(): FarmActivityEntity {
    return FarmActivityEntity(
        id = id,
        userId = userId,
        activityType = activityType.name,
        date = date,
        crop = crop,
        field = field,
        note = note,
        createdAt = createdAt
    )
}
