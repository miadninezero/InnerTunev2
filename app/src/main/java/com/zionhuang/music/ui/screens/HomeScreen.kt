package com.zionhuang.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.zionhuang.innertube.models.AlbumItem
import com.zionhuang.innertube.models.ArtistItem
import com.zionhuang.innertube.models.PlaylistItem
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.WatchEndpoint
import com.zionhuang.innertube.models.YTItem
import com.zionhuang.innertube.utils.parseCookieString
import com.zionhuang.music.LocalDatabase
import com.zionhuang.music.LocalPlayerAwareWindowInsets
import com.zionhuang.music.LocalPlayerConnection
import com.zionhuang.music.R
import com.zionhuang.music.constants.GridThumbnailHeight
import com.zionhuang.music.constants.InnerTubeCookieKey
import com.zionhuang.music.constants.ListItemHeight
import com.zionhuang.music.constants.ListThumbnailSize
import com.zionhuang.music.constants.ThumbnailCornerRadius
import com.zionhuang.music.data.local.SongPlaySummary
import com.zionhuang.music.db.entities.Album
import com.zionhuang.music.db.entities.Artist
import com.zionhuang.music.db.entities.LocalItem
import com.zionhuang.music.db.entities.Playlist
import com.zionhuang.music.db.entities.Song
import com.zionhuang.music.extensions.togglePlayPause
import com.zionhuang.music.models.toMediaMetadata
import com.zionhuang.music.playback.queues.LocalAlbumRadio
import com.zionhuang.music.playback.queues.YouTubeAlbumRadio
import com.zionhuang.music.playback.queues.YouTubeQueue
import com.zionhuang.music.ui.component.AlbumGridItem
import com.zionhuang.music.ui.component.ArtistGridItem
import com.zionhuang.music.ui.component.HideOnScrollFAB
import com.zionhuang.music.ui.component.LocalMenuState
import com.zionhuang.music.ui.component.NavigationTile
import com.zionhuang.music.ui.component.NavigationTitle
import com.zionhuang.music.ui.component.SongGridItem
import com.zionhuang.music.ui.component.SongListItem
import com.zionhuang.music.ui.component.MostPlayedSongCard
import com.zionhuang.music.ui.component.YouTubeGridItem
import com.zionhuang.music.ui.component.shimmer.GridItemPlaceHolder
import com.zionhuang.music.ui.component.shimmer.ShimmerHost
import com.zionhuang.music.ui.component.shimmer.TextPlaceholder
import com.zionhuang.music.ui.menu.AlbumMenu
import com.zionhuang.music.ui.menu.ArtistMenu
import com.zionhuang.music.ui.menu.SongMenu
import com.zionhuang.music.ui.menu.YouTubeAlbumMenu
import com.zionhuang.music.ui.menu.YouTubeArtistMenu
import com.zionhuang.music.ui.menu.YouTubePlaylistMenu
import com.zionhuang.music.ui.menu.YouTubeSongMenu
import com.zionhuang.music.ui.utils.SnapLayoutInfoProvider
import com.zionhuang.music.data.recommendation.HybridFeed
import com.zionhuang.music.data.recommendation.RecommendationUiState
import com.zionhuang.music.utils.rememberPreference
import com.zionhuang.music.viewmodels.ExploreViewModel
import com.zionhuang.music.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random


