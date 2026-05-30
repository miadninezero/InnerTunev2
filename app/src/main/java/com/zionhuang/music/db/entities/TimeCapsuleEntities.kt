package com.zionhuang.music.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * PlayHistory entity - tracks all song plays for Time Capsule algorithm
 */
@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: String,
    val songTitle: String,
    val artistName: String,
    val artistId: String,
    val albumName: String,
    val albumId: String,
    val albumArtUrl: String,
    val durationMs: Long,
    val playedAt: LocalDateTime,
    val completedMs: Long = 0L, // How far into song before skipped (0 = completed)
)

/**
 * TimeCapsule entity - generated weekly capsules of forgotten/nostalgic songs
 */
@Entity(tableName = "time_capsules")
data class TimeCapsuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val capsuleId: String, // e.g., "capsule_2026_w22"
    val generatedAt: LocalDateTime,
    val weekNumber: Int,
    val year: Int,
    val songIds: String, // JSON array of song IDs
    val algorithm: String, // "nostalgia" or "rediscovery"
    val isViewed: Boolean = false,
    val viewedAt: LocalDateTime? = null,
)

/**
 * Song favorite/engagement tracking for Time Capsule scoring
 */
@Entity(tableName = "song_engagement")
data class SongEngagementEntity(
    @PrimaryKey
    val songId: String,
    val playCount: Int = 0,
    val lastPlayedAt: LocalDateTime? = null,
    val lastPlayedWeeksAgo: Int? = null, // Calculated field
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val totalListenTimeMs: Long = 0L,
    val skipCount: Int = 0,
)
