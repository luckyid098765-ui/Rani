package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RaniDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 200")
    fun observeActivityLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity)

    @Query("DELETE FROM activity_logs")
    suspend fun clearActivityLogs()

    @Query("SELECT * FROM memory_preferences ORDER BY updatedAt DESC")
    fun observeMemories(): Flow<List<MemoryPreferenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemory(memory: MemoryPreferenceEntity)

    @Query("DELETE FROM memory_preferences WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM memory_preferences")
    suspend fun clearMemories()

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC LIMIT 120")
    fun observeChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatMessages()
}
