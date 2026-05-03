package com.zionhuang.music.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * A dedicated, lightweight Room database that tracks user interactions with
 * songs.  It lives in a separate file ("interactions.db") from the main
 * song/library database ("song.db") so that the interaction log can be
 * cleared or migrated independently.
 *
 * Access this database only through [InteractionRepository] — never call
 * [dao] directly from UI or service code.
 */
@Database(
    entities = [SongInteraction::class],
    version = 1,
    exportSchema = true,
)
abstract class InteractionDatabase : RoomDatabase() {

    abstract val dao: InteractionDao

    companion object {
        const val DB_NAME = "interactions.db"

        /**
         * Creates (or opens) the singleton database.
         * Prefer injecting [InteractionDatabase] via Hilt rather than calling
         * this directly.
         */
        fun newInstance(context: Context): InteractionDatabase =
            Room.databaseBuilder(context, InteractionDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()   // safe for an analytics log
                .build()
    }
}
