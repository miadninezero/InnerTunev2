package com.zionhuang.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium hero banner for home screen featuring daily pick.
 * Displays animated gradient background synced to playback.
 */
@Composable
fun HeroBanner(
    dailyPickTitle: String,
    dailyPickArtist: String,
    gradientColors: List<Color> = listOf(
        Color(0xFF4285F4),
        Color(0xFF34A853),
        Color(0xFFFBBC04),
        Color(0xFFEA4335)
    ),
    onBannerClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val animatedGradientColor by animateColorAsState(
        targetValue = gradientColors.getOrNull(0) ?: MaterialTheme.colorScheme.primary,
        animationSpec = tween(1000),
        label = "banner_gradient_animation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        animatedGradientColor.copy(alpha = 0.3f),
                        Color(0xFF2c3e50).copy(alpha = 0.4f)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FEATURED TRACK",
                fontSize = 12.sp,
                fontWeight = FontWeight.W300,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = dailyPickTitle,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dailyPickArtist,
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(20.dp))

            GlassmorphicButton(
                onClick = onBannerClick,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(44.dp),
                cornerRadius = 12
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PLAY NOW",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Quick action button grid for rapid access to features.
 * Shows: Shuffle, Mood Picker, Time Capsule, History
 */
@Composable
fun QuickActionsRow(
    onShuffleClick: () -> Unit = {},
    onMoodPickerClick: () -> Unit = {},
    onTimeCapsuleClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Quick Actions",
            fontSize = 12.sp,
            fontWeight = FontWeight.W600,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            letterSpacing = 0.5.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // We'll use a simple grid here - Row with 2 items wide
            Column(modifier = Modifier.fillMaxWidth()) {
                // First row
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        label = "🔀 Shuffle",
                        onClick = onShuffleClick,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        label = "😊 Mood",
                        onClick = onMoodPickerClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Second row
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        label = "⏰ Time Capsule",
                        onClick = onTimeCapsuleClick,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        label = "📜 History",
                        onClick = onHistoryClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassmorphicButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        cornerRadius = 16
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}
