package com.ugandai.ugandai.logbook.ui

import com.ugandai.ugandai.logbook.domain.model.ActivityType
import com.ugandai.ugandai.logbook.domain.model.FarmActivity

data class ActivityDraft(
    val activityType: ActivityType,
    val date: String,
    val crop: String = "",
    val field: String = "",
    val note: String = ""
) {
    companion object {
        fun fromActivity(activity: FarmActivity): ActivityDraft {
            return ActivityDraft(
                activityType = activity.activityType,
                date = activity.date,
                crop = activity.crop,
                field = activity.field,
                note = activity.note
            )
        }
    }
}
