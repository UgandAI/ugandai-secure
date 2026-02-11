package com.donatienthorez.ugandai.chat.data.api
data class ProposedActivity(
    val activityType: String,
    val crop: String,
    val date: String,
    val note: String?,
    val confidence: Double
)