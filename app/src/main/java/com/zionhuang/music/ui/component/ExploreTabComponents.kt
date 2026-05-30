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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Explore tab data models
 */
data class MoodCategory(
    val id: String,
    val name: String,
    val emoji: String,
    val color: Color,
    val songCount: Int,
)

data class TrendingItem(
    val id: String,
    val title: String,
    val type: String, // "song", "artist", "playlist"
    val imageUrl: String,
    val metadata: String, // "123M plays" or "50K followers"
    val rank: Int,
)

data class SearchResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val type: String, // "song", "artist", "album", "playlist"
)

/**
 * Explore screen - discovery and search
 */
@Composable
fun ExploreScreen(
    moods: List<MoodCategory> = defaultMoodCategories(),
    trending: List<TrendingItem> = emptyList(),
    onMoodClick: (MoodCategory) -> Unit = {},
    onTrendingClick: (TrendingItem) -> Unit = {},
    onSearchChange: (String) -> Unit = {},
    searchResults: List<SearchResult> = emptyList(),
    isSearching: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val isSearchActive = searchQuery.text.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        // Search bar
        SearchBar(
            value = searchQuery,
            onValueChange = { newValue ->
                searchQuery = newValue
                onSearchChange(newValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        if (isSearchActive) {
            // Search results
            SearchResultsList(
                results = searchResults,
                isLoading = isSearching,
                onResultClick = { /* Navigate to item */ }
            )
        } else {
            // Explore content
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                // Mood categories
                item {
                    MoodCategoriesGrid(
                        moods = moods,
                        onMoodClick = onMoodClick
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // Trending
                item {
                    TrendingSection(
                        items = trending,
                        onItemClick = onTrendingClick
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

/**
 * Search bar component
 */
@Composable
fun SearchBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Search songs, artists, playlists...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )
}

/**
 * Mood categories grid
 */
@Composable
private fun MoodCategoriesGrid(
    moods: List<MoodCategory>,
    onMoodClick: (MoodCategory) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Moods & Genres",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(moods) { mood ->
                MoodCategoryCard(
                    mood = mood,
                    onClick = { onMoodClick(mood) }
                )
            }
        }
    }
}

/**
 * Single mood category card
 */
@Composable
private fun MoodCategoryCard(
    mood: MoodCategory,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = mood.color.copy(alpha = 0.2f)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = mood.emoji,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = mood.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${mood.songCount} songs",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Trending section
 */
@Composable
private fun TrendingSection(
    items: List<TrendingItem>,
    onItemClick: (TrendingItem) -> Unit = {},
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
                    imageVector = Icons.Filled.TrendingUp,
                    contentDescription = "Trending",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Trending Now",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "See all",
                fontSize = 12.sp,
                fontWeight = FontWeight.W500,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { /* Navigate to all trending */ }
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items) { item ->
                TrendingCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

/**
 * Trending item card
 */
@Composable
private fun TrendingCard(
    item: TrendingItem,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(160.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        // Background image
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.title,
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
                            Color.Black.copy(alpha = 0.8f)
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
            // Rank badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${item.rank}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.metadata,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Search results list
 */
@Composable
private fun SearchResultsList(
    results: List<SearchResult>,
    isLoading: Boolean = false,
    onResultClick: (SearchResult) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(results) { result ->
            SearchResultRow(
                result = result,
                onClick = { onResultClick(result) }
            )
        }
    }
}

/**
 * Single search result row
 */
@Composable
private fun SearchResultRow(
    result: SearchResult,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp, 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = result.imageUrl,
                contentDescription = result.title,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = result.subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.W400,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = result.type.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Default mood categories for Explore
 */
fun defaultMoodCategories(): List<MoodCategory> = listOf(
    MoodCategory("chill", "Chill", "😌", Color(0xFF6C5CE7), 2543),
    MoodCategory("workout", "Workout", "💪", Color(0xFFFF7675), 3421),
    MoodCategory("focus", "Focus", "🎯", Color(0xFF74B9FF), 1876),
    MoodCategory("party", "Party", "🎉", Color(0xFFFFDA7B), 4123),
    MoodCategory("romantic", "Romantic", "💕", Color(0xFFF368E0), 2891),
    MoodCategory("sleep", "Sleep", "😴", Color(0xFF6C63FF), 1654),
    MoodCategory("jazz", "Jazz", "🎷", Color(0xFFDDA15E), 987),
    MoodCategory("indie", "Indie", "🎸", Color(0xFFBC6C25), 2345),
)
