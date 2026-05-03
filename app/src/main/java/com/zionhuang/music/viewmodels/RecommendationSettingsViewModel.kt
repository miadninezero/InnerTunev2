package com.zionhuang.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zionhuang.music.data.local.InteractionRepository
import com.zionhuang.music.data.recommendation.ActivityRecognitionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel backing [RecommendationSettingsScreen].
 *
 * Exposes:
 * - [resetAllInteractions] — wipes the entire `song_interaction` table so
 *   the recommendation engine starts fresh.
 * - [activityRecognitionManager] — passed through to the composable so it can
 *   call [ActivityRecognitionManager.start] / [ActivityRecognitionManager.stop]
 *   and check [ActivityRecognitionManager.hasPermission] without needing a
 *   separate Hilt injection point.
 */
@HiltViewModel
class RecommendationSettingsViewModel @Inject constructor(
    private val interactionRepository: InteractionRepository,
    val activityRecognitionManager: ActivityRecognitionManager,
) : ViewModel() {

    /** Deletes every interaction record from the local database. */
    suspend fun resetAllInteractions() {
        interactionRepository.resetAllInteractions()
    }
}
