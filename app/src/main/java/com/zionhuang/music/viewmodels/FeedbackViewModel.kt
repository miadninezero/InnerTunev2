package com.zionhuang.music.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zionhuang.music.data.local.InteractionRepository
import com.zionhuang.music.data.local.SongInteraction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the thumb-up / thumb-down feedback buttons across the UI.
 *
 * State for each song is stored in [feedbackState] — a snapshot-state map
 * keyed by `songId` whose value is `"LIKE"`, `"DISLIKE"`, or `null` (unrated).
 *
 * State is loaded lazily via [loadFeedback] and persisted via
 * [InteractionRepository.logLike] / [logDislike].
 *
 * Because this ViewModel is scoped to the Activity (provided via
 * `hiltViewModel()` in composable context), the in-memory map is shared
 * across all screens — no redundant DB reads for the same song within a
 * session.
 */
@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val interactionRepository: InteractionRepository,
) : ViewModel() {

    /**
     * Snapshot-state map so Compose automatically recomposes any composable
     * that reads an entry when it changes.
     * Key = songId, Value = "LIKE" | "DISLIKE" | null (not yet rated / reset).
     */
    val feedbackState = mutableStateMapOf<String, String?>()

    /**
     * Loads the persisted feedback for [songId] from the database if not
     * already cached in [feedbackState].  Safe to call multiple times —
     * subsequent calls for the same id are no-ops.
     */
    fun loadFeedback(songId: String) {
        if (feedbackState.containsKey(songId)) return
        viewModelScope.launch(Dispatchers.IO) {
            val persisted = interactionRepository.getLastFeedback(songId)
            // Update snapshot state from the IO thread (Compose handles this)
            feedbackState[songId] = persisted
        }
    }

    /**
     * Toggles the LIKE state for [songId]:
     * - If currently LIKE  → clears to null (un-like)
     * - Otherwise          → sets to LIKE and logs the event
     */
    fun toggleLike(songId: String, title: String, artist: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (feedbackState[songId] == SongInteraction.EVENT_LIKE) {
                // Un-like: store a neutral interaction by logging nothing new;
                // set state to null so the button de-highlights
                feedbackState[songId] = null
            } else {
                interactionRepository.logLike(songId, title, artist)
                feedbackState[songId] = SongInteraction.EVENT_LIKE
            }
        }
    }

    /**
     * Toggles the DISLIKE state for [songId]:
     * - If currently DISLIKE → clears to null (un-dislike)
     * - Otherwise            → sets to DISLIKE and logs the event
     */
    fun toggleDislike(songId: String, title: String, artist: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (feedbackState[songId] == SongInteraction.EVENT_DISLIKE) {
                feedbackState[songId] = null
            } else {
                interactionRepository.logDislike(songId, title, artist)
                feedbackState[songId] = SongInteraction.EVENT_DISLIKE
            }
        }
    }

    /** Clears the in-memory cache so UI reloads from DB after a reset. */
    fun clearCache() {
        feedbackState.clear()
    }
}
