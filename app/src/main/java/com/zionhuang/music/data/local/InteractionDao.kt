package com.zionhuang.music.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [SongInteraction].
 *
 * All query functions are suspending so callers must already be inside a
 * coroutine scope (Dispatchers.IO is recommended at the call-site).
 */
@Dao
interface InteractionDao {

    /** Persist a new interaction. Rows are auto-assigned an id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(interaction: SongInteraction): Long

    /**
     * Returns the [limit] songs with the highest cumulative play-duration,
     * considering PLAY events only. Songs are ordered from most- to least-played.
     */
    @Query(
        """
        SELECT songId, title, artist, SUM(playDurationMs) AS totalMs
        FROM song_interaction
        WHERE eventType = 'PLAY'
        GROUP BY songId
        ORDER BY totalMs DESC
        LIMIT :limit
        """
    )
    suspend fun getMostPlayedSongs(limit: Int = 20): List<SongPlaySummary>

    /**
     * Same query as [getMostPlayedSongs] but returns a [Flow] so the UI
     * automatically reacts whenever the `song_interaction` table changes.
     * Room invalidates the Flow whenever any write touches the queried table.
     */
    @Query(
        """
        SELECT songId, title, artist, SUM(playDurationMs) AS totalMs
        FROM song_interaction
        WHERE eventType = 'PLAY'
        GROUP BY songId
        ORDER BY totalMs DESC
        LIMIT :limit
        """
    )
    fun getMostPlayedSongsFlow(limit: Int = 20): Flow<List<SongPlaySummary>>

    /**
     * Returns all songs that have at least one LIKE event and no subsequent
     * DISLIKE event (i.e., the most-recent interaction of type LIKE or DISLIKE
     * for a given song is LIKE).
     */
    @Query(
        """
        SELECT DISTINCT songId, title, artist
        FROM song_interaction s1
        WHERE eventType = 'LIKE'
          AND NOT EXISTS (
              SELECT 1 FROM song_interaction s2
              WHERE s2.songId = s1.songId
                AND s2.eventType = 'DISLIKE'
                AND s2.timestamp > s1.timestamp
          )
        """
    )
    suspend fun getLikedSongs(): List<SongBasicInfo>

    /**
     * Deletes interactions older than [days] days.
     * Useful for pruning the table and keeping storage bounded.
     *
     * @param days number of days to retain (default 90).
     */
    @Query(
        """
        DELETE FROM song_interaction
        WHERE timestamp < :cutoffMs
        """
    )
    suspend fun deleteOldInteractions(cutoffMs: Long): Int

    /** Convenience wrapper that computes the cutoff from [days]. */
    suspend fun deleteOldInteractions(days: Int = 90): Int {
        val cutoffMs = System.currentTimeMillis() - days.toLong() * 24 * 60 * 60 * 1000
        return deleteOldInteractions(cutoffMs)
    }

    /**
     * Returns the most-recent LIKE or DISLIKE event type for [songId], or null
     * if the song has never been rated. Used by the UI to decide which thumb
     * button to highlight.
     */
    @Query(
        """
        SELECT eventType
        FROM song_interaction
        WHERE songId = :songId
          AND eventType IN ('LIKE', 'DISLIKE')
        ORDER BY timestamp DESC
        LIMIT 1
        """
    )
    suspend fun getLastFeedbackForSong(songId: String): String?

    /**
     * Returns all song IDs that have a DISLIKE as their most-recent feedback
     * event. Used by [HybridRecommendationEngine] to apply weight decay.
     */
    @Query(
        """
        SELECT DISTINCT songId
        FROM song_interaction s1
        WHERE eventType = 'DISLIKE'
          AND NOT EXISTS (
              SELECT 1 FROM song_interaction s2
              WHERE s2.songId = s1.songId
                AND s2.eventType = 'LIKE'
                AND s2.timestamp > s1.timestamp
          )
        """
    )
    suspend fun getDislikedSongIds(): List<String>

    /** Wipes the entire interaction history (used by the settings reset action). */
    @Query("DELETE FROM song_interaction")
    suspend fun deleteAllInteractions(): Int
}

/** Lightweight projection returned by [InteractionDao.getMostPlayedSongs]. */
data class SongPlaySummary(
    val songId: String,
    val title: String,
    val artist: String,
    val totalMs: Long,
)

/** Lightweight projection returned by [InteractionDao.getLikedSongs]. */
data class SongBasicInfo(
    val songId: String,
    val title: String,
    val artist: String,
)
