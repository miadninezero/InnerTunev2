package com.zionhuang.music.data.recommendation

import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.pages.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enriches the "For You" section with activity-aware songs.
 *
 * When the user's [ActivityContext] is known and non-[ActivityContext.Unknown],
 * [getActivitySongs] executes a YouTube Music search for the context's
 * [ActivityContext.searchQuery], returning up to [limit] [SongItem]s.
 *
 * ## Design
 * - No BPM data is available through the InnerTube proxy, so we use curated
 *   search queries (e.g. "workout running high energy music") as a reliable
 *   fallback.  The queries are defined on [ActivityContext] itself so they can
 *   be independently tweaked.
 * - The results are **injected at the front** of the "For You" list in
 *   [HybridRecommendationEngine], then de-duplicated with the rest of the
 *   personal feed.
 * - Failures (network, API) are silently swallowed: returning an empty list
 *   falls back gracefully to the non-activity-aware "For You" section.
 *
 * @see ActivityContext
 * @see HybridRecommendationEngine
 */
@Singleton
class ActivityAwareRecommender @Inject constructor() {

    /**
     * Returns a list of [SongItem]s matching the current [activity] context.
     *
     * @param activity The current physical activity.
     * @param limit    Maximum number of songs to return (default 10).
     * @return Up to [limit] songs, or an empty list if [activity] is
     *         [ActivityContext.Unknown] or the search fails.
     */
    suspend fun getActivitySongs(
        activity: ActivityContext,
        limit: Int = 10,
    ): List<SongItem> = withContext(Dispatchers.IO) {
        val query = activity.searchQuery ?: return@withContext emptyList()

        runCatching {
            val result: SearchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                .getOrThrow()
            result.items
                .filterIsInstance<SongItem>()
                .take(limit)
        }.getOrDefault(emptyList())
    }
}
