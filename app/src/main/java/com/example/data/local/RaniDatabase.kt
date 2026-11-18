package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ActivityLogEntity::class,
        MemoryPreferenceEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RaniDatabase : RoomDatabase() {
    abstract fun raniDao(): RaniDao

    companion object {
        @Volatile
        private var instance: RaniDatabase? = null

        fun getInstance(context: Context): RaniDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RaniDatabase::class.java,
                    "rani_ai_local.db"
                ).build().also { instance = it }
            }
        }
    }
}
