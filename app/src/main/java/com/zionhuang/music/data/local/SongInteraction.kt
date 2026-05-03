package com.zionhuang.music.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single user interaction event with a song.
 *
 * [eventType] is one of: PLAY, SKIP, LIKE, DISLIKE
 * [playDurationMs] is only meaningful for PLAY events — set to 0 for others.
 * [timestamp] stores epoch-milliseconds (System.currentTimeMillis()) so no
 * type-converter is required and queries can use simple arithmetic.
 */
@Entity(
    tableName = "song_interaction",
    indices = [
        Index(value = ["songId"]),
        Index(value = ["timestamp"]),
    ]
)
data class SongInteraction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val title: String,
    val artist: String,
    /** Epoch-milliseconds (System.currentTimeMillis()). */
    val timestamp: Long = System.currentTimeMillis(),
    /** One of: PLAY, SKIP, LIKE, DISLIKE */
    val eventType: String,
    /** Total played milliseconds for this session. Meaningful only for PLAY events. */
    val playDurationMs: Long = 0L,
) {
    companion object {
        const val EVENT_PLAY = "PLAY"
        const val EVENT_SKIP = "SKIP"
        const val EVENT_LIKE = "LIKE"
        const val EVENT_DISLIKE = "DISLIKE"
    }
}
