package com.ugandai.ugandai.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ugandai.ugandai.logbook.data.dao.FarmActivityDao
import com.ugandai.ugandai.logbook.data.entity.FarmActivityEntity
import com.ugandai.ugandai.chat.data.dao.MessageDao
import com.ugandai.ugandai.chat.data.entity.MessageEntity

@Database(
    entities = [FarmActivityEntity::class, MessageEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun farmActivityDao(): FarmActivityDao
    abstract fun messageDao(): MessageDao
}
