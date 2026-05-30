package com.zionhuang.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zionhuang.music.lyrics.LyricLine
import com.zionhuang.music.lyrics.SyncedLyrics

/**
 * Full-screen lyrics display with synchronized highlighting and smooth transitions.
 */
@Composable
fun FullScreenLyrics(
    lyrics: SyncedLyrics?,
    currentTimeMs: Long = 0L,
    modifier: Modifier = Modifier,
) {
    if (lyrics == null || lyrics.lines.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🎵",
                    fontSize = 48.sp
                )
                Text(
                    text = "Lyrics not available",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W500,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = "from ${lyrics?.source ?: "unknown"} source",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
        return
    }

    val listState = rememberLazyListState()

    // Find current line index
    val currentLineIndex = remember(currentTimeMs) {
        lyrics.lines.indexOfLast { it.timeMs <= currentTimeMs }
            .coerceAtLeast(0)
    }

    // Auto-scroll to current line
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex > 0) {
            listState.animateScrollToItem(
                index = (currentLineIndex - 1).coerceAtLeast(0)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            state = listState,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top padding
            item { Box(modifier = Modifier.height(120.dp)) }

            items(
                items = lyrics.lines,
                key = { "${it.timeMs}-${it.text}" }
            ) { line ->
                val isCurrentLine = lyrics.lines.indexOf(line) == currentLineIndex
                val isPastLine = lyrics.lines.indexOf(line) < currentLineIndex

                LyricLineDisplay(
                    line = line,
                    isCurrent = isCurrentLine,
                    isPast = isPastLine,
                    progress = if (isCurrentLine) {
                        val nextLine = lyrics.lines.getOrNull(currentLineIndex + 1)
                        if (nextLine != null) {
                            val duration = nextLine.timeMs - line.timeMs
                            if (duration > 0) {
                                ((currentTimeMs - line.timeMs).toFloat() / duration).coerceIn(0f, 1f)
                            } else 0f
                        } else 0f
                    } else 0f
                )
            }

            // Bottom padding
            item { Box(modifier = Modifier.height(120.dp)) }
        }
    }
}

/**
 * Single lyric line display with animation
 */
@Composable
private fun LyricLineDisplay(
    line: LyricLine,
    isCurrent: Boolean,
    isPast: Boolean,
    progress: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val fontSize = if (isCurrent) 24.sp else 16.sp
    val fontSizeAnimated by animateFloatAsState(
        targetValue = fontSize.value,
        animationSpec = tween(300),
        label = "lyric_font_size"
    )

    val fontWeight = when {
        isCurrent -> FontWeight.Bold
        isPast -> FontWeight.W600
        else -> FontWeight.Normal
    }

    val alpha = when {
        isCurrent -> 1f
        isPast -> 0.7f
        else -> 0.4f
    }

    val alphaAnimated by animateFloatAsState(
        targetValue = alpha,
        animationSpec = tween(300),
        label = "lyric_alpha"
    )

    val color = when {
        isCurrent -> MaterialTheme.colorScheme.primary
        isPast -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Progress line under current lyric
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }

            Text(
                text = line.text,
                fontSize = fontSizeAnimated.sp,
                fontWeight = fontWeight,
                color = color.copy(alpha = alphaAnimated),
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            // Glow effect for current line
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(2.dp)
                        )
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * Compact lyrics display for inline player
 * Shows current and next line with scrolling effect
 */
@Composable
fun CompactLyricsDisplay(
    lyrics: SyncedLyrics?,
    currentTimeMs: Long = 0L,
    modifier: Modifier = Modifier,
) {
    if (lyrics == null || lyrics.lines.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No lyrics available",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        return
    }

    val currentLineIndex = remember(currentTimeMs) {
        lyrics.lines.indexOfLast { it.timeMs <= currentTimeMs }
            .coerceAtLeast(0)
    }

    val currentLine = lyrics.lines.getOrNull(currentLineIndex)
    val nextLine = lyrics.lines.getOrNull(currentLineIndex + 1)

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Current line - larger, prominent
            AnimatedVisibility(
                visible = currentLine != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (currentLine != null) {
                    Text(
                        text = currentLine.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            // Next line - smaller, dimmed
            AnimatedVisibility(
                visible = nextLine != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (nextLine != null) {
                    Text(
                        text = nextLine.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W400,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Lyrics source indicator badge
 */
@Composable
fun LyricsSourceBadge(
    source: String?,
    modifier: Modifier = Modifier,
) {
    if (source == null) return

    val sourceLabel = when (source) {
        "youtube" -> "YouTube Music"
        "genius" -> "Genius"
        "lrclib" -> "Synchronized"
        else -> "Lyrics"
    }

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = sourceLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.W600,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
