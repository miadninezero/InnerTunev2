package com.zionhuang.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.Canvas
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Mind Map data models for artist relationship visualization
 */
data class ArtistNode(
    val id: String,
    val name: String,
    val imageUrl: String,
    val listenerCount: Int,
    val isCenter: Boolean = false,
)

data class ArtistConnection(
    val fromId: String,
    val toId: String,
    val strength: Float, // 0.0-1.0 based on collaboration/similarity
)

data class MindMapGraph(
    val centerArtist: ArtistNode,
    val relatedArtists: List<ArtistNode>,
    val connections: List<ArtistConnection>,
)

/**
 * Mind Map visualization - displays artist relationships as interactive graph.
 * Center = selected artist, outer ring = related artists, lines = connections.
 */
@Composable
fun MindMapVisualization(
    graph: MindMapGraph,
    onArtistClick: (ArtistNode) -> Unit = {},
    onConnectionClick: (ArtistConnection) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var canvasSize by remember { mutableStateOf(IntSize(0, 0)) }
    var selectedNode by remember { mutableStateOf<ArtistNode?>(null) }
    var zoomLevel by remember { mutableStateOf(1f) }

    val zoomAnimated by animateFloatAsState(
        targetValue = zoomLevel,
        label = "mind_map_zoom"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Panning logic would go here
                }
            }
    ) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(zoomAnimated)
            ) {
                drawMindMapGraph(
                    graph = graph,
                    canvasSize = canvasSize,
                    selectedNode = selectedNode
                )
            }

            // Interactive nodes overlay
            MindMapNodeOverlay(
                graph = graph,
                canvasSize = canvasSize,
                selectedNode = selectedNode,
                onNodeClick = { node ->
                    selectedNode = node
                    onArtistClick(node)
                },
                onNodeDeselect = { selectedNode = null }
            )
        }

        // Zoom controls
        MindMapControls(
            onZoomIn = { zoomLevel = (zoomLevel + 0.2f).coerceAtMost(3f) },
            onZoomOut = { zoomLevel = (zoomLevel - 0.2f).coerceAtLeast(0.5f) },
            onReset = { zoomLevel = 1f; selectedNode = null },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

/**
 * Draw the graph connections and nodes (native drawing)
 */
private fun DrawScope.drawMindMapGraph(
    graph: MindMapGraph,
    canvasSize: IntSize,
    selectedNode: ArtistNode?,
) {
    val centerX = canvasSize.width / 2f
    val centerY = canvasSize.height / 2f
    val radius = minOf(canvasSize.width, canvasSize.height) / 3f

    // Draw connections first (behind nodes)
    val paint = Paint().apply {
        strokeWidth = 2f
        style = PaintingStyle.Stroke
        strokeCap = StrokeCap.Round
    }

    graph.connections.forEach { connection ->
        val fromArtist = graph.relatedArtists.find { it.id == connection.fromId }
        val toArtist = graph.relatedArtists.find { it.id == connection.toId }

        if (fromArtist != null && toArtist != null) {
            val fromAngle = graph.relatedArtists.indexOf(fromArtist) * 2f * Math.PI / graph.relatedArtists.size
            val toAngle = graph.relatedArtists.indexOf(toArtist) * 2f * Math.PI / graph.relatedArtists.size

            val fromX = centerX + (radius * cos(fromAngle)).toFloat()
            val fromY = centerY + (radius * sin(fromAngle)).toFloat()
            val toX = centerX + (radius * cos(toAngle)).toFloat()
            val toY = centerY + (radius * sin(toAngle)).toFloat()

            paint.color = Color(0xFF2196F3).copy(alpha = connection.strength * 0.5f)
            drawLine(
                color = Color(0xFF2196F3).copy(alpha = connection.strength * 0.5f),
                start = androidx.compose.ui.geometry.Offset(fromX, fromY),
                end = androidx.compose.ui.geometry.Offset(toX, toY),
                strokeWidth = 2f * connection.strength
            )
        }
    }

    // Draw center node
    drawCircle(
        color = Color(0xFF2196F3),
        radius = 40f,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )

    // Draw outer nodes
    graph.relatedArtists.forEachIndexed { index, artist ->
        val angle = index * 2f * Math.PI / graph.relatedArtists.size
        val x = centerX + (radius * cos(angle)).toFloat()
        val y = centerY + (radius * sin(angle)).toFloat()

        val isSelected = selectedNode?.id == artist.id
        val nodeColor = if (isSelected)
            Color(0xFF2196F3)
        else
            Color(0xFF2196F3).copy(alpha = 0.6f)

        drawCircle(
            color = nodeColor,
            radius = if (isSelected) 35f else 28f,
            center = androidx.compose.ui.geometry.Offset(x, y)
        )
    }
}

/**
 * Interactive node overlay for clickable nodes
 */
@Composable
private fun MindMapNodeOverlay(
    graph: MindMapGraph,
    canvasSize: IntSize,
    selectedNode: ArtistNode?,
    onNodeClick: (ArtistNode) -> Unit = {},
    onNodeDeselect: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val centerX = canvasSize.width / 2f
    val centerY = canvasSize.height / 2f
    val radius = minOf(canvasSize.width, canvasSize.height) / 3f

    Box(modifier = modifier.fillMaxSize()) {
        // Outer nodes
        graph.relatedArtists.forEachIndexed { index, artist ->
            val angle = index * 2f * Math.PI / graph.relatedArtists.size
            val x = centerX + (radius * cos(angle)).toFloat()
            val y = centerY + (radius * sin(angle)).toFloat()

            val isSelected = selectedNode?.id == artist.id
            val nodeScale = if (isSelected) 1.3f else 1f

            MindMapNodeCard(
                artist = artist,
                isSelected = isSelected,
                scale = nodeScale,
                x = x.dp,
                y = y.dp,
                onClick = {
                    if (isSelected) {
                        onNodeDeselect()
                    } else {
                        onNodeClick(artist)
                    }
                }
            )
        }

        // Center node
        MindMapNodeCard(
            artist = graph.centerArtist,
            isSelected = true,
            scale = 1.5f,
            x = centerX.dp,
            y = centerY.dp,
            onClick = { /* Centered node */ }
        )
    }
}

/**
 * Single node card in the mind map
 */
@Composable
private fun MindMapNodeCard(
    artist: ArtistNode,
    isSelected: Boolean,
    scale: Float,
    x: androidx.compose.ui.unit.Dp,
    y: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val nodeSize = if (artist.isCenter) 80.dp else 56.dp

    Box(
        modifier = modifier
            .size(nodeSize * scale)
            .scale(scale)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = artist.imageUrl,
            contentDescription = artist.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        shape = CircleShape,
                        color = Color.Transparent
                    )
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }

        // Label (shows on hover/select)
        if (isSelected) {
            Text(
                text = artist.name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(4.dp)
            )
        }
    }
}

/**
 * Zoom and pan controls
 */
@Composable
private fun MindMapControls(
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onReset: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
    ) {
        IconButton(onClick = onZoomIn) {
            Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onZoomOut) {
            Text(text = "−", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onReset) {
            Text(text = "⊙", fontSize = 16.sp)
        }
    }
}

/**
 * Fallback grid view (for devices with poor performance)
 */
@Composable
fun MindMapGridFallback(
    graph: MindMapGraph,
    onArtistClick: (ArtistNode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = graph.centerArtist.name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = "Related Artists",
            fontSize = 14.sp,
            fontWeight = FontWeight.W600,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
                items(graph.relatedArtists) { artist: ArtistNode ->
                MindMapGridCard(
                    artist = artist,
                    onClick = { onArtistClick(artist) }
                )
            }
        }
    }
}

/**
 * Grid card for fallback view
 */
@Composable
private fun MindMapGridCard(
    artist: ArtistNode,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
        ) {
            AsyncImage(
                model = artist.imageUrl,
                contentDescription = artist.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Text(
            text = artist.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.W600,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
