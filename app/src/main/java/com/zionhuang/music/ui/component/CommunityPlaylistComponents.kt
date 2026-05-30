package com.zionhuang.music.ui.component

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Community playlist data models
 */
data class CommunityPlaylist(
    val id: String,
    val title: String,
    val description: String,
    val curator: CuratorProfile,
    val coverImageUrl: String,
    val songCount: Int,
    val followerCount: Int,
    val isFollowing: Boolean = false,
    val tags: List<String> = emptyList(),
    val updatedAt: Long,
)

data class CuratorProfile(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val followerCount: Int,
    val isVerified: Boolean = false,
)

data class SharedPlaylist(
    val id: String,
    val title: String,
    val curator: String,
    val shareLink: String,
    val shareCode: String, // Short code like "ABC123"
)

/**
 * Community playlists browse screen
 */
@Composable
fun CommunityPlaylistsScreen(
    playlists: List<CommunityPlaylist> = emptyList(),
    onPlaylistClick: (CommunityPlaylist) -> Unit = {},
    onFollowClick: (CommunityPlaylist, Boolean) -> Unit = { _, _ -> },
    onShareClick: (CommunityPlaylist) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Category filter
        CategoryFilterRow(
            selectedCategory = selectedCategory,
            onCategorySelect = { selectedCategory = it },
            modifier = Modifier.fillMaxWidth()
        )

        // Playlists list
        val filteredPlaylists = if (selectedCategory != null)
            playlists.filter { it.tags.contains(selectedCategory) }
        else
            playlists

        if (filteredPlaylists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No playlists found",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(filteredPlaylists) { playlist ->
                    CommunityPlaylistCard(
                        playlist = playlist,
                        onPlaylistClick = { onPlaylistClick(playlist) },
                        onFollowClick = { onFollowClick(playlist, it) },
                        onShareClick = { onShareClick(playlist) }
                    )
                }
            }
        }
    }
}

/**
 * Category filter row
 */
@Composable
private fun CategoryFilterRow(
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val categories = listOf(
        "Chill", "Focus", "Party", "Workout", "Sleep",
        "Road Trip", "Indie", "Hip-Hop", "Jazz", "Electronic"
    )

    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CategoryChip(
                label = "All",
                isSelected = selectedCategory == null,
                onClick = { onCategorySelect(null) }
            )
        }
        items(categories) { category ->
            CategoryChip(
                label = category,
                isSelected = selectedCategory == category,
                onClick = { onCategorySelect(category) }
            )
        }
    }
}

/**
 * Category chip
 */
@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        label = "category_bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected)
            Color.White
        else
            MaterialTheme.colorScheme.primary,
        label = "category_text"
    )

    Box(
        modifier = modifier
            .background(backgroundColor, shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.W600,
            color = textColor
        )
    }
}

/**
 * Community playlist card
 */
@Composable
fun CommunityPlaylistCard(
    playlist: CommunityPlaylist,
    onPlaylistClick: () -> Unit = {},
    onFollowClick: (Boolean) -> Unit = {},
    onShareClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onPlaylistClick),
        cornerRadius = 16
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cover image
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = playlist.coverImageUrl,
                    contentDescription = playlist.title,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            }

            // Playlist info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Title
                Text(
                    text = playlist.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Description
                Text(
                    text = playlist.description,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W400,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Curator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                    ) {
                        AsyncImage(
                            model = playlist.curator.avatarUrl,
                            contentDescription = playlist.curator.name,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Text(
                        text = playlist.curator.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.W500,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    if (playlist.curator.isVerified) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Verified",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBadge(
                        label = "${playlist.songCount}",
                        icon = "🎵"
                    )
                    StatBadge(
                        label = "${playlist.followerCount / 1000}K",
                        icon = "👥"
                    )
                }
            }

            // Action buttons
            Column(
                modifier = Modifier
                    .align(Alignment.Top)
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onFollowClick(!playlist.isFollowing) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (playlist.isFollowing)
                            Icons.Filled.Check
                        else
                            Icons.Filled.PersonAdd,
                        contentDescription = if (playlist.isFollowing) "Following" else "Follow",
                        tint = if (playlist.isFollowing)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.IosShare,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Stat badge for card
 */
@Composable
private fun StatBadge(
    label: String,
    icon: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 8.sp
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.W600,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Playlist sharing dialog / sheet
 */
@Composable
fun PlaylistShareDialog(
    playlist: SharedPlaylist,
    onShare: (platform: String) -> Unit = {},
    onCopyLink: () -> Unit = {},
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Share Playlist",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Playlist preview
            GlassmorphicCard(cornerRadius = 12) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(60.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "by ${playlist.curator}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Share code
            ShareCodeCard(
                code = playlist.shareCode,
                onCopy = onCopyLink
            )

            // Share buttons
            Text(
                text = "Share to",
                fontSize = 12.sp,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShareButton(
                    label = "Message",
                    icon = "💬",
                    onClick = { onShare("message") },
                    modifier = Modifier.weight(1f)
                )
                ShareButton(
                    label = "Instagram",
                    icon = "📸",
                    onClick = { onShare("instagram") },
                    modifier = Modifier.weight(1f)
                )
                ShareButton(
                    label = "Twitter",
                    icon = "𝕏",
                    onClick = { onShare("twitter") },
                    modifier = Modifier.weight(1f)
                )
                ShareButton(
                    label = "Copy",
                    icon = "🔗",
                    onClick = onCopyLink,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Share code card with copy button
 */
@Composable
private fun ShareCodeCard(
    code: String,
    onCopy: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 12
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Share Code",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W600,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = code,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
            }

            androidx.compose.material3.Button(
                onClick = onCopy,
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    text = "Copy",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W600
                )
            }
        }
    }
}

/**
 * Share button for platform
 */
@Composable
private fun ShareButton(
    label: String,
    icon: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.W600,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
