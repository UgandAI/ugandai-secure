package com.ugandai.ugandai.logbook.domain.model

data class FarmActivity(
    val id: Long = 0,
    val userId: String,
    val activityType: ActivityType,
    val date: String, // ISO format: YYYY-MM-DD
    val crop: String = "",
    val field: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class ActivityType {
    PLANTED,
    WEEDED,
    FERTILIZED,
    SPRAYED,
    HARVESTED,
    WATERED,
    OTHER;

    fun displayName(): String {
        return when (this) {
            PLANTED -> "Planted"
            WEEDED -> "Weeded"
            FERTILIZED -> "Fertilized"
            SPRAYED -> "Sprayed"
            HARVESTED -> "Harvested"
            WATERED -> "Watered"
            OTHER -> "Other"
        }
    }
}
