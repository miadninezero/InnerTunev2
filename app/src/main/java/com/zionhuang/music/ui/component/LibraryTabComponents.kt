package com.zionhuang.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Library tab data models
 */
data class PlaylistItem(
    val id: String,
    val name: String,
    val songCount: Int,
    val imageUrl: String,
    val isUserCreated: Boolean = false,
)

data class DownloadItem(
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String,
    val sizeBytes: Long,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
)

/**
 * Library screen - saved music, downloads, playlists
 */
@Composable
fun LibraryScreen(
    likedCount: Int = 0,
    downloadedCount: Int = 0,
    playlists: List<PlaylistItem> = emptyList(),
    downloads: List<DownloadItem> = emptyList(),
    onLikedClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onPlaylistClick: (PlaylistItem) -> Unit = {},
    onDownloadItemClick: (DownloadItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        // Library quick access
        item {
            LibraryQuickAccess(
                likedCount = likedCount,
                downloadedCount = downloadedCount,
                onLikedClick = onLikedClick,
                onDownloadsClick = onDownloadsClick
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Playlists section
        if (playlists.isNotEmpty()) {
            item {
                PlaylistsSection(
                    playlists = playlists,
                    onPlaylistClick = onPlaylistClick
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // Downloads section
        if (downloads.isNotEmpty()) {
            item {
                DownloadsSection(
                    downloads = downloads,
                    onDownloadItemClick = onDownloadItemClick
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

/**
 * Quick access cards (Liked Songs, Downloads, History)
 */
@Composable
private fun LibraryQuickAccess(
    likedCount: Int = 0,
    downloadedCount: Int = 0,
    onLikedClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Your Library",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                QuickAccessCard(
                    icon = "❤️",
                    label = "Liked Songs",
                    count = likedCount,
                    onClick = onLikedClick,
                    backgroundColor = Color(0xFFE91E63).copy(alpha = 0.2f),
                    modifier = Modifier.width(160.dp)
                )
            }

            item {
                QuickAccessCard(
                    icon = "📥",
                    label = "Downloads",
                    count = downloadedCount,
                    onClick = onDownloadsClick,
                    backgroundColor = Color(0xFF2196F3).copy(alpha = 0.2f),
                    modifier = Modifier.width(160.dp)
                )
            }
        }
    }
}

/**
 * Quick access card
 */
@Composable
private fun QuickAccessCard(
    icon: String,
    label: String,
    count: Int,
    onClick: () -> Unit = {},
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    GlassmorphicCard(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        cornerRadius = 12
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$count songs",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W400,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Playlists section
 */
@Composable
private fun PlaylistsSection(
    playlists: List<PlaylistItem>,
    onPlaylistClick: (PlaylistItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PlaylistPlay,
                    contentDescription = "Playlists",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Playlists",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "New",
                fontSize = 12.sp,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { /* Create new playlist */ }
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(playlists) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) }
                )
            }
        }
    }
}

/**
 * Playlist card
 */
@Composable
private fun PlaylistCard(
    playlist: PlaylistItem,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        // Background image
        AsyncImage(
            model = playlist.imageUrl,
            contentDescription = playlist.name,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = playlist.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${playlist.songCount} songs",
                fontSize = 11.sp,
                fontWeight = FontWeight.W400,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Downloads section
 */
@Composable
private fun DownloadsSection(
    downloads: List<DownloadItem>,
    onDownloadItemClick: (DownloadItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = "Downloads",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Downloaded",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "${downloads.size} items",
                fontSize = 12.sp,
                fontWeight = FontWeight.W500,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            items(downloads) { download ->
                DownloadItemRow(
                    item = download,
                    onItemClick = { onDownloadItemClick(download) }
                )
            }
        }
    }
}

/**
 * Download item row with progress and delete
 */
@Composable
private fun DownloadItemRow(
    item: DownloadItem,
    onItemClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
            
            if (item.isDownloading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${(item.downloadProgress * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.artist,
                fontSize = 12.sp,
                fontWeight = FontWeight.W400,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = formatBytes(item.sizeBytes),
                fontSize = 10.sp,
                fontWeight = FontWeight.W400,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        // Delete button
        IconButton(
            onClick = { /* Delete download */ },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteForever,
                contentDescription = "Delete",
                tint = Color(0xFFE53935),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Format bytes to human readable size
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024f * 1024f))
    }
}
