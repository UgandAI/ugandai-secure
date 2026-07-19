package com.ugandai.ugandai.chat.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ugandai.ugandai.chat.data.Message
import com.ugandai.ugandai.chat.data.MessageStatus
import com.donatienthorez.ugandai.chat.data.api.ProposedActivity

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "is_from_user") val isFromUser: Boolean,
    @ColumnInfo(name = "message_status") val messageStatus: String,
    @ColumnInfo(name = "proposed_activity_type") val proposedActivityType: String?,
    @ColumnInfo(name = "proposed_date") val proposedDate: String?,
    @ColumnInfo(name = "proposed_crop") val proposedCrop: String?,
    @ColumnInfo(name = "proposed_note") val proposedNote: String?,
    @ColumnInfo(name = "proposed_confidence") val proposedConfidence: Double?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

// Mapping extensions
fun MessageEntity.toDomain(): Message {
    val proposed = if (proposedActivityType != null && proposedDate != null && proposedCrop != null) {
        ProposedActivity(
            activityType = proposedActivityType,
            date = proposedDate,
            crop = proposedCrop,
            note = proposedNote,
            confidence = proposedConfidence ?: 0.0
        )
    } else null

    return Message(
        id = id,
        text = text,
        isFromUser = isFromUser,
        messageStatus = when (messageStatus) {
            "Sent" -> MessageStatus.Sent
            "Error" -> MessageStatus.Error
            else -> MessageStatus.Sending
        },
        proposedActivity = proposed
    )
}

fun Message.toEntity(username: String, time: Long): MessageEntity {
    return MessageEntity(
        id = id,
        username = username,
        text = text,
        isFromUser = isFromUser,
        messageStatus = when (messageStatus) {
            MessageStatus.Sent -> "Sent"
            MessageStatus.Error -> "Error"
            MessageStatus.Sending -> "Sending"
        },
        proposedActivityType = proposedActivity?.activityType,
        proposedDate = proposedActivity?.date,
        proposedCrop = proposedActivity?.crop,
        proposedNote = proposedActivity?.note,
        proposedConfidence = proposedActivity?.confidence,
        createdAt = time
    )
}
