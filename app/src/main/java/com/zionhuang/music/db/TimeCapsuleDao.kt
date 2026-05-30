package com.zionhuang.music.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zionhuang.music.db.entities.PlayHistoryEntity
import com.zionhuang.music.db.entities.SongEngagementEntity
import com.zionhuang.music.db.entities.TimeCapsuleEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface PlayHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayHistory(playHistory: PlayHistoryEntity)

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentPlays(limit: Int = 50): Flow<List<PlayHistoryEntity>>

    @Query("""
        SELECT * FROM play_history 
        WHERE playedAt BETWEEN :startDate AND :endDate
        ORDER BY playedAt DESC
    """)
    suspend fun getPlaysBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<PlayHistoryEntity>

    @Query("""
        SELECT * FROM play_history 
        WHERE songId = :songId
        ORDER BY playedAt DESC
        LIMIT :limit
    """)
    suspend fun getPlayHistoryForSong(songId: String, limit: Int = 100): List<PlayHistoryEntity>

    @Query("DELETE FROM play_history WHERE playedAt < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: LocalDateTime)

    @Query("SELECT COUNT(*) FROM play_history")
    suspend fun getTotalPlaysCount(): Long

    @Query("DELETE FROM play_history")
    suspend fun clearAll()
}

@Dao
interface TimeCapsuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeCapsule(capsule: TimeCapsuleEntity)

    @Query("SELECT * FROM time_capsules ORDER BY generatedAt DESC LIMIT 1")
    fun getLatestCapsule(): Flow<TimeCapsuleEntity?>

    @Query("SELECT * FROM time_capsules WHERE year = :year AND weekNumber = :week")
    suspend fun getCapsuleForWeek(year: Int, week: Int): TimeCapsuleEntity?

    @Query("SELECT * FROM time_capsules ORDER BY generatedAt DESC LIMIT :limit")
    fun getRecentCapsules(limit: Int = 10): Flow<List<TimeCapsuleEntity>>

    @Update
    suspend fun markCapsuleAsViewed(capsule: TimeCapsuleEntity)

    @Query("SELECT COUNT(*) FROM time_capsules")
    suspend fun getTotalCapsulesCount(): Long

    @Query("""
        SELECT * FROM time_capsules 
        WHERE isViewed = 0 
        ORDER BY generatedAt DESC 
        LIMIT 5
    """)
    fun getUnviewedCapsules(): Flow<List<TimeCapsuleEntity>>
}

@Dao
interface SongEngagementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateEngagement(engagement: SongEngagementEntity)

    @Query("SELECT * FROM song_engagement WHERE songId = :songId")
    suspend fun getEngagement(songId: String): SongEngagementEntity?

    @Query("""
        SELECT * FROM song_engagement 
        WHERE lastPlayedWeeksAgo IS NOT NULL
        ORDER BY lastPlayedWeeksAgo DESC
        LIMIT :limit
    """)
    suspend fun getForgottenSongs(limit: Int = 20): List<SongEngagementEntity>

    @Query("""
        SELECT * FROM song_engagement 
        WHERE playCount > :minPlays 
        AND lastPlayedWeeksAgo > :minWeeksAgo
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    suspend fun getSongsForNostalgia(
        minPlays: Int = 5,
        minWeeksAgo: Int = 12,
        limit: Int = 30
    ): List<SongEngagementEntity>

    @Query("""
        SELECT * FROM song_engagement 
        WHERE isLiked = 1
        ORDER BY lastPlayedAt DESC
        LIMIT :limit
    """)
    suspend fun getLikedSongs(limit: Int = 50): List<SongEngagementEntity>

    @Update
    suspend fun updateEngagement(engagement: SongEngagementEntity)

    @Delete
    suspend fun deleteEngagement(engagement: SongEngagementEntity)

    @Query("DELETE FROM song_engagement")
    suspend fun clearAll()
}