/**
 * Emits the four hybrid recommendation rows into a [LazyListScope].
 * Called from within the [LazyColumn] in [HomeScreen].
 *
 * Sections rendered (in order):
 *  1. "For You" — personalised songs + new-release albums
 *  2. "Because you liked X" — [HybridFeed.moreLikeThis] (if available)
 *  3. "Hot in Your Area" — regional top chart
 *  4. "Global Top 50" — global top chart
 *
 * @param feed         The [HybridFeed] to render.
 * @param ytGridItem   The shared composable lambda that wraps [YouTubeGridItem] with all
 *                     the play/menu/navigation callbacks already closed over.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.hybridFeedItems(
    feed: HybridFeed,
    @Suppress("UNUSED_PARAMETER") mediaMetadata: com.zionhuang.music.models.MediaMetadata?,
    @Suppress("UNUSED_PARAMETER") isPlaying: Boolean,
    @Suppress("UNUSED_PARAMETER") scope: kotlinx.coroutines.CoroutineScope,
    @Suppress("UNUSED_PARAMETER") playerConnection: com.zionhuang.music.playback.PlayerConnection,
    @Suppress("UNUSED_PARAMETER") navController: androidx.navigation.NavController,
    @Suppress("UNUSED_PARAMETER") menuState: com.zionhuang.music.ui.component.MenuState,
    @Suppress("UNUSED_PARAMETER") haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    ytGridItem: @Composable (com.zionhuang.innertube.models.YTItem) -> Unit,
) {
    // ── "For You" (personal) ─────────────────────────────────────────────────
    if (feed.forYou.isNotEmpty() || feed.forYouAlbums.isNotEmpty()) {
        item(key = "section_for_you_title") {
            NavigationTitle(
                title = stringResource(R.string.section_for_you),
                modifier = Modifier.animateItem()
            )
        }
        item(key = "section_for_you_row") {
            LazyRow(
                contentPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.Horizontal)
                    .asPaddingValues(),
                modifier = Modifier.animateItem()
            ) {
                items(
                    items = feed.forYou,
                    key = { "foryou_song_${it.id}" }
                ) { song ->
                    ytGridItem(song)
                }
                items(
                    items = feed.forYouAlbums,
                    key = { "foryou_album_${it.id}" }
                ) { album ->
                    ytGridItem(album)
                }
            }
        }
    }

    // ── "Because you liked X" (More Like This) ───────────────────────────────
    val moreLikeThis = feed.moreLikeThis
    if (moreLikeThis != null && moreLikeThis.songs.isNotEmpty()) {
        item(key = "section_similar_title") {
            NavigationTitle(
                title = moreLikeThis.sectionLabel,
                modifier = Modifier.animateItem()
            )
        }
        item(key = "section_similar_row") {
            LazyRow(
                contentPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.Horizontal)
                    .asPaddingValues(),
                modifier = Modifier.animateItem()
            ) {
                items(
                    items = moreLikeThis.songs,
                    key = { "similar_${it.id}" }
                ) { song ->
                    ytGridItem(song)
                }
            }
        }
    }

    // ── "Hot in Your Area" (regional) ────────────────────────────────────────
    if (feed.hotInYourArea.isNotEmpty()) {
        item(key = "section_regional_title") {
            NavigationTitle(
                title = stringResource(R.string.section_hot_in_area),
                modifier = Modifier.animateItem()
            )
        }
        item(key = "section_regional_row") {
            LazyRow(
                contentPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.Horizontal)
                    .asPaddingValues(),
                modifier = Modifier.animateItem()
            ) {
                items(
                    items = feed.hotInYourArea,
                    key = { "regional_${it.id}" }
                ) { song ->
                    ytGridItem(song)
                }
            }
        }
    }

    // ── "Global Top 50" ───────────────────────────────────────────────────────
    if (feed.globalTop50.isNotEmpty()) {
        item(key = "section_global_title") {
            NavigationTitle(
                title = stringResource(R.string.section_global_top_50),
                modifier = Modifier.animateItem()
            )
        }
        item(key = "section_global_row") {
            LazyRow(
                contentPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.Horizontal)
                    .asPaddingValues(),
                modifier = Modifier.animateItem()
            ) {
                items(
                    items = feed.globalTop50,
                    key = { "global_${it.id}" }
                ) { song ->
                    ytGridItem(song)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    exploreViewModel: ExploreViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()

    val allLocalItems by viewModel.allLocalItems.collectAsState()
    val allYtItems by viewModel.allYtItems.collectAsState()

    // ── Most Played (from local interaction log) ──────────────────────────
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()

    // ── Hybrid recommendation feed ───────────────────────────────────────
    val exploreUiState by exploreViewModel.uiState.collectAsState()
    val exploreRefreshing by exploreViewModel.isRefreshing.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val localGridItem: @Composable (LocalItem) -> Unit = {
        when (it) {
            is Song -> SongGridItem(
                song = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (it.id == mediaMetadata?.id) {
                                playerConnection.player.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    YouTubeQueue.radio(it.toMediaMetadata()),
                                )
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                SongMenu(
                                    originalSong = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
                isActive = it.id == mediaMetadata?.id,
                isPlaying = isPlaying,
            )

            is Album -> AlbumGridItem(
                album = it,
                isActive = it.id == mediaMetadata?.album?.id,
                isPlaying = isPlaying,
                coroutineScope = scope,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("album/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                AlbumMenu(
                                    originalAlbum = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            )

            is Artist -> ArtistGridItem(
                artist = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("artist/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                ArtistMenu(
                                    originalArtist = it,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
            )

            is Playlist -> {}
        }
    }

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(
            item = item,
            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        when (item) {
                            is SongItem -> playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            when (item) {
                                is SongItem -> YouTubeSongMenu(
                                    song = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is AlbumItem -> YouTubeAlbumMenu(
                                    albumItem = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is ArtistItem -> YouTubeArtistMenu(
                                    artist = item,
                                    onDismiss = menuState::dismiss
                                )

                                is PlaylistItem -> YouTubePlaylistMenu(
                                    playlist = item,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    }
                )
        )
    }

    LaunchedEffect(quickPicks) {
        quickPicksLazyGridState.scrollToItem(0)
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh
            ),
        contentAlignment = Alignment.TopStart
    ) {
        val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
        val quickPicksSnapLayoutInfoProvider = remember(quickPicksLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = quickPicksLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }
        val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = forgottenFavoritesLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }

        LazyColumn(
            state = lazylistState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            item {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .animateItem()
                ) {
                    NavigationTile(
                        title = stringResource(R.string.history),
                        icon = R.drawable.history,
                        onClick = { navController.navigate("history") },
                        modifier = Modifier.weight(1f)
                    )

                    NavigationTile(
                        title = stringResource(R.string.stats),
                        icon = R.drawable.trending_up,
                        onClick = { navController.navigate("stats") },
                        modifier = Modifier.weight(1f)
                    )

                    if (isLoggedIn) {
                        NavigationTile(
                            title = stringResource(R.string.account),
                            icon = R.drawable.person,
                            onClick = {
                                navController.navigate("account")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicks ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.quick_picks),
                        modifier = Modifier.animateItem()
                    )
                }

                item {
                    LazyHorizontalGrid(
                        state = quickPicksLazyGridState,
                        rows = GridCells.Fixed(4),
                        flingBehavior = rememberSnapFlingBehavior(quickPicksSnapLayoutInfoProvider),
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ListItemHeight * 4)
                            .animateItem()
                    ) {
                        items(
                            items = quickPicks,
                            key = { it.id }
                        ) { originalSong ->
                            // fetch song from database to keep updated
                            val song by database.song(originalSong.id).collectAsState(initial = originalSong)

                            SongListItem(
                                song = song!!,
                                showInLibraryIcon = true,
                                isActive = song!!.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                modifier = Modifier
                                    .width(horizontalLazyGridItemWidth)
                                    .combinedClickable(
                                        onClick = {
                                            if (song!!.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(YouTubeQueue.radio(song!!.toMediaMetadata()))
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song!!,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            // ── Most Played section ────────────────────────────────────────────────
            // mostPlayedSongs == null  →  still loading (show nothing)
            // mostPlayedSongs == []   →  new user (show placeholder)
            // mostPlayedSongs != []   →  show the horizontal row
            if (mostPlayedSongs != null) {
                item {
                    NavigationTitle(
                        title = stringResource(R.string.most_played),
                        modifier = Modifier.animateItem()
                    )
                }

                if (mostPlayedSongs!!.isEmpty()) {
                    // New user: no play interactions logged yet
                    item {
                        androidx.compose.foundation.layout.Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .animateItem()
                        ) {
                            androidx.compose.material3.Text(
                                text = stringResource(R.string.most_played_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                } else {
                    item {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(
                                items = mostPlayedSongs!!,
                                key = { it.songId }
                            ) { summary ->
                                // Look up the full Song from MusicDatabase so badges
                                // (liked, library, download) stay live and accurate.
                                val song by database.song(summary.songId)
                                    .collectAsState(initial = null)

                                MostPlayedSongCard(
                                    summary = summary,
                                    song = song,
                                    isActive = (song?.id ?: summary.songId) == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    onClick = {
                                        val fullSong = song
                                        if (fullSong != null) {
                                            if (fullSong.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    YouTubeQueue.radio(fullSong.toMediaMetadata())
                                                )
                                            }
                                        } else {
                                            // Song not in the local library yet —
                                            // build a minimal MediaMetadata and start a radio.
                                            playerConnection.playQueue(
                                                YouTubeQueue.radio(
                                                    com.zionhuang.music.models.MediaMetadata(
                                                        id = summary.songId,
                                                        title = summary.title,
                                                        artists = listOf(
                                                            com.zionhuang.music.models.MediaMetadata.Artist(
                                                                id = null,
                                                                name = summary.artist,
                                                            )
                                                        ),
                                                        duration = -1,
                                                    )
                                                )
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        song?.let { s ->
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = s,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavorites ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.forgotten_favorites),
                        modifier = Modifier.animateItem()
                    )
                }

                item {
                    // take min in case list size is less than 4
                    val rows = min(4, forgottenFavorites.size)
                    LazyHorizontalGrid(
                        state = forgottenFavoritesLazyGridState,
                        rows = GridCells.Fixed(rows),
                        flingBehavior = rememberSnapFlingBehavior(forgottenFavoritesSnapLayoutInfoProvider),
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ListItemHeight * rows)
                            .animateItem()
                    ) {
                        items(
                            items = forgottenFavorites,
                            key = { it.id }
                        ) { originalSong ->
                            val song by database.song(originalSong.id).collectAsState(initial = originalSong)

                            SongListItem(
                                song = song!!,
                                showInLibraryIcon = true,
                                isActive = song!!.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                modifier = Modifier
                                    .width(horizontalLazyGridItemWidth)
                                    .combinedClickable(
                                        onClick = {
                                            if (song!!.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(YouTubeQueue.radio(song!!.toMediaMetadata()))
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song!!,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            keepListening?.takeIf { it.isNotEmpty() }?.let { keepListening ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.keep_listening),
                        modifier = Modifier.animateItem()
                    )
                }

                item {
                    val rows = if (keepListening.size > 6) 2 else 1
                    LazyHorizontalGrid(
                        state = rememberLazyGridState(),
                        rows = GridCells.Fixed(rows),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((GridThumbnailHeight + 24.dp + with(LocalDensity.current) {
                                MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2 +
                                        MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2
                            }) * rows)
                            .animateItem()
                    ) {
                        items(keepListening) {
                            localGridItem(it)
                        }
                    }
                }
            }

            accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylists ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.your_youtube_playlists),
                        onClick = {
                            navController.navigate("account")
                        },
                        modifier = Modifier.animateItem()
                    )
                }

                item {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier.animateItem()
                    ) {
                        items(
                            items = accountPlaylists,
                            key = { it.id },
                        ) { item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            similarRecommendations?.forEach {
                item {
                    NavigationTitle(
                        label = stringResource(R.string.similar_to),
                        title = it.title.title,
                        thumbnail = it.title.thumbnailUrl?.let { thumbnailUrl ->
                            {
                                val shape = if (it.title is Artist) CircleShape else RoundedCornerShape(ThumbnailCornerRadius)
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(ListThumbnailSize)
                                        .clip(shape)
                                )
                            }
                        },
                        onClick = {
                            when (it.title) {
                                is Song -> navController.navigate("album/${it.title.album!!.id}")
                                is Album -> navController.navigate("album/${it.title.id}")
                                is Artist -> navController.navigate("artist/${it.title.id}")
                                is Playlist -> {}
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }

                item {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier.animateItem()
                    ) {
                        items(it.items) { item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            // ── Hybrid recommendation sections ───────────────────────────────
            // exploreUiState drives the three rows: For You, Hot in Your Area,
            // and Global Top 50.  While loading we show shimmers; on error we
            // show a subtle banner but still display any partial data.
            when (val state = exploreUiState) {
                is RecommendationUiState.Loading -> {
                    // Three shimmer rows while all network calls are in flight
                    repeat(3) {
                        item {
                            ShimmerHost(modifier = Modifier.animateItem()) {
                                TextPlaceholder(
                                    height = 36.dp,
                                    modifier = Modifier.padding(12.dp).width(220.dp),
                                )
                                LazyRow {
                                    items(5) { GridItemPlaceHolder() }
                                }
                            }
                        }
                    }
                }

                is RecommendationUiState.Error -> {
                    // Show error banner; still render partialFeed below if available
                    item {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .animateItem()
                        ) {
                            androidx.compose.material3.Text(
                                text = stringResource(R.string.recommendation_error),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    state.partialFeed?.let { feed ->
                        hybridFeedItems(
                            feed            = feed,
                            mediaMetadata   = mediaMetadata,
                            isPlaying       = isPlaying,
                            scope           = scope,
                            playerConnection = playerConnection,
                            navController   = navController,
                            menuState       = menuState,
                            haptic          = haptic,
                            ytGridItem      = ytGridItem,
                        )
                    }
                }

                is RecommendationUiState.Success -> {
                    hybridFeedItems(
                        feed            = state.feed,
                        mediaMetadata   = mediaMetadata,
                        isPlaying       = isPlaying,
                        scope           = scope,
                        playerConnection = playerConnection,
                        navController   = navController,
                        menuState       = menuState,
                        haptic          = haptic,
                        ytGridItem      = ytGridItem,
                    )
                }
            }

            if (isLoading) {
                item {
                    ShimmerHost(modifier = Modifier.animateItem()) {
                        TextPlaceholder(
                            height = 36.dp,
                            modifier = Modifier.padding(12.dp).width(250.dp),
                        )
                        LazyRow { items(4) { GridItemPlaceHolder() } }
                    }
                }
            }
        }

        HideOnScrollFAB(
            visible = allLocalItems.isNotEmpty() || allYtItems.isNotEmpty(),
            lazyListState = lazylistState,
            icon = R.drawable.casino,
            onClick = {
                val local = when {
                    allLocalItems.isNotEmpty() && allYtItems.isNotEmpty() -> Random.nextFloat() < 0.5
                    allLocalItems.isNotEmpty() -> true
                    else -> false
                }
                if (local) {
                    when (val luckyItem = allLocalItems.random()) {
                        is Song -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                        is Album -> {
                            scope.launch(Dispatchers.IO) {
                                database.albumWithSongs(luckyItem.id).first()?.let {
                                    playerConnection.playQueue(
                                        LocalAlbumRadio(it)
                                    )
                                }
                            }
                        }
                        // not possible, already filtered out
                        is Artist -> {}
                        is Playlist -> {}
                    }
                } else {
                    when (val luckyItem = allYtItems.random()) {
                        is SongItem -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                        is AlbumItem -> playerConnection.playQueue(YouTubeAlbumRadio(luckyItem.playlistId))
                        is ArtistItem -> luckyItem.radioEndpoint?.let {
                            playerConnection.playQueue(YouTubeQueue(it))
                        }

                        is PlaylistItem -> luckyItem.playEndpoint?.let {
                            playerConnection.playQueue(YouTubeQueue(it))
                        }
                    }
                }
            }
        )

        Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}
