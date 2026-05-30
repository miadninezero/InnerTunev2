package com.zionhuang.music.playback

import com.zionhuang.music.db.PlayHistoryDao
import com.zionhuang.music.db.SongEngagementDao
import com.zionhuang.music.db.TimeCapsuleDao
import com.zionhuang.music.db.entities.PlayHistoryEntity
import com.zionhuang.music.db.entities.SongEngagementEntity
import com.zionhuang.music.db.entities.TimeCapsuleEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.min

/**
 * Time Capsule service - generates weekly nostalgic/forgotten songs playlists.
 * Algorithm prioritizes:
 * 1. Songs not played in 3+ months (forgotten gems)
 * 2. Songs with high historical play count (nostalgia)
 * 3. Liked songs from past (emotional connection)
 */
class TimeCapsuleService(
    private val playHistoryDao: PlayHistoryDao,
    private val engagementDao: SongEngagementDao,
    private val capsuleDao: TimeCapsuleDao,
) {
    private val json = Json { prettyPrint = true }

    /**
     * Generate a weekly time capsule.
     * Called automatically by WorkManager or manually by user.
     */
    suspend fun generateWeeklyCapsule(): TimeCapsuleEntity? {
        try {
            val now = LocalDateTime.now()
            val weekFields = WeekFields.ISO
            val weekNumber = now.get(weekFields.weekOfYear())
            val year = now.year

            // Check if capsule already exists for this week
            val existing = capsuleDao.getCapsuleForWeek(year, weekNumber)
            if (existing != null) {
                return existing
            }

            // Get candidate songs
            val forgottenSongs = engagementDao.getForgottenSongs(limit = 15)
            val nostalgiaSongs = engagementDao.getSongsForNostalgia(
                minPlays = 5,
                minWeeksAgo = 12,
                limit = 10
            )
            val likedSongs = engagementDao.getLikedSongs(limit = 8)

            // Combine and deduplicate
            val candidates = (forgottenSongs + nostalgiaSongs + likedSongs)
                .distinctBy { it.songId }
                .take(25) // Limit to 25 songs per capsule

            if (candidates.isEmpty()) {
                return null // Not enough data
            }

            // Score and rank candidates
            val scored = candidates.map { engagement ->
                val score = calculateTimeCapsuleScore(engagement, now)
                engagement to score
            }.sortedByDescending { it.second }

            // Select top 12-15 songs
            val selectedSongs = scored.take(min(15, candidates.size)).map { it.first.songId }
            val songIdsJson = json.encodeToString(selectedSongs)

            // Determine algorithm type
            val algorithm = when {
                forgottenSongs.size > nostalgiaSongs.size -> "rediscovery"
                else -> "nostalgia"
            }

            // Create capsule entity
            val capsule = TimeCapsuleEntity(
                capsuleId = "capsule_${year}_w${weekNumber}",
                generatedAt = now,
                weekNumber = weekNumber,
                year = year,
                songIds = songIdsJson,
                algorithm = algorithm,
                isViewed = false
            )

            capsuleDao.insertTimeCapsule(capsule)
            return capsule
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Calculate time capsule score for a song based on multiple factors.
     */
    private fun calculateTimeCapsuleScore(
        engagement: SongEngagementEntity,
        now: LocalDateTime
    ): Float {
        var score = 0f

        // Factor 1: Time since last play (weeks ago)
        val weeksAgo = engagement.lastPlayedWeeksAgo?.toFloat() ?: 0f
        score += when {
            weeksAgo > 26 -> 100f // Not played in 6+ months = highest priority
            weeksAgo > 13 -> 80f  // Not played in 3+ months
            weeksAgo > 8 -> 60f   // Not played in 2 months
            else -> 30f           // Recent but less priority
        }

        // Factor 2: Historical play count (recency + frequency)
        score += (engagement.playCount * 5f).coerceAtMost(60f)

        // Factor 3: Like status (emotional connection)
        if (engagement.isLiked) score += 40f

        // Factor 4: Downloaded (user cares about this song)
        if (engagement.isDownloaded) score += 25f

        // Factor 5: Listen time vs skips (engagement quality)
        if (engagement.skipCount > 0) {
            val skipRatio = engagement.skipCount.toFloat() / engagement.playCount.toFloat()
            score *= (1f - (skipRatio * 0.3f)) // Penalize frequent skips
        }

        // Factor 6: Boost for very forgotten songs (ultra-nostalgia)
        if (weeksAgo > 52) score += 50f

        return score
    }

    /**
     * Mark capsule as viewed and record interaction.
     */
    suspend fun markCapsuleViewed(capsule: TimeCapsuleEntity) {
        val updatedCapsule = capsule.copy(
            isViewed = true,
            viewedAt = LocalDateTime.now()
        )
        capsuleDao.markCapsuleAsViewed(updatedCapsule)
    }

    /**
     * Record a song play for engagement tracking.
     */
    suspend fun recordSongPlay(
        songId: String,
        songTitle: String,
        artistName: String,
        artistId: String,
        albumName: String,
        albumId: String,
        albumArtUrl: String,
        durationMs: Long,
        completedMs: Long = durationMs // 0 if skipped before completion
    ) {
        // Insert play history
        val playHistory = PlayHistoryEntity(
            songId = songId,
            songTitle = songTitle,
            artistName = artistName,
            artistId = artistId,
            albumName = albumName,
            albumId = albumId,
            albumArtUrl = albumArtUrl,
            durationMs = durationMs,
            playedAt = LocalDateTime.now(),
            completedMs = completedMs
        )
        playHistoryDao.insertPlayHistory(playHistory)

        // Update engagement metrics
        val current = engagementDao.getEngagement(songId) ?: SongEngagementEntity(songId)
        val isSkipped = completedMs < (durationMs * 0.8f) // Skipped if <80% played
        val updatedEngagement = current.copy(
            playCount = current.playCount + 1,
            lastPlayedAt = LocalDateTime.now(),
            totalListenTimeMs = current.totalListenTimeMs + completedMs,
            skipCount = if (isSkipped) current.skipCount + 1 else current.skipCount,
            lastPlayedWeeksAgo = 0 // Reset weeks ago
        )
        engagementDao.insertOrUpdateEngagement(updatedEngagement)
    }

    /**
     * Update song like/favorite status.
     */
    suspend fun updateSongLikeStatus(songId: String, liked: Boolean) {
        val current = engagementDao.getEngagement(songId) ?: SongEngagementEntity(songId)
        engagementDao.insertOrUpdateEngagement(
            current.copy(isLiked = liked)
        )
    }

    /**
     * Update song download status.
     */
    suspend fun updateSongDownloadStatus(songId: String, downloaded: Boolean) {
        val current = engagementDao.getEngagement(songId) ?: SongEngagementEntity(songId)
        engagementDao.insertOrUpdateEngagement(
            current.copy(isDownloaded = downloaded)
        )
    }

    /**
     * Clean up old play history (keep last 1 year).
     */
    suspend fun cleanupOldHistory() {
        val oneYearAgo = LocalDateTime.now().minusYears(1)
        playHistoryDao.deleteOlderThan(oneYearAgo)
    }
}
