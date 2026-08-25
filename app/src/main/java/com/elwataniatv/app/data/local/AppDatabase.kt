package com.elwataniatv.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FavoriteProgram::class,
        WatchHistoryItem::class,
        CommentEntity::class,
        ProgramReminder::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun commentsDao(): CommentsDao
    abstract fun remindersDao(): RemindersDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v1 → v2: add the Firestore reconciliation column. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE comments ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * v2 → v3: make remoteId the primary key. Legacy rows without a
         * Firestore id receive a deterministic local identity from old id.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE comments_new (
                        remoteId TEXT NOT NULL,
                        authorName TEXT NOT NULL,
                        content TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        likesCount INTEGER NOT NULL,
                        PRIMARY KEY(remoteId)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO comments_new(remoteId, authorName, content, timestamp, likesCount)
                    SELECT CASE WHEN remoteId = '' THEN 'local-' || id ELSE remoteId END,
                           authorName, content, timestamp, likesCount
                    FROM comments
                """.trimIndent())
                db.execSQL("DROP TABLE comments")
                db.execSQL("ALTER TABLE comments_new RENAME TO comments")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "watania_tv_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
