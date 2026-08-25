package com.elwataniatv.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorite_programs ORDER BY savedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteProgram>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_programs WHERE id = :id)")
    fun isFavorite(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(program: FavoriteProgram)

    @Query("DELETE FROM favorite_programs WHERE id = :id")
    suspend fun deleteFavorite(id: String)
}

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY updatedAt DESC LIMIT 20")
    fun getWatchHistory(): Flow<List<WatchHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: WatchHistoryItem)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearHistory()
}

@Dao
interface CommentsDao {
    @Query("SELECT * FROM comments ORDER BY timestamp DESC LIMIT 50")
    fun getComments(): Flow<List<CommentEntity>>

    @Query("SELECT COUNT(*) FROM comments")
    suspend fun getCommentsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

}

@Dao
interface RemindersDao {
    @Query("SELECT * FROM program_reminders ORDER BY createdAt DESC")
    fun getReminders(): Flow<List<ProgramReminder>>

    @Query("SELECT * FROM program_reminders ORDER BY createdAt DESC")
    suspend fun getAllReminders(): List<ProgramReminder>

    /** Blocking read retained only for BootReceiver's background executor. */
    @Query("SELECT * FROM program_reminders ORDER BY createdAt DESC")
    fun getAllRemindersSync(): List<ProgramReminder>

    @Query("SELECT * FROM program_reminders WHERE id = :id LIMIT 1")
    fun getReminderByIdSync(id: String): ProgramReminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReminder(reminder: ProgramReminder)

    @Query("DELETE FROM program_reminders WHERE id = :id")
    suspend fun deleteReminder(id: String)
}
