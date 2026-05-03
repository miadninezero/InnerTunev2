package com.zionhuang.music.data.recommendation

/**
 * A simplified, privacy-oriented representation of the user's current physical
 * activity, derived from the Google Activity Recognition API.
 *
 * The value maps 1-to-1 with the most-confident `DetectedActivity` type
 * delivered by `ActivityRecognitionClient`, but expressed as an app-level
 * concept so the rest of the codebase stays decoupled from GMS types.
 */
sealed class ActivityContext {
    /** Running (high confidence IN_VEHICLE is excluded; RUNNING or WALKING fast) */
    data object Running : ActivityContext()

    /** Walking at a moderate pace */
    data object Walking : ActivityContext()

    /** Stationary — on a couch, at a desk, etc. */
    data object Still : ActivityContext()

    /** In a car, bus, train, or other vehicle */
    data object Vehicle : ActivityContext()

    /** Activity could not be determined (permission denied, first start, etc.) */
    data object Unknown : ActivityContext()

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * A human-readable label shown in the notification / status chip.
     * e.g. "Running 🏃" or "In a vehicle 🚗"
     */
    val label: String get() = when (this) {
        Running -> "Running 🏃"
        Walking -> "Walking 🚶"
        Still   -> "Relaxing 🛋️"
        Vehicle -> "In a vehicle 🚗"
        Unknown -> "Unknown"
    }

    /**
     * The YouTube Music search query injected into the "For You" section when
     * this activity is active.  Returns null for [Unknown] (no injection).
     */
    val searchQuery: String? get() = when (this) {
        Running -> "workout running high energy music"
        Walking -> "feel good walking music"
        Still   -> "lofi chill ambient relaxing"
        Vehicle -> "road trip driving mix playlist"
        Unknown -> null
    }
}
