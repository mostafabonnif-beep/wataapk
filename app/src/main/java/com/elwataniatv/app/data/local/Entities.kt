package com.elwataniatv.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_programs")
data class FavoriteProgram(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val youtubeUrl: String,
    val thumbnailUrl: String,
    val date: String,
    val duration: String,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_history")
data class WatchHistoryItem(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val youtubeUrl: String,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class CommentEntity(
    /**
     * Stable local/remote identity. Firestore comments use their document id;
     * local optimistic comments use a generated `local-<uuid>` value.
     */
    @PrimaryKey val remoteId: String,
    val authorName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0
)

@Entity(tableName = "program_reminders")
data class ProgramReminder(
    @PrimaryKey val id: String,
    val programTitle: String,
    val startTime: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
