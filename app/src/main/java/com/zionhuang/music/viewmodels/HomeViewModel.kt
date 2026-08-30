package com.zionhuang.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.AlbumItem
import com.zionhuang.innertube.models.ArtistItem
import com.zionhuang.innertube.models.PlaylistItem
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.WatchEndpoint
import com.zionhuang.innertube.models.YTItem
import com.zionhuang.innertube.models.filterExplicit
import com.zionhuang.innertube.pages.ExplorePage
import com.zionhuang.innertube.pages.HomePage
import com.zionhuang.music.R
import com.zionhuang.music.constants.HideExplicitKey
import com.zionhuang.music.db.MusicDatabase
import com.zionhuang.music.db.entities.Album
import com.zionhuang.music.db.entities.Artist
import com.zionhuang.music.db.entities.LocalItem
import com.zionhuang.music.db.entities.Song
import com.zionhuang.music.models.MediaMetadata
import com.zionhuang.music.models.PersistQueue
import com.zionhuang.music.models.SimilarRecommendation
import com.zionhuang.music.models.toMediaMetadata
import com.zionhuang.music.models.toSong
import com.zionhuang.music.models.toSongItem
import com.zionhuang.music.playback.MusicService.Companion.PERSISTENT_QUEUE_FILE
import com.zionhuang.music.utils.dataStore
import com.zionhuang.music.utils.get
import com.zionhuang.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ObjectInputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    private suspend fun load() {
        isLoading.value = true

        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val today = LocalDate.now()

        // 1. Fetch all available local data
        val allSongs = database.allSongs().first()
        val allEvents = database.events().first()
        val likedSongs = database.likedSongsByCreateDateAsc().first()
        val allArtists = database.artistsByCreateDateAsc().first()
        val allAlbums = database.albumsByCreateDateAsc().first()

        val eventsBySongId = allEvents.groupBy { it.event.songId }
        val songScoreMap = mutableMapOf<String, Double>()

        // 2. Score songs based on user behavior (play count, completion rate, recency, likes, skips)
        for (song in allSongs) {
            val songEvents = eventsBySongId[song.id].orEmpty()
            val eventCount = songEvents.size
            val latestEvent = songEvents.maxByOrNull { it.event.timestamp }

            val recencyMultiplier = if (latestEvent != null) {
                val daysAgo = max(0L, ChronoUnit.DAYS.between(latestEvent.event.timestamp.toLocalDate(), today))
                exp(-0.05 * daysAgo.toDouble()) // Decay factor
            } else {
                0.05
            }

            var completeCount = 0
            var skipCount = 0
            for (ev in songEvents) {
                val durationMs = if (song.song.duration > 0) song.song.duration * 1000L else 180000L
                if (ev.event.playTime >= 45000L || ev.event.playTime >= 0.8 * durationMs) {
                    completeCount++
                } else if (ev.event.playTime < 15000L && durationMs > 30000L) {
                    skipCount++
                }
            }

            val playQuality = (completeCount * 1.5) - (skipCount * 1.0) + (eventCount * 1.0)
            val recencyScore = playQuality * recencyMultiplier

            val likedBonus = if (song.song.liked) 3.5 else 0.0
            val libraryBonus = if (song.song.inLibrary != null) 1.5 else 0.0
            val playTimeBonus = ln(1.0 + song.song.totalPlayTime / 60000.0) * 1.2
            val artistBonus = if (song.artists.any { it.bookmarkedAt != null }) 1.5 else 0.0

            val totalScore = recencyScore + likedBonus + libraryBonus + playTimeBonus + artistBonus
            songScoreMap[song.id] = totalScore
        }

        // 3. Continue Listening & Recently Played
        val continueListeningList = mutableListOf<LocalItem>()
        val addedItemIds = mutableSetOf<String>()

        // Check persistent queue for in-progress tracks
        runCatching {
            val queueFile = context.filesDir.resolve(PERSISTENT_QUEUE_FILE)
            if (queueFile.exists()) {
                queueFile.inputStream().use { fis ->
                    ObjectInputStream(fis).use { ois ->
                        val queue = ois.readObject() as? PersistQueue
                        if (queue != null && queue.items.isNotEmpty()) {
                            val curIdx = queue.mediaItemIndex.coerceIn(0, queue.items.lastIndex)
                            val candidates = listOfNotNull(
                                queue.items.getOrNull(curIdx),
                                queue.items.getOrNull(curIdx + 1),
                                queue.items.getOrNull(curIdx - 1)
                            )
                            for (meta in candidates) {
                                if (addedItemIds.add(meta.id)) {
                                    val matchedSong = allSongs.find { it.id == meta.id } ?: meta.toSong()
                                    continueListeningList.add(matchedSong)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add recently played distinct songs
        val recentEvents = allEvents.sortedByDescending { it.event.timestamp }
        val recentSongs = recentEvents.map { it.song }.distinctBy { it.id }
        for (song in recentSongs) {
            if (addedItemIds.add(song.id)) {
                continueListeningList.add(song)
            }
            if (continueListeningList.size >= 10) break
        }

        // Add recently played albums
        val recentAlbums = recentSongs.mapNotNull { it.album }
            .distinctBy { it.id }
            .take(5)
            .mapNotNull { albumEntity -> allAlbums.find { it.id == albumEntity.id } }
        for (album in recentAlbums) {
            if (addedItemIds.add(album.id)) {
                continueListeningList.add(album)
            }
        }

        // Add recently played artists
        val recentArtists = recentSongs.flatMap { it.artists }
            .distinctBy { it.id }
            .filter { it.isYouTubeArtist && it.thumbnailUrl != null }
            .take(5)
            .mapNotNull { artistEntity -> allArtists.find { it.id == artistEntity.id } }
        for (artist in recentArtists) {
            if (addedItemIds.add(artist.id)) {
                continueListeningList.add(artist)
            }
        }

        keepListening.value = continueListeningList.ifEmpty { null }

        // 4. Quick Picks: Scored songs prioritizing user affinity with artist diversity
        val rankedSongs = allSongs.sortedByDescending { songScoreMap[it.id] ?: 0.0 }
        val quickPickSongs = mutableListOf<Song>()
        val artistFrequency = mutableMapOf<String, Int>()

        for (song in rankedSongs) {
            val primaryArtistId = song.artists.firstOrNull()?.id ?: ""
            val count = artistFrequency.getOrDefault(primaryArtistId, 0)
            if (count < 3) {
                quickPickSongs.add(song)
                artistFrequency[primaryArtistId] = count + 1
            }
            if (quickPickSongs.size >= 20) break
        }

        quickPicks.value = quickPickSongs.ifEmpty { null }

        // 5. Rediscover (Forgotten Favorites)
        val rediscoverCandidates = allSongs.filter { song ->
            val songEvents = eventsBySongId[song.id].orEmpty()
            val hasPastAffinity = song.song.liked || song.song.totalPlayTime >= 60000L || songEvents.size >= 2
            val latestEvent = songEvents.maxByOrNull { it.event.timestamp }
            val notPlayedRecently = if (latestEvent != null) {
                ChronoUnit.DAYS.between(latestEvent.event.timestamp.toLocalDate(), today) >= 14
            } else {
                song.song.liked || song.song.inLibrary != null
            }
            hasPastAffinity && notPlayedRecently
        }.sortedByDescending { song ->
            (if (song.song.liked) 500000L else 0L) + song.song.totalPlayTime
        }

        val rediscoverList = if (rediscoverCandidates.isNotEmpty()) {
            rediscoverCandidates.take(20)
        } else {
            allSongs.filter { it.song.inLibrary != null }
                .sortedBy { it.song.inLibrary }
                .take(12)
        }

        forgottenFavorites.value = rediscoverList.ifEmpty { null }

        allLocalItems.value = (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
            .filter { it is Song || it is Album }

        // 6. User's Playlists
        if (YouTube.cookie != null) {
            YouTube.likedPlaylists().onSuccess {
                accountPlaylists.value = it.ifEmpty { null }
            }.onFailure {
                reportException(it)
            }
        }

        // 7. Similar to Your Taste (Personalized recommendations based on top artists & songs)
        val topArtists = allArtists
            .filter { it.artist.isYouTubeArtist }
            .sortedByDescending { artist ->
                allSongs.filter { s -> s.artists.any { it.id == artist.id } }
                    .sumOf { songScoreMap[it.id] ?: 0.0 }
            }
            .take(4)

        val artistRecommendations = topArtists.mapNotNull { artist ->
            val items = mutableListOf<YTItem>()
            YouTube.artist(artist.id).onSuccess { page ->
                items += page.sections.getOrNull(page.sections.size - 2)?.items.orEmpty()
                items += page.sections.lastOrNull()?.items.orEmpty()
            }
            SimilarRecommendation(
                title = artist,
                items = items
                    .filterExplicit(hideExplicit)
                    .distinctBy { it.id }
                    .take(12)
                    .ifEmpty { return@mapNotNull null }
            )
        }

        val topSongs = rankedSongs
            .filter { it.album != null }
            .take(3)

        val songRecommendations = topSongs.mapNotNull { song ->
            val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint ?: return@mapNotNull null
            val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
            SimilarRecommendation(
                title = song,
                items = (page.songs + page.albums + page.artists + page.playlists)
                    .filterExplicit(hideExplicit)
                    .distinctBy { it.id }
                    .take(12)
                    .ifEmpty { return@mapNotNull null }
            )
        }

        similarRecommendations.value = (artistRecommendations + songRecommendations).ifEmpty { null }

        // 8. Dynamic Sections: Liked Songs, Trending / Popular, Charts, and Home Sections
        val dynamicSections = mutableListOf<HomePage.Section>()

        // Liked Songs Section
        if (likedSongs.isNotEmpty()) {
            dynamicSections.add(
                HomePage.Section(
                    title = context.getString(R.string.liked_songs),
                    label = null,
                    thumbnail = likedSongs.firstOrNull()?.song?.thumbnailUrl,
                    endpoint = null,
                    items = likedSongs.take(15).map { it.toSongItem() }
                )
            )
        }

        // Fetch YouTube Charts / Trending when available
        YouTube.browse("FEmusic_charts", null).onSuccess { chartsResult ->
            for (chartItem in chartsResult.items) {
                if (chartItem.items.isNotEmpty()) {
                    dynamicSections.add(
                        HomePage.Section(
                            title = chartItem.title ?: context.getString(R.string.stats),
                            label = null,
                            thumbnail = null,
                            endpoint = null,
                            items = chartItem.items.filterExplicit(hideExplicit).take(15)
                        )
                    )
                }
            }
        }

        // Fetch YouTube Home Sections
        YouTube.home().onSuccess { page ->
            val filteredPage = page.filterExplicit(hideExplicit)
            dynamicSections.addAll(filteredPage.sections)
        }.onFailure {
            reportException(it)
        }

        homePage.value = if (dynamicSections.isNotEmpty()) {
            HomePage(sections = dynamicSections.distinctBy { it.title })
        } else {
            null
        }

        // 9. Explore Page: New releases sorted by artist preference + Mood and Genres
        YouTube.explore().onSuccess { page ->
            val libraryArtistIds = allArtists.map(Artist::id).toHashSet()
            val bookmarkedArtistIds = allArtists
                .filter { it.artist.bookmarkedAt != null }
                .map { it.id }
                .toHashSet()

            val sortedNewReleases = page.newReleaseAlbums
                .sortedBy { album ->
                    if (album.artists.orEmpty().any { it.id in bookmarkedArtistIds }) 0
                    else if (album.artists.orEmpty().any { it.id in libraryArtistIds }) 1
                    else 2
                }
                .filterExplicit(hideExplicit)

            explorePage.value = page.copy(
                newReleaseAlbums = sortedNewReleases
            )

            // Fallback for Quick Picks if local database is empty: populate with new release songs/albums
            if (quickPicks.value == null && sortedNewReleases.isNotEmpty()) {
                val fallbackSongs = sortedNewReleases.take(10).map { album ->
                    Song(
                        song = com.zionhuang.music.db.entities.SongEntity(
                            id = album.id,
                            title = album.title,
                            duration = -1,
                            thumbnailUrl = album.thumbnail,
                            albumId = album.browseId,
                            albumName = album.title
                        ),
                        artists = album.artists.orEmpty().map {
                            com.zionhuang.music.db.entities.ArtistEntity(
                                id = it.id ?: com.zionhuang.music.db.entities.ArtistEntity.generateArtistId(),
                                name = it.name
                            )
                        },
                        album = com.zionhuang.music.db.entities.AlbumEntity(
                            id = album.browseId,
                            title = album.title,
                            songCount = 0,
                            duration = 0
                        )
                    )
                }
                quickPicks.value = fallbackSongs
            }
        }.onFailure {
            reportException(it)
        }

        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                homePage.value?.sections?.flatMap { it.items }.orEmpty() +
                explorePage.value?.newReleaseAlbums.orEmpty()

        isLoading.value = false
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            load()
            isRefreshing.value = false
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            load()
        }
    }
}
