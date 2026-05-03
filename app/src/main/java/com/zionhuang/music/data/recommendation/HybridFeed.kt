package com.zionhuang.music.data.recommendation

import com.zionhuang.innertube.models.AlbumItem
import com.zionhuang.innertube.models.SongItem

/**
 * The output of [HybridRecommendationEngine.load].
 *
 * @param forYou        Personalised song recommendations derived from the
 *                      user's most-played and liked songs via YouTube's
 *                      `related` endpoint.
 * @param forYouAlbums  New-release albums that complement the personal feed.
 * @param hotInYourArea Top charting songs in the user's country ([countryCode]).
 * @param globalTop50   The worldwide YouTube Music top-50 chart songs.
 * @param moreLikeThis  "Because you liked X" section — songs similar to the
 *                      user's top liked / most-played tracks, with a descriptive
 *                      label. Null until [HybridRecommendationEngine.getSimilarContent]
 *                      has resolved, or when there is no history.
 * @param countryCode   ISO 3166-1 alpha-2 country code used for the regional section.
 */
data class HybridFeed(
    val forYou: List<SongItem>,
    val forYouAlbums: List<AlbumItem>,
    val hotInYourArea: List<SongItem>,
    val globalTop50: List<SongItem>,
    val moreLikeThis: SimilarContentFeed? = null,
    val countryCode: String,
)

/**
 * Output of [HybridRecommendationEngine.getSimilarContent].
 *
 * @param songs     Up to 20 deduplicated [SongItem]s similar to [seedSongTitle]
 *                  (or [seedArtistName]).
 * @param seedSongTitle   The title of the primary seed song shown in the header.
 * @param seedArtistName  The primary artist name of the seed — used in the
 *                        section label "Because you liked [seedArtistName]".
 */
data class SimilarContentFeed(
    val songs: List<SongItem>,
    val seedSongTitle: String,
    val seedArtistName: String,
) {
    /** Human-readable section header, e.g. "Because you liked Adele". */
    val sectionLabel: String get() = "Because you liked $seedArtistName"
}

/**
 * Sealed UI state for the hybrid recommendation screen.
 */
sealed class RecommendationUiState {
    /** Initial state while the first load is running. */
    data object Loading : RecommendationUiState()

    /** Feed loaded successfully. */
    data class Success(val feed: HybridFeed) : RecommendationUiState()

    /**
     * A non-fatal error occurred (e.g. no internet).  The [message] can be
     * shown to the user.  [partialFeed] is non-null if at least one section
     * was loaded successfully before the error.
     */
    data class Error(
        val message: String,
        val partialFeed: HybridFeed? = null,
    ) : RecommendationUiState()
}
