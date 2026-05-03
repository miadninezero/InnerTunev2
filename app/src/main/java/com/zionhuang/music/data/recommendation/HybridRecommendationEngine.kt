package com.zionhuang.music.data.recommendation

import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.WatchEndpoint
import com.zionhuang.music.data.local.InteractionRepository
import com.zionhuang.music.db.MusicDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Merges personal listening history, regional charts, and global trending data
 * into distinct recommendation sections shown on the Home screen.
 *
 * All network calls hit the YouTube Music InnerTube API via the existing [YouTube]
 * object.  The interface of [YouTube] is never modified.
 *
 * ## Section weights
 * | Section            | Composition                                                |
 * |--------------------|-----------------------------------------------------------|
 * | For You            | 60 % personal (related to most-played + liked), 40 % new releases |
 * | Hot in Your Area   | Regional top-charts playlist (locale-derived)             |
 * | Global Top 50      | YouTube Music global charts playlist                      |
 * | More Like This     | Deep related from top liked/played + artist radio         |
 *
 * All sections are fetched concurrently via [async]/[awaitAll].
 */
@Singleton
class HybridRecommendationEngine @Inject constructor(
    private val interactionRepository: InteractionRepository,
    private val database: MusicDatabase,
    private val activityAwareRecommender: ActivityAwareRecommender,
) {

    // ── Known YouTube Music chart playlist IDs ────────────────────────────────
    private val GLOBAL_CHARTS_PLAYLIST = "RDCLAK5uy_kmPRjHDECp8_FTe0G5xMiAT_eAmM6PN_s"

    // ── "More Like This" 30-minute cache ─────────────────────────────────────
    private val cacheMutex = Mutex()
    private var cachedSimilar: SimilarContentFeed? = null
    private var cacheTimestamp: Long = 0L
    private val CACHE_TTL_MS = 30 * 60 * 1000L   // 30 minutes

    /**
     * Returns a country-specific charts playlist id.  Unknown countries fall
     * back to the global chart.
     */
    private fun regionalPlaylistId(countryCode: String): String = when (countryCode.uppercase()) {
        "US" -> "RDCLAK5uy_kpnTKMtfnvn8sVPkHN8mfRXELJ2n3yY"
        "GB" -> "RDCLAK5uy_lA-23b7BYNnFGPmz_0Hv7i5L8RZ7LBE"
        "DE" -> "RDCLAK5uy_mUqKFkqfvteBz0jc3bpT2p9kbj0-QKLE"
        "FR" -> "RDCLAK5uy_m-RL-R3OL7N4-fGFhQ0AMx27nKnq1Kg"
        "JP" -> "RDCLAK5uy_klxFa0Bi8OEdGD-j9CmG15vLVFVLfhis"
        "KR" -> "RDCLAK5uy_m2SZRnJPyBp6Sxbq3sMJkB1FiV9RcDrI"
        "BR" -> "RDCLAK5uy_k1Y4f4GRGQ5-lJmPiPl9HXNn5fkrOXA4"
        "IN" -> "RDCLAK5uy_kh9IQaFR2HZ-h-v3i2MNKHkCqFLfGDGs"
        "AU" -> "RDCLAK5uy_lN8xLd0JEa9MBxSMqLlT0VIZtOBHCbCQ"
        "CA" -> "RDCLAK5uy_nSj5_wSGLrn6ZqN3KiujLyAtMPT-XKfPQ"
        else  -> GLOBAL_CHARTS_PLAYLIST
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Loads all sections concurrently and returns a [HybridFeed].
     * [getSimilarContent] is called concurrently and merged into the feed;
     * a failure there is non-fatal (the field will be null).
     *
     * @param activityContext The user's current physical activity. When non-
     *                        [ActivityContext.Unknown], activity-specific songs
     *                        are prepended to the "For You" section.
     */
    suspend fun load(
        activityContext: ActivityContext = ActivityContext.Unknown,
    ): HybridFeed = withContext(Dispatchers.IO) {
        coroutineScope {
            // ── Local data ────────────────────────────────────────────────────
            val mostPlayedDeferred = async { interactionRepository.getMostPlayedSongs(limit = 10) }
            val likedSongsDeferred = async { interactionRepository.getLikedSongs() }
            val dbTopSongsDeferred = async {
                val fromTimestamp = System.currentTimeMillis() - 86_400_000L * 90
                database.mostPlayedSongs(fromTimestamp, limit = 10).first()
            }
            val dislikedDeferred = async { interactionRepository.getDislikedSongIds() }

            val mostPlayed  = mostPlayedDeferred.await()
            val likedSongs  = likedSongsDeferred.await()
            val dbTopSongs  = dbTopSongsDeferred.await()
            val dislikedIds = dislikedDeferred.await()

            // ── Seed list (excludes disliked) ─────────────────────────────────
            val personalSeeds: List<String> = buildList {
                addAll(mostPlayed.map { it.songId }.filter { it !in dislikedIds })
                addAll(likedSongs.map { it.songId }.filter { it !in dislikedIds })
                if (isEmpty()) addAll(dbTopSongs.take(5).map { it.id }.filter { it !in dislikedIds })
            }.distinct().take(6)

            // ── Network calls (fully concurrent) ─────────────────────────────
            val countryCode = YouTube.locale.gl

            // ── Activity-aware injection ──────────────────────────────────────
            val activitySongsDeferred = async {
                runCatching {
                    activityAwareRecommender.getActivitySongs(activityContext, limit = 10)
                }.getOrDefault(emptyList())
            }

            val relatedJobsDeferred = personalSeeds.map { seedId ->
                async {
                    YouTube.next(WatchEndpoint(videoId = seedId))
                        .getOrNull()?.relatedEndpoint
                        ?.let { YouTube.related(it).getOrNull()?.songs }
                        .orEmpty()
                }
            }
            val globalChartsDeferred = async {
                YouTube.queue(playlistId = GLOBAL_CHARTS_PLAYLIST).getOrElse { emptyList() }
            }
            val regionalChartsDeferred = async {
                val pid = regionalPlaylistId(countryCode)
                YouTube.queue(playlistId = pid).getOrElse {
                    if (pid != GLOBAL_CHARTS_PLAYLIST)
                        YouTube.queue(playlistId = GLOBAL_CHARTS_PLAYLIST).getOrElse { emptyList() }
                    else emptyList()
                }
            }
            val newReleasesDeferred = async {
                YouTube.newReleaseAlbums().getOrElse { emptyList() }
            }
            // "More Like This" runs concurrently; failure is silently swallowed
            val moreLikeThisDeferred = async {
                runCatching { getSimilarContent() }.getOrNull()
            }

            // ── Await all ─────────────────────────────────────────────────────
            val activitySongs   = activitySongsDeferred.await()
            val relatedSongs    = relatedJobsDeferred.awaitAll().flatten()
            val globalCharts    = globalChartsDeferred.await()
            val regionalCharts  = regionalChartsDeferred.await()
            val newReleases     = newReleasesDeferred.await()
            val moreLikeThis    = moreLikeThisDeferred.await()

            // ── Merge "For You" ───────────────────────────────────────────────
            val forYouTarget    = 30
            val personalCount   = (forYouTarget * 0.6).toInt()   // 18
            val newReleaseCount = forYouTarget - personalCount    // 12

            // Activity songs are injected first, then back-filled from related.
            // All are de-duplicated together so one song can't appear twice.
            val personalItems  = (activitySongs + relatedSongs)
                .distinctById()
                .filter { it.id !in dislikedIds }
                .take(personalCount)
            val newReleaseSongs = newReleases.take(newReleaseCount)

            // ── Deduplicate regional vs global ────────────────────────────────
            val globalIds     = globalCharts.map { it.id }.toHashSet()
            val uniqueRegional = regionalCharts.filter { it.id !in globalIds }

            // Weight-decay: disliked songs pushed to the back of chart sections
            val regionalSorted = uniqueRegional.sortedBy { if (it.id in dislikedIds) 1 else 0 }
            val globalSorted   = globalCharts.sortedBy   { if (it.id in dislikedIds) 1 else 0 }

            HybridFeed(
                forYou        = personalItems,
                forYouAlbums  = newReleaseSongs,
                hotInYourArea = regionalSorted.take(50).ifEmpty { regionalCharts.take(50) },
                globalTop50   = globalSorted.take(50),
                moreLikeThis  = moreLikeThis,
                countryCode   = countryCode,
            )
        }
    }

    // ── "More Like This" / getSimilarContent ─────────────────────────────────

    /**
     * Fetches up to 20 songs similar to the user's top liked (or most-played)
     * track.  Results are cached for [CACHE_TTL_MS] (30 minutes) to avoid
     * excessive API calls on rapid screen re-entries.
     *
     * ## Strategy (no proxy changes required)
     * For the primary seed song we:
     *  1. Call `YouTube.next(WatchEndpoint(videoId = seedId))` to obtain its
     *     `relatedEndpoint` (a `BrowseEndpoint` returned by the watch API).
     *  2. Call `YouTube.related(relatedEndpoint)` to get directly related songs.
     *  3. Fetch the primary artist's page via `YouTube.artist(artistId)` and
     *     use `artist.radioEndpoint` (the artist-mix playlist endpoint) to load
     *     more songs via `YouTube.next()`.
     *  4. For the next 4 seeds (up to 5 total) we call `YouTube.next()` only,
     *     collecting more related queues and flattening them.
     *
     * If the user has no history at all, returns null.
     *
     * @return A [SimilarContentFeed] whose [SimilarContentFeed.sectionLabel]
     *         reads "Because you liked [artistName]", or null on failure / no seeds.
     */
    suspend fun getSimilarContent(): SimilarContentFeed? = withContext(Dispatchers.IO) {
        // ── Cache hit? ────────────────────────────────────────────────────────
        cacheMutex.withLock {
            val age = System.currentTimeMillis() - cacheTimestamp
            if (cachedSimilar != null && age < CACHE_TTL_MS) {
                return@withContext cachedSimilar
            }
        }

        // ── Determine seeds ───────────────────────────────────────────────────
        val dislikedIds = interactionRepository.getDislikedSongIds()

        // Prefer liked songs, fall back to most-played, then DB top-played
        val likedSeeds = interactionRepository.getLikedSongs()
            .map { it.songId }
            .filter { it !in dislikedIds }

        val interactionSeeds = interactionRepository.getMostPlayedSongs(limit = 10)
            .map { it.songId }
            .filter { it !in dislikedIds }

        val dbSeeds = run {
            val from = System.currentTimeMillis() - 86_400_000L * 90
            database.mostPlayedSongs(from, limit = 10).first().map { it.id }
                .filter { it !in dislikedIds }
        }

        val candidateSeeds: List<String> = buildList {
            addAll(likedSeeds)
            addAll(interactionSeeds)
            addAll(dbSeeds)
        }.distinct().take(5)

        if (candidateSeeds.isEmpty()) return@withContext null

        // The primary seed drives the label and artist-radio call
        val primarySeedId = candidateSeeds.first()

        // ── Fetch primary seed metadata + related ─────────────────────────────
        val primaryNextResult = YouTube.next(WatchEndpoint(videoId = primarySeedId)).getOrNull()

        // Attempt deep related (song-specific related page)
        val primaryRelated: List<SongItem> = primaryNextResult?.relatedEndpoint
            ?.let { YouTube.related(it).getOrNull()?.songs }
            .orEmpty()

        // Resolve seed label from the queue items returned by next()
        val primarySongTitle = primaryNextResult?.items
            ?.firstOrNull { it.id == primarySeedId }
            ?.title ?: "your favourite song"
        val primaryArtistName = primaryNextResult?.items
            ?.firstOrNull { it.id == primarySeedId }
            ?.artists?.firstOrNull()?.name ?: ""
        val primaryArtistId = primaryNextResult?.items
            ?.firstOrNull { it.id == primarySeedId }
            ?.artists?.firstOrNull()?.id

        // ── Artist radio ──────────────────────────────────────────────────────
        // If we have an artist browseId, load the artist page and use its
        // radioEndpoint to start the artist-mix playlist via next().
        val artistRadioSongs: List<SongItem> = if (primaryArtistId != null) {
            runCatching {
                val artistPage = YouTube.artist(primaryArtistId).getOrNull()
                val radioEndpoint = artistPage?.artist?.radioEndpoint
                    ?: artistPage?.artist?.shuffleEndpoint
                if (radioEndpoint != null) {
                    YouTube.next(radioEndpoint).getOrNull()?.items.orEmpty()
                } else emptyList()
            }.getOrDefault(emptyList())
        } else emptyList()

        // ── Additional seeds (seeds 2-5): just next() for queue songs ─────────
        val additionalSeeds = candidateSeeds.drop(1).take(4)
        val additionalSongs: List<SongItem> = coroutineScope {
            additionalSeeds.map { seedId ->
                async {
                    YouTube.next(WatchEndpoint(videoId = seedId))
                        .getOrNull()?.items.orEmpty()
                }
            }.awaitAll().flatten()
        }

        // ── Merge + deduplicate ───────────────────────────────────────────────
        // Priority: primary related > artist radio > additional seeds
        val merged: List<SongItem> = (primaryRelated + artistRadioSongs + additionalSongs)
            .distinctById()
            .filter { it.id !in dislikedIds }
            .filter { it.id != primarySeedId }   // don't recommend the seed itself
            .take(20)

        if (merged.isEmpty()) return@withContext null

        val result = SimilarContentFeed(
            songs          = merged,
            seedSongTitle  = primarySongTitle,
            seedArtistName = primaryArtistName.ifEmpty { primarySongTitle },
        )

        // ── Store in cache ────────────────────────────────────────────────────
        cacheMutex.withLock {
            cachedSimilar    = result
            cacheTimestamp   = System.currentTimeMillis()
        }

        result
    }

    /** Evicts the "More Like This" cache so the next call fetches fresh data. */
    suspend fun invalidateSimilarCache() = cacheMutex.withLock {
        cachedSimilar  = null
        cacheTimestamp = 0L
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun List<SongItem>.distinctById(): List<SongItem> {
        val seen = HashSet<String>()
        return filter { seen.add(it.id) }
    }
}
