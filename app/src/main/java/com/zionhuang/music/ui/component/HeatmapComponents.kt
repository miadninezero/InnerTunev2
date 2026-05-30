package com.zionhuang.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * Heatmap data models
 */
data class HeatmapData(
    val dayOfWeek: Int, // 0 = Monday, 6 = Sunday
    val hourOfDay: Int, // 0-23
    val playCount: Int,
    val totalListeningTime: Long, // in milliseconds
)

data class ListeningHeatmap(
    val entries: List<HeatmapData>,
    val maxPlayCount: Int,
    val averagePlayCount: Int,
)

/**
 * Listening heatmap visualization - shows listening patterns by day/time
 * Hot colors = high activity, cool colors = low activity
 */
@Composable
fun ListeningHeatmap(
    heatmap: ListeningHeatmap,
    modifier: Modifier = Modifier,
) {
    if (heatmap.entries.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No listening data",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "Your Listening Heatmap",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Heatmap grid
        HeatmapGrid(
            heatmap = heatmap,
            modifier = Modifier.padding(16.dp)
        )

        // Legend
        HeatmapLegend(
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Stats
        HeatmapStats(
            heatmap = heatmap,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Main heatmap grid
 */
@Composable
private fun HeatmapGrid(
    heatmap: ListeningHeatmap,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Time labels (hours)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Empty corner for day labels
            Box(modifier = Modifier.width(40.dp))

            // Hour labels
            for (hour in 0..23) {
                Text(
                    text = if (hour % 3 == 0) "$hour" else "",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.W500,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier
                        .width(14.dp)
                        .padding(2.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Day rows
        for (day in 0..6) {
            HeatmapDayRow(
                day = day,
                heatmap = heatmap,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Single day row in heatmap
 */
@Composable
private fun HeatmapDayRow(
    day: Int,
    heatmap: ListeningHeatmap,
    modifier: Modifier = Modifier,
) {
    val dayName = when (day) {
        0 -> "Mon"
        1 -> "Tue"
        2 -> "Wed"
        3 -> "Thu"
        4 -> "Fri"
        5 -> "Sat"
        6 -> "Sun"
        else -> "???"
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day label
        Text(
            text = dayName,
            fontSize = 10.sp,
            fontWeight = FontWeight.W600,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(40.dp)
        )

        // Hour cells
        for (hour in 0..23) {
            HeatmapCell(
                playCount = heatmap.entries
                    .find { it.dayOfWeek == day && it.hourOfDay == hour }
                    ?.playCount ?: 0,
                maxPlayCount = heatmap.maxPlayCount,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Single heatmap cell with intensity-based coloring
 */
@Composable
private fun HeatmapCell(
    playCount: Int,
    maxPlayCount: Int,
    modifier: Modifier = Modifier,
) {
    val intensity = if (maxPlayCount > 0)
        min(playCount.toFloat() / maxPlayCount, 1f)
    else
        0f

    val color = getHeatmapColor(intensity)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        // Optional: Show count on hover (not implemented here)
    }
}

/**
 * Get color for intensity (cool blue → hot red)
 */
private fun getHeatmapColor(intensity: Float): Color {
    return when {
        intensity < 0.2f -> Color(0xFFEBEEF1) // Very cold (white)
        intensity < 0.4f -> Color(0xFFC1E8F5) // Cold (light blue)
        intensity < 0.6f -> Color(0xFF4DB8F3) // Cool (blue)
        intensity < 0.8f -> Color(0xFFFF9F43) // Warm (orange)
        else -> Color(0xFFE55039) // Hot (red)
    }
}

/**
 * Color legend for heatmap intensities
 */
@Composable
private fun HeatmapLegend(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Less",
            fontSize = 10.sp,
            fontWeight = FontWeight.W500,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        // Color gradient
        listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f).forEach { intensity ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(getHeatmapColor(intensity))
            )
        }

        Text(
            text = "More",
            fontSize = 10.sp,
            fontWeight = FontWeight.W500,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

/**
 * Summary statistics for heatmap
 */
@Composable
private fun HeatmapStats(
    heatmap: ListeningHeatmap,
    modifier: Modifier = Modifier,
) {
    val totalPlays = heatmap.entries.sumOf { it.playCount }
    val totalTime = heatmap.entries.sumOf { it.totalListeningTime }
    val peakHour = heatmap.entries.maxByOrNull { it.playCount }
    val peakDay = heatmap.entries.maxByOrNull { it.playCount }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCard(
            label = "Total Plays",
            value = totalPlays.toString(),
            icon = "🎵"
        )

        StatCard(
            label = "Peak Hour",
            value = peakHour?.let { formatHour(it.hourOfDay) } ?: "N/A",
            icon = "⏰"
        )

        StatCard(
            label = "Listening Time",
            value = formatDuration(totalTime),
            icon = "⏱️"
        )

        StatCard(
            label = "Most Active Day",
            value = peakDay?.let { getDayName(it.dayOfWeek) } ?: "N/A",
            icon = "📅"
        )
    }
}

/**
 * Single stat card
 */
@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: String = "",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            fontSize = 16.sp
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.W500,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

/**
 * Format hour as 12-hour time
 */
private fun formatHour(hour: Int): String {
    return when (hour) {
        0 -> "12 AM"
        12 -> "12 PM"
        in 1..11 -> "$hour AM"
        else -> "${hour - 12} PM"
    }
}

/**
 * Format duration in milliseconds
 */
private fun formatDuration(millis: Long): String {
    val hours = millis / (1000 * 60 * 60)
    val days = hours / 24
    return when {
        days > 0 -> "${days}d"
        hours > 0 -> "${hours}h"
        else -> "<1h"
    }
}

/**
 * Get day name
 */
private fun getDayName(day: Int): String {
    return when (day) {
        0 -> "Monday"
        1 -> "Tuesday"
        2 -> "Wednesday"
        3 -> "Thursday"
        4 -> "Friday"
        5 -> "Saturday"
        6 -> "Sunday"
        else -> "Unknown"
    }
}

/**
 * Weekly breakdown visualization (alternative to heatmap)
 */
@Composable
fun ListeningBreakdown(
    heatmap: ListeningHeatmap,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Weekly Breakdown",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        for (day in 0..6) {
            val dayPlays = heatmap.entries
                .filter { it.dayOfWeek == day }
                .sumOf { it.playCount }

            val percentage = if (heatmap.maxPlayCount > 0)
                (dayPlays.toFloat() / (heatmap.maxPlayCount * 24)) * 100
            else
                0f

            BreakdownBar(
                day = getDayName(day),
                plays = dayPlays,
                percentage = percentage
            )
        }
    }
}

/**
 * Bar for breakdown chart
 */
@Composable
private fun BreakdownBar(
    day: String,
    plays: Int,
    percentage: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = day,
                fontSize = 11.sp,
                fontWeight = FontWeight.W500,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(60.dp)
            )
            Text(
                text = plays.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage / 100f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        color = when {
                            percentage < 30f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            percentage < 60f -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                    )
            )
        }
    }
}
