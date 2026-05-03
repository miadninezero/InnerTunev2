package com.zionhuang.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zionhuang.music.R
import com.zionhuang.music.viewmodels.FeedbackViewModel

/**
 * Thumb-up / thumb-down feedback buttons.
 *
 * The active state is loaded from [FeedbackViewModel] (backed by the
 * [InteractionRepository]) and automatically persisted across app restarts.
 *
 * @param songId   The YouTube video-id / library song-id to rate.
 * @param title    Song title — passed to the repository for logging.
 * @param artist   Primary artist string — passed to the repository for logging.
 * @param iconSize Size of each button image.
 * @param spacing  Gap between the two buttons.
 * @param modifier Applied to the wrapping [Row].
 */
@Composable
fun SongFeedbackButtons(
    songId: String,
    title: String,
    artist: String,
    iconSize: Dp = 24.dp,
    spacing: Dp = 4.dp,
    modifier: Modifier = Modifier,
    viewModel: FeedbackViewModel = hiltViewModel(),
) {
    // Load persisted state once the songId is known
    LaunchedEffect(songId) {
        viewModel.loadFeedback(songId)
    }

    val feedback = viewModel.feedbackState[songId]
    val scope = rememberCoroutineScope()

    val likeColor by animateColorAsState(
        targetValue = if (feedback == "LIKE") MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "likeColor"
    )
    val dislikeColor by animateColorAsState(
        targetValue = if (feedback == "DISLIKE") MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "dislikeColor"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        ResizableIconButton(
            icon = R.drawable.thumb_up,
            color = likeColor,
            modifier = Modifier
                .size(iconSize)
                .padding(2.dp),
            onClick = {
                viewModel.toggleLike(songId, title, artist)
            }
        )

        Spacer(Modifier.width(spacing))

        ResizableIconButton(
            icon = R.drawable.thumb_down,
            color = dislikeColor,
            modifier = Modifier
                .size(iconSize)
                .padding(2.dp),
            onClick = {
                viewModel.toggleDislike(songId, title, artist)
            }
        )
    }
}
