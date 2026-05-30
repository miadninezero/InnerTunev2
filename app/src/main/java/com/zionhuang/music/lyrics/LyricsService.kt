package com.zionhuang.music.lyrics

import com.zionhuang.music.db.entities.LyricsEntity
import kotlinx.serialization.Serializable

/**
 * Lyrics sync models for time-based lyrics display
 */
@Serializable
data class LyricLine(
    val timeMs: Long,           // Time in milliseconds
    val text: String,           // Lyric text
    val startTime: Long = 0L,   // Start time in milliseconds
    val endTime: Long = 0L,     // End time in milliseconds
    val isDuration: Boolean = false,  // Use duration instead of end time
)

@Serializable
data class SyncedLyrics(
    val lines: List<LyricLine> = emptyList(),
    val source: String = "unknown", // "youtube", "genius", "lrclib", "manual"
    // Use epoch millis for serialization compatibility
    val fetchedAt: Long = System.currentTimeMillis(),
    val languageCode: String? = null,
    val isInstrumental: Boolean = false,
)

/**
 * Lyrics service - fetches and manages synchronized lyrics.
 * Priority: YouTube Music API → Genius API → Local LRC → Manual/None
 */
class LyricsService(
    private val youtubeApi: YouTubeLyricsProvider,
    private val geniusApi: GeniusLyricsProvider,
    private val lrcProvider: LrcFilesProvider,
) {
    /**
     * Fetch lyrics with fallback chain.
     * Tries YouTube first, then Genius, then LRC files.
     */
    suspend fun fetchSyncedLyrics(
        songId: String,
        songTitle: String,
        artistName: String,
        albumName: String? = null,
    ): SyncedLyrics? {
        return try {
            // Try YouTube Music API first
            youtubeApi.fetchLyrics(songId, songTitle, artistName)
                ?: run {
                    // Fallback to Genius API
                    geniusApi.fetchLyrics(songTitle, artistName)
                }
                ?: run {
                    // Fallback to local LRC files
                    lrcProvider.fetchLyrics(songId, songTitle, artistName)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get next lyric line based on current playback position.
     * Returns null if at end of lyrics.
     */
    fun getNextLyricLine(lyrics: SyncedLyrics, currentTimeMs: Long): LyricLine? {
        return lyrics.lines.firstOrNull { it.timeMs >= currentTimeMs }
    }

    /**
     * Get current and next lyric lines for display.
     */
    fun getCurrentAndNextLines(
        lyrics: SyncedLyrics,
        currentTimeMs: Long,
    ): Pair<LyricLine?, LyricLine?> {
        val current = lyrics.lines.lastOrNull { it.timeMs <= currentTimeMs }
        val next = lyrics.lines.firstOrNull { it.timeMs > currentTimeMs }
        return current to next
    }

    /**
     * Calculate progress for current line (0.0 to 1.0)
     */
    fun getLineProgress(
        current: LyricLine,
        next: LyricLine?,
        currentTimeMs: Long,
    ): Float {
        if (next == null) return 1f
        val duration = next.timeMs - current.timeMs
        if (duration <= 0) return 0f
        val elapsed = currentTimeMs - current.timeMs
        return (elapsed.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }
}

/**
 * YouTube Music API lyrics provider
 */
interface YouTubeLyricsProvider {
    suspend fun fetchLyrics(
        songId: String,
        songTitle: String,
        artistName: String,
    ): SyncedLyrics?
}

/**
 * Genius API lyrics provider with fallback
 */
interface GeniusLyricsProvider {
    suspend fun fetchLyrics(
        songTitle: String,
        artistName: String,
    ): SyncedLyrics?
}

/**
 * Local LRC files provider
 */
interface LrcFilesProvider {
    suspend fun fetchLyrics(
        songId: String,
        songTitle: String,
        artistName: String,
    ): SyncedLyrics?
}

/**
 * Example YouTube Music API implementation (basic structure)
 */
class YouTubeMusicLyricsProvider : YouTubeLyricsProvider {
    override suspend fun fetchLyrics(
        songId: String,
        songTitle: String,
        artistName: String,
    ): SyncedLyrics? {
        return try {
            // TODO: Call YouTube Music API for songId
            // YouTube API returns synced lyrics if available
            // Returns SyncedLyrics object with source="youtube"
            null // Placeholder
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Example Genius API implementation with parsing
 */
class GeniusApiProvider(private val apiKey: String) : GeniusLyricsProvider {
    override suspend fun fetchLyrics(
        songTitle: String,
        artistName: String,
    ): SyncedLyrics? {
        return try {
            // TODO: Search Genius API for song
            // Fetch lyrics page, parse timestamps if available
            // Returns SyncedLyrics object with source="genius"
            null // Placeholder
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Local LRC file provider
 */
class LocalLrcFilesProvider : LrcFilesProvider {
    override suspend fun fetchLyrics(
        songId: String,
        songTitle: String,
        artistName: String,
    ): SyncedLyrics? {
        return try {
            // TODO: Look for .lrc files in app cache or downloads directory
            // Parse LRC format: [00:12.00]Lyric text
            // Returns SyncedLyrics object with source="lrclib"
            null // Placeholder
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * LRC file format parser
 * Format: [00:12.00]Lyric text
 * Converts to SyncedLyrics
 */
fun parseLrcFormat(lrcText: String): SyncedLyrics {
    val lines = mutableListOf<LyricLine>()

    lrcText.lines().forEach { line ->
        val match = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)").find(line)
        if (match != null) {
            val (minutes, seconds, centiseconds, text) = match.destructured
            val timeMs = (minutes.toInt() * 60 * 1000) +
                    (seconds.toInt() * 1000) +
                    (centiseconds.toInt() * 10)

            lines.add(
                LyricLine(
                    timeMs = timeMs.toLong(),
                    text = text.trim()
                )
            )
        }
    }

    return SyncedLyrics(
        lines = lines.sortedBy { it.timeMs },
        source = "lrclib"
    )
}
