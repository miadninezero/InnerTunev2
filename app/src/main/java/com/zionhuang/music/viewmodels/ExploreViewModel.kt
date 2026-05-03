package com.zionhuang.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zionhuang.music.data.recommendation.ActivityContext
import com.zionhuang.music.data.recommendation.ActivityRecognitionManager
import com.zionhuang.music.data.recommendation.HybridRecommendationEngine
import com.zionhuang.music.data.recommendation.RecommendationUiState
import com.zionhuang.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel that drives the four-section hybrid recommendation feed:
 * "For You", "Hot in Your Area", "Global Top 50", and "Because you liked X".
 *
 * When the user has enabled **activity-aware recommendations** and granted the
 * `ACTIVITY_RECOGNITION` permission, this ViewModel also observes
 * [ActivityRecognitionManager.activityFlow] and automatically refreshes the
 * "For You" section whenever the user's physical activity changes (e.g. they
 * start running).
 *
 * ## Activity integration
 * - [startActivityAwareMode] begins observing the activity flow.  Call this
 *   after the user grants the permission in the settings screen.
 * - [stopActivityAwareMode] unsubscribes from the flow and performs a final
 *   load with [ActivityContext.Unknown] so the section returns to normal.
 * - The current [activityContext] is exposed as a [StateFlow] for the UI to
 *   display a chip/badge showing what InnerTune thinks the user is doing.
 *
 * All heavy work runs on [Dispatchers.IO] via [HybridRecommendationEngine].
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val engine: HybridRecommendationEngine,
    private val activityRecognitionManager: ActivityRecognitionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecommendationUiState>(RecommendationUiState.Loading)

    /** Observed by the composable to drive all recommendation rows. */
    val uiState: StateFlow<RecommendationUiState> = _uiState.asStateFlow()

    val isRefreshing = MutableStateFlow(false)

    /** The currently detected [ActivityContext]. [ActivityContext.Unknown] when
     *  activity-aware mode is disabled or permission is not granted. */
    val activityContext: StateFlow<ActivityContext> =
        activityRecognitionManager.activityFlow

    private var activityObserverJob: Job? = null

    init {
        load()
    }

    // ── Activity-aware mode ───────────────────────────────────────────────────

    /**
     * Begins observing [ActivityRecognitionManager.activityFlow].
     * Each time the detected activity changes, the feed is automatically
     * reloaded with the new [ActivityContext].
     *
     * This is idempotent — calling it while already observing is a no-op.
     */
    fun startActivityAwareMode() {
        if (activityObserverJob?.isActive == true) return
        activityObserverJob = viewModelScope.launch(Dispatchers.IO) {
            activityRecognitionManager.activityFlow
                .collectLatest { context ->
                    // Reload the feed whenever the activity changes
                    loadInternal(context)
                }
        }
    }

    /**
     * Stops observing the activity flow and reloads the feed without any
     * activity context so the "For You" section returns to normal.
     */
    fun stopActivityAwareMode() {
        activityObserverJob?.cancel()
        activityObserverJob = null
        load()   // reload without activity context
    }

    // ── Standard refresh ──────────────────────────────────────────────────────

    /** Reloads all sections from scratch. Safe to call multiple times. */
    fun refresh() {
        if (isRefreshing.value) return
        load()
    }

    /**
     * Forces a fresh "More Like This" fetch (evicts the cache first), then
     * patches the existing [uiState] with the new result without triggering a
     * full reload.  Useful after the user likes/dislikes a song.
     */
    fun refreshSimilar() {
        viewModelScope.launch(Dispatchers.IO) {
            engine.invalidateSimilarCache()
            val newSimilar = runCatching { engine.getSimilarContent() }.getOrNull()
            val current = (_uiState.value as? RecommendationUiState.Success)?.feed ?: return@launch
            _uiState.value = RecommendationUiState.Success(
                current.copy(moreLikeThis = newSimilar)
            )
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            loadInternal(activityRecognitionManager.activityFlow.value)
        }
    }

    private suspend fun loadInternal(activityContext: ActivityContext) {
        isRefreshing.value = true
        // Keep showing existing data while refreshing (don't flash Loading)
        if (_uiState.value !is RecommendationUiState.Success) {
            _uiState.value = RecommendationUiState.Loading
        }
        try {
            val feed = engine.load(activityContext)
            _uiState.value = RecommendationUiState.Success(feed)
        } catch (e: Exception) {
            reportException(e)
            val existing = (_uiState.value as? RecommendationUiState.Success)?.feed
            _uiState.value = RecommendationUiState.Error(
                message = e.localizedMessage ?: "Unknown error",
                partialFeed = existing,
            )
        } finally {
            isRefreshing.value = false
        }
    }
}
