package com.zionhuang.music.ui.component

import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TimeCapsuleItem(
    val id: String,
    val weekNumber: Int,
    val year: Int,
    val songCount: Int = 12,
    val algorithm: String = "nostalgia", // "nostalgia" or "rediscovery"
    val isViewed: Boolean = false,
    val songs: List<String> = emptyList(), // Song titles
)

/**
 * Time Capsule card with retro cassette tape aesthetic.
 * Displays weekly nostalgia playlists.
 */
@Composable
fun TimeCapsuleCard(
    capsule: TimeCapsuleItem,
    isPlaying: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        cornerRadius = 20
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with week info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Week ${capsule.weekNumber} • ${capsule.year}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W600,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = when (capsule.algorithm) {
                            "rediscovery" -> "🎶 Forgotten Gems"
                            else -> "🎵 Nostalgia Time"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Status badge
                if (!capsule.isViewed) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF4CAF50))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cassette UI - two reels
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        color = Color(0xFF2C3E50).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left reel
                    CassettReel(
                        isSpinning = isPlaying,
                        modifier = Modifier.size(50.dp)
                    )

                    // Tape label in center
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${capsule.songCount} SONGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (capsule.algorithm) {
                                "rediscovery" -> "REDISCOVERY"
                                else -> "NOSTALGIA"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.W600,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Right reel
                    CassettReel(
                        isSpinning = isPlaying,
                        spinDirection = -1f,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Song preview (first 2 songs)
            Column(modifier = Modifier.fillMaxWidth()) {
                capsule.songs.take(2).forEach { songTitle ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = songTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W500,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (capsule.songs.size > 2) {
                    Text(
                        text = "+${capsule.songs.size - 2} more",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.W400,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Animated cassette reel (spinning circles)
 */
@Composable
private fun CassettReel(
    isSpinning: Boolean,
    spinDirection: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isSpinning) 360f * spinDirection else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "reel_rotation"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF34495E))
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        // Reel circle
        repeat(8) { index ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(
                        when (index) {
                            0 -> Alignment.TopCenter
                            1 -> Alignment.TopEnd
                            2 -> Alignment.CenterEnd
                            3 -> Alignment.BottomEnd
                            4 -> Alignment.BottomCenter
                            5 -> Alignment.BottomStart
                            6 -> Alignment.CenterStart
                            else -> Alignment.TopStart
                        }
                    )
                    .offset(x = 16.dp, y = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFBDC3C7))
            )
        }

        // Center hub
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF7F8C8D))
        )
    }
}

/**
 * Time Capsule list - shows recent capsules in chronological order
 */
@Composable
fun TimeCapsuleList(
    capsules: List<TimeCapsuleItem>,
    onCapsuleClick: (TimeCapsuleItem) -> Unit = {},
    onPlayClick: (TimeCapsuleItem) -> Unit = {},
    playingCapsuleId: String? = null,
    modifier: Modifier = Modifier,
) {
    if (capsules.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⏰",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Time Capsules will appear weekly",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W500,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Start playing songs to build your history",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxWidth()) {
            items(capsules) { capsule ->
                TimeCapsuleCard(
                    capsule = capsule,
                    isPlaying = playingCapsuleId == capsule.id,
                    onClick = { onCapsuleClick(capsule) }
                )
            }
        }
    }
}

/**
 * Time Capsule hero banner for home screen quick access
 */
@Composable
fun TimeCapsuleHero(
    latestCapsule: TimeCapsuleItem?,
    isAvailable: Boolean = true,
    onPlayClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        cornerRadius = 20
    ) {
        if (latestCapsule != null) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Background gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                    Color(0xFF2D6A4F).copy(alpha = 0.2f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⏰ Time Capsule",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = when (latestCapsule.algorithm) {
                                "rediscovery" -> "Forgotten Gems: ${latestCapsule.songCount} songs"
                                else -> "Nostalgia Time: ${latestCapsule.songCount} songs"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.W500,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Week ${latestCapsule.weekNumber} • ${latestCapsule.year}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W400,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    // Play button
                    GlassmorphicButton(
                        onClick = onPlayClick,
                        modifier = Modifier.size(56.dp),
                        cornerRadius = 28
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play Time Capsule",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⏰ Your Time Capsule",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "First capsule generates after 1 week of plays",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
