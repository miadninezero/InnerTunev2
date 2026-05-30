package com.zionhuang.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Leaderboard data models
 */
data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val avatarUrl: String,
    val score: Long, // Total play time, plays, or listener count
    val isCurrentUser: Boolean = false,
    val trend: Trend = Trend.STABLE, // UP, DOWN, or STABLE
    val trendPercentage: Float = 0f, // % change from last week
)

data class ArtistCharts(
    val position: Int,
    val artistId: String,
    val artistName: String,
    val imageUrl: String,
    val listeners: Long,
    val playsThisWeek: Long,
    val trend: Trend = Trend.STABLE,
    val genreTag: String = "Pop",
)

data class ChartMetric(
    val label: String,
    val value: Float, // 0.0-1.0
    val displayValue: String,
)

enum class Trend {
    UP, DOWN, STABLE
}

/**
 * Leaderboard screen - User rankings & trending metrics
 */
@Composable
fun LeaderboardScreen(
    userEntries: List<LeaderboardEntry> = emptyList(),
    artistCharts: List<ArtistCharts> = emptyList(),
    currentUserRank: Int? = null,
    onUserClick: (LeaderboardEntry) -> Unit = {},
    onArtistClick: (ArtistCharts) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Tab selector
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Users") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Artists") }
            )
        }

        when (selectedTab) {
            0 -> LeaderboardList(
                entries = userEntries,
                currentUserRank = currentUserRank,
                onEntryClick = onUserClick
            )
            1 -> ArtistChartsSection(
                charts = artistCharts,
                onArtistClick = onArtistClick
            )
        }
    }
}

/**
 * User leaderboard list with ranking badges
 */
@Composable
private fun LeaderboardList(
    entries: List<LeaderboardEntry>,
    currentUserRank: Int? = null,
    onEntryClick: (LeaderboardEntry) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No ranking data available",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(entries) { entry ->
            LeaderboardEntryRow(
                entry = entry,
                isCurrentUser = entry.isCurrentUser || entry.rank == currentUserRank,
                onClick = { onEntryClick(entry) }
            )
        }
    }
}

/**
 * Single leaderboard entry with rank badge and trend indicator
 */
@Composable
private fun LeaderboardEntryRow(
    entry: LeaderboardEntry,
    isCurrentUser: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isCurrentUser)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    val rankColor = when (entry.rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> MaterialTheme.colorScheme.primary
    }

    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        cornerRadius = 16
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(rankColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "#${entry.rank}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = rankColor
                        )
                        if (entry.rank <= 3) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = "Top rank",
                                tint = rankColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // User info
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = entry.avatarUrl,
                        contentDescription = entry.username,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Name and score
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.username,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatScore(entry.score),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W400,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Trend indicator
                TrendBadge(
                    trend = entry.trend,
                    percentage = entry.trendPercentage
                )
            }
        }
    }
}

/**
 * Trend badge showing movement (up/down/stable)
 */
@Composable
private fun TrendBadge(
    trend: Trend,
    percentage: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val (trendColor, trendIcon, trendText) = when (trend) {
        Trend.UP -> Triple(
            Color(0xFF4CAF50),
            Icons.Filled.ArrowUpward,
            "+${percentage.toInt()}%"
        )
        Trend.DOWN -> Triple(
            Color(0xFFF44336),
            Icons.Filled.ArrowDownward,
            "-${percentage.toInt()}%"
        )
        Trend.STABLE -> Triple(
            Color(0xFF9E9E9E),
            Icons.Filled.TrendingUp,
            "−"
        )
    }

    Box(
        modifier = modifier
            .background(
                color = trendColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 4.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = trendIcon,
                contentDescription = trend.name,
                tint = trendColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = trendText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = trendColor
            )
        }
    }
}

/**
 * Artist charts section with genre filtering
 */
@Composable
private fun ArtistChartsSection(
    charts: List<ArtistCharts> = emptyList(),
    onArtistClick: (ArtistCharts) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val genres = charts.map { it.genreTag }.distinct().take(5)
    var selectedGenre by remember { mutableStateOf<String?>(null) }

    val filteredCharts = if (selectedGenre != null)
        charts.filter { it.genreTag == selectedGenre }
    else
        charts

    Column(modifier = modifier.fillMaxWidth()) {
        // Genre filter chips
        if (genres.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    GenreChip(
                        label = "All",
                        isSelected = selectedGenre == null,
                        onClick = { selectedGenre = null }
                    )
                }
                items(genres) { genre ->
                    GenreChip(
                        label = genre,
                        isSelected = selectedGenre == genre,
                        onClick = { selectedGenre = genre }
                    )
                }
            }
        }

        // Artist charts
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(filteredCharts.take(20)) { chart ->
                ArtistChartRow(
                    chart = chart,
                    onClick = { onArtistClick(chart) }
                )
            }
        }
    }
}

/**
 * Genre filter chip
 */
@Composable
private fun GenreChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.W600,
            color = if (isSelected)
                Color.White
            else
                MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Single artist in charts
 */
@Composable
private fun ArtistChartRow(
    chart: ArtistCharts,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        cornerRadius = 12
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${chart.position}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Artist image
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = chart.imageUrl,
                    contentDescription = chart.artistName,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            }

            // Artist info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chart.artistName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${chart.listeners / 1_000_000}M listeners",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W400,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "•",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        text = chart.genreTag,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.W500,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }

            // Trend
            TrendBadge(
                trend = chart.trend,
                percentage = if (chart.trend == Trend.UP) 12f else if (chart.trend == Trend.DOWN) 5f else 0f
            )
        }
    }
}

/**
 * Format score with abbreviations (M for million, K for thousand)
 */
private fun formatScore(score: Long): String {
    return when {
        score >= 1_000_000 -> "${score / 1_000_000}M"
        score >= 1_000 -> "${score / 1_000}K"
        else -> score.toString()
    }
}
