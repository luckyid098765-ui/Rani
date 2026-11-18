package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val language: String,
    val category: String,
    val resultSummary: String,
    val status: String
)

@Entity(tableName = "memory_preferences")
data class MemoryPreferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prefKey: String,
    val prefValue: String,
    val category: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user", "rani", "system"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPlaceholderAi: Boolean = false,
    val actionBadge: String? = null
)
