package com.zionhuang.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zionhuang.music.lyrics.SyncedLyrics

/**
 * Enhanced full player with integrated lyrics display.
 * Allows toggling between player controls and full lyrics view.
 */
@Composable
fun EnhancedFullPlayerScreen(
    state: FullPlayerState,
    onClose: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onSeek: (Float) -> Unit = {},
    onRepeatModeChange: () -> Unit = {},
    onShuffleToggle: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showLyrics by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(true) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Show full lyrics OR regular player
        AnimatedVisibility(
            visible = showLyrics && state.lyrics != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (state.lyrics != null) {
                FullScreenLyrics(
                    lyrics = state.lyrics,
                    currentTimeMs = state.currentTime,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(
            visible = !showLyrics,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FullPlayerScreen(
                state = state.copy(lyrics = null),
                onClose = onClose,
                onPlayPauseClick = onPlayPauseClick,
                onNextClick = onNextClick,
                onPreviousClick = onPreviousClick,
                onLikeClick = onLikeClick,
                onDownloadClick = onDownloadClick,
                onSeek = onSeek,
                onRepeatModeChange = onRepeatModeChange,
                onShuffleToggle = onShuffleToggle,
                onShareClick = onShareClick,
                onMoreClick = onMoreClick,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Floating lyrics toggle button (when lyrics available)
        if (state.lyrics != null && state.lyrics.lines.isNotEmpty()) {
            FloatingLyricsButton(
                isActive = showLyrics,
                source = state.lyrics.source,
                onClick = { showLyrics = !showLyrics },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
        }

        // Close button (always visible in top left)
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

/**
 * Floating lyrics button in bottom left
 */
@Composable
private fun FloatingLyricsButton(
    isActive: Boolean,
    source: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = if (isActive)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "📝",
                fontSize = 20.sp
            )
            Text(
                text = if (isActive) "Lyrics" else "Show",
                fontSize = 10.sp,
                fontWeight = FontWeight.W600,
                color = Color.White
            )
        }
    }
}

/**
 * Full player state without lyrics for backwards compatibility
 */
fun FullPlayerState.copy(
    songTitle: String = this.songTitle,
    artistName: String = this.artistName,
    albumName: String = this.albumName,
    albumArtUrl: String = this.albumArtUrl,
    currentTime: Long = this.currentTime,
    duration: Long = this.duration,
    isPlaying: Boolean = this.isPlaying,
    isLiked: Boolean = this.isLiked,
    isDownloaded: Boolean = this.isDownloaded,
    repeatMode: Int = this.repeatMode,
    isShuffleEnabled: Boolean = this.isShuffleEnabled,
    lyrics: SyncedLyrics? = this.lyrics,
) = FullPlayerState(
    songTitle = songTitle,
    artistName = artistName,
    albumName = albumName,
    albumArtUrl = albumArtUrl,
    currentTime = currentTime,
    duration = duration,
    isPlaying = isPlaying,
    isLiked = isLiked,
    isDownloaded = isDownloaded,
    repeatMode = repeatMode,
    isShuffleEnabled = isShuffleEnabled,
    lyrics = lyrics
)
