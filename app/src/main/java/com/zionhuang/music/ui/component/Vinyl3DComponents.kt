package com.zionhuang.music.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vinyl record data model
 */
data class VinylRecord(
    val id: String,
    val title: String,
    val artist: String,
    val albumArt: String,
    val year: Int,
    val isNew: Boolean = false,
)

/**
 * 3D Vinyl Shelf with parallax scrolling and tilt effects
 * High-end: Full 3D transforms with perspective
 * Low-end: 2D parallax fallback
 */
@Composable
fun Vinyl3DShelf(
    records: List<VinylRecord> = emptyList(),
    onRecordClick: (VinylRecord) -> Unit = {},
    onRecordPlay: (VinylRecord) -> Unit = {},
    modifier: Modifier = Modifier,
    useHighEndEffects: Boolean = true,
) {
    if (records.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No new releases",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "New Releases",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp)
        )

        if (useHighEndEffects) {
            Vinyl3DShelfHighEnd(
                records = records,
                onRecordClick = onRecordClick,
                onRecordPlay = onRecordPlay
            )
        } else {
            Vinyl3DShelfLowEnd(
                records = records,
                onRecordClick = onRecordClick,
                onRecordPlay = onRecordPlay
            )
        }
    }
}

/**
 * High-end 3D shelf with perspective transforms
 */
@Composable
private fun Vinyl3DShelfHighEnd(
    records: List<VinylRecord>,
    onRecordClick: (VinylRecord) -> Unit = {},
    onRecordPlay: (VinylRecord) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var scrollProgress by remember { mutableStateOf(0f) }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    scrollProgress += dragAmount.x * 0.01f
                }
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        items(records) { record ->
            VinylRecord3D(
                record = record,
                scrollProgress = scrollProgress,
                index = records.indexOf(record),
                totalRecords = records.size,
                onRecordClick = { onRecordClick(record) },
                onRecordPlay = { onRecordPlay(record) }
            )
        }
    }
}

/**
 * Low-end 2D parallax fallback
 */
@Composable
private fun Vinyl3DShelfLowEnd(
    records: List<VinylRecord>,
    onRecordClick: (VinylRecord) -> Unit = {},
    onRecordPlay: (VinylRecord) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        items(records) { record ->
            VinylRecord2D(
                record = record,
                onRecordClick = { onRecordClick(record) },
                onRecordPlay = { onRecordPlay(record) }
            )
        }
    }
}

/**
 * 3D vinyl record with perspective tilt
 */
@Composable
private fun VinylRecord3D(
    record: VinylRecord,
    scrollProgress: Float,
    index: Int,
    totalRecords: Int,
    onRecordClick: () -> Unit = {},
    onRecordPlay: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isHovered by remember { mutableStateOf(false) }

    // Calculate 3D perspective based on scroll position
    val angle = scrollProgress * 45f
    val scale = if (isHovered) 1.1f else 1.0f
    val elevation = if (isHovered) 16.dp else 4.dp
    val rotation = angle * ((index - totalRecords / 2f) / totalRecords)

    val animatedScale by animateFloatAsState(scale, label = "vinyl_scale")
    val animatedElevation by animateDpAsState(elevation, label = "vinyl_elevation")
    val animatedRotation by animateFloatAsState(rotation, label = "vinyl_rotation")

    Box(
        modifier = modifier
            .size(160.dp)
            .shadow(elevation = animatedElevation, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .graphicsLayer(
                rotationY = animatedRotation,
                rotationX = if (isHovered) -10f else 0f,
                transformOrigin = TransformOrigin.Center,
                scaleX = animatedScale,
                scaleY = animatedScale,
                cameraDistance = 8f * 16f // Depth for perspective
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onRecordClick),
        contentAlignment = Alignment.Center
    ) {
        // Album art
        AsyncImage(
            model = record.albumArt,
            contentDescription = record.title,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Crop
        )

        // Vinyl record overlay (circular with grooves)
        VinylRecordOverlay(
            isHovered = isHovered,
            onHoverChange = { isHovered = it }
        )

        // Metadata and play button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = record.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.artist,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.W400,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.material3.IconButton(
                    onClick = onRecordPlay,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // New badge
        if (record.isNew) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(
                        color = Color(0xFFFF6B6B),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "NEW",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 2D vinyl record fallback (low-end devices)
 */
@Composable
private fun VinylRecord2D(
    record: VinylRecord,
    onRecordClick: () -> Unit = {},
    onRecordPlay: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isHovered by remember { mutableStateOf(false) }

    val scale = if (isHovered) 1.05f else 1.0f
    val animatedScale by animateFloatAsState(scale, label = "vinyl_2d_scale")

    Box(
        modifier = modifier
            .size(140.dp)
            .shadow(elevation = if (isHovered) 8.dp else 2.dp, shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale)
            .clickable(onClick = onRecordClick)
    ) {
        // Album art
        AsyncImage(
            model = record.albumArt,
            contentDescription = record.title,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Crop
        )

        // Metadata
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = record.title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = record.artist,
                fontSize = 8.sp,
                fontWeight = FontWeight.W400,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // New badge
        if (record.isNew) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .background(Color(0xFFFF6B6B), shape = RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "NEW",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Play overlay on hover
        if (isHovered) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.IconButton(
                    onClick = onRecordPlay,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Vinyl record spinning effect overlay
 */
@Composable
private fun VinylRecordOverlay(
    isHovered: Boolean,
    onHoverChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isHovered) 360f else 0f,
        label = "vinyl_rotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(rotationZ = rotation * 10f)
            .pointerInput(Unit) {
                detectDragGestures { _, _ ->
                    onHoverChange(!isHovered)
                }
            }
    ) {
        // Vinyl grooves (concentric circles)
        repeat(4) { ring ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(100.dp - (15.dp * ring))
                    .clip(RoundedCornerShape(100))
                    .background(
                        color = Color.Black.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(100)
                    )
            )
        }

        // Center label
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(30.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.3f))
        )
    }
}
