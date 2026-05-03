package com.zionhuang.music.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that wraps [InteractionDao] and exposes a clean, coroutine-based
 * API to the rest of the application.
 *
 * All public functions are suspend functions that switch to [Dispatchers.IO]
 * internally, so callers on the Main dispatcher are safe.
 */
@Singleton
class InteractionRepository @Inject constructor(
    private val dao: InteractionDao,
) {

    /**
     * Logs a PLAY event for [songId].
     *
     * Should only be called after the song has played for at least 10 000 ms
     * (10 seconds). Enforcement is done at the call-site (MusicService) so the
     * repository stays free of business logic.
     *
     * @param songId      YouTube video / song id
     * @param title       Song display title
     * @param artist      Primary artist display name (comma-separated if multiple)
     * @param durationMs  Actual milliseconds the song was played in this session
     */
    suspend fun logPlay(
        songId: String,
        title: String,
        artist: String,
        durationMs: Long,
    ): Unit = withContext(Dispatchers.IO) {
        dao.insert(
            SongInteraction(
                songId = songId,
                title = title,
                artist = artist,
                eventType = SongInteraction.EVENT_PLAY,
                playDurationMs = durationMs,
            )
        )
    }

    /**
     * Logs a SKIP event — called when the user explicitly taps "next" before
     * the song finishes.
     */
    suspend fun logSkip(
        songId: String,
        title: String,
        artist: String,
    ): Unit = withContext(Dispatchers.IO) {
        dao.insert(
            SongInteraction(
                songId = songId,
                title = title,
                artist = artist,
                eventType = SongInteraction.EVENT_SKIP,
            )
        )
    }

    /** Logs a LIKE event (user pressed the like/heart button). */
    suspend fun logLike(
        songId: String,
        title: String,
        artist: String,
    ): Unit = withContext(Dispatchers.IO) {
        dao.insert(
            SongInteraction(
                songId = songId,
                title = title,
                artist = artist,
                eventType = SongInteraction.EVENT_LIKE,
            )
        )
    }

    /** Logs a DISLIKE event. */
    suspend fun logDislike(
        songId: String,
        title: String,
        artist: String,
    ): Unit = withContext(Dispatchers.IO) {
        dao.insert(
            SongInteraction(
                songId = songId,
                title = title,
                artist = artist,
                eventType = SongInteraction.EVENT_DISLIKE,
            )
        )
    }

    /**
     * Returns the [limit] most-played songs ranked by cumulative play
     * duration.
     */
    suspend fun getMostPlayedSongs(limit: Int = 20): List<SongPlaySummary> =
        withContext(Dispatchers.IO) {
            dao.getMostPlayedSongs(limit)
        }

    /**
     * Same as [getMostPlayedSongs] but returns a [Flow] that Room will
     * re-emit whenever the underlying table changes. Collect this in the
     * ViewModel with `stateIn` for reactive UI updates.
     */
    fun getMostPlayedSongsFlow(limit: Int = 20): Flow<List<SongPlaySummary>> =
        dao.getMostPlayedSongsFlow(limit).flowOn(Dispatchers.IO)

    /**
     * Returns songs that have a LIKE event with no subsequent DISLIKE.
     */
    suspend fun getLikedSongs(): List<SongBasicInfo> =
        withContext(Dispatchers.IO) {
            dao.getLikedSongs()
        }

    /**
     * Purges interactions older than [days] days.
     * @return the number of rows deleted.
     */
    suspend fun deleteOldInteractions(days: Int = 90): Int =
        withContext(Dispatchers.IO) {
            dao.deleteOldInteractions(days)
        }

    /**
     * Returns the last explicit feedback event type ("LIKE" or "DISLIKE") for
     * [songId], or null if the user has never rated this song.
     * Used by the thumbs UI to determine which button is highlighted.
     */
    suspend fun getLastFeedback(songId: String): String? =
        withContext(Dispatchers.IO) {
            dao.getLastFeedbackForSong(songId)
        }

    /**
     * Returns the set of song IDs whose latest feedback event is DISLIKE.
     * Used by [HybridRecommendationEngine] to apply weight decay.
     */
    suspend fun getDislikedSongIds(): Set<String> =
        withContext(Dispatchers.IO) {
            dao.getDislikedSongIds().toHashSet()
        }

    /**
     * Deletes every interaction record.
     * Called from the Recommendations settings screen so the user can start fresh.
     */
    suspend fun resetAllInteractions(): Unit =
        withContext(Dispatchers.IO) {
            dao.deleteAllInteractions()
        }
}
