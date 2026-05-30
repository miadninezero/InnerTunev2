package com.zionhuang.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

data class MiniPlayerState(
    val songTitle: String,
    val artistName: String,
    val albumArtUrl: String,
    val isPlaying: Boolean,
    val progress: Float = 0f, // 0f to 1f
    val liked: Boolean = false,
    val downloaded: Boolean = false,
)

/**
 * Enhanced mini-player with swipe gestures for download and like actions.
 * Swipe left: download | Swipe right: like
 */
@Composable
fun EnhancedMiniPlayer(
    state: MiniPlayerState,
    onPlayPauseClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onLikeSwipe: () -> Unit = {},
    onDownloadSwipe: () -> Unit = {},
    onPlayerExpand: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var swipeOffset by remember { mutableStateOf(0f) }
    var showLikeIndicator by remember { mutableStateOf(false) }
    var showDownloadIndicator by remember { mutableStateOf(false) }

    val likeIndicatorOpacity = animateDpAsState(
        targetValue = if (showLikeIndicator) 40.dp else 0.dp,
        label = "like_indicator"
    )

    val downloadIndicatorOpacity = animateDpAsState(
        targetValue = if (showDownloadIndicator) 40.dp else 0.dp,
        label = "download_indicator"
    )

    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onPlayerExpand)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        swipeOffset += dragAmount.x

                        // Trigger like on significant left swipe
                        if (swipeOffset > 100f) {
                            if (!showLikeIndicator) {
                                showLikeIndicator = true
                                onLikeSwipe()
                            }
                        }

                        // Trigger download on significant right swipe
                        if (swipeOffset < -100f) {
                            if (!showDownloadIndicator) {
                                showDownloadIndicator = true
                                onDownloadSwipe()
                            }
                        }
                    },
                    onDragEnd = {
                        swipeOffset = 0f
                        showLikeIndicator = false
                        showDownloadIndicator = false
                    }
                )
            },
        cornerRadius = 16
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = state.albumArtUrl,
                    contentDescription = state.songTitle,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = state.songTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.artistName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W400,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(1.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(state.progress)
                            .height(2.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Controls
            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying)
                            Icons.Filled.PauseCircle
                        else
                            Icons.Filled.PlayCircle,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Swipe indicators
            AnimatedVisibility(
                visible = showLikeIndicator,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(likeIndicatorOpacity.value)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFFE91E63).copy(alpha = 0.8f))
                        .align(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "❤️",
                        fontSize = 20.sp
                    )
                }
            }

            AnimatedVisibility(
                visible = showDownloadIndicator,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(downloadIndicatorOpacity.value)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF2196F3).copy(alpha = 0.8f))
                        .align(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Download",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact mini-player (minimal version for space-constrained layouts)
 */
@Composable
fun CompactMiniPlayer(
    state: MiniPlayerState,
    onPlayPauseClick: () -> Unit = {},
    onPlayerExpand: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onPlayerExpand),
        cornerRadius = 12
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = state.songTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.artistName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.W400,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play/Pause button
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (state.isPlaying)
                        Icons.Filled.PauseCircle
                    else
                        Icons.Filled.PlayCircle,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
