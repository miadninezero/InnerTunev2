package com.zionhuang.music.data.recommendation

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Privacy-first wrapper around Google's `ActivityRecognitionClient`.
 *
 * ## Privacy guarantee
 * - Activity recognition is **opt-in**: this class does nothing until the user
 *   explicitly enables it in settings AND grants the
 *   `ACTIVITY_RECOGNITION` runtime permission.
 * - No data is persisted; the current [ActivityContext] is held only in
 *   memory for the lifetime of the process.
 * - Updates are stopped and the in-memory state reset to [ActivityContext.Unknown]
 *   whenever [stop] is called (e.g. the user toggles the setting off).
 *
 * ## How it works
 * Requests 30-second interval updates from the Activity Recognition API.
 * Results are delivered to [ActivityUpdateReceiver] via a `PendingIntent`,
 * which converts the `DetectedActivity` to an [ActivityContext] and pushes it
 * into the [activityFlow].
 *
 * @see ActivityUpdateReceiver
 */
@Singleton
class ActivityRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "ActivityRecognitionMgr"

        /** Action string for the activity-recognition PendingIntent. */
        const val ACTION_ACTIVITY_UPDATE =
            "com.zionhuang.music.ACTION_ACTIVITY_UPDATE"

        /** Interval between activity polls (30 seconds). */
        private const val DETECTION_INTERVAL_MS = 30_000L

        // Shared state: written by the BroadcastReceiver, read by anyone.
        internal val _activityFlow =
            MutableStateFlow<ActivityContext>(ActivityContext.Unknown)
    }

    /** Emits the latest detected [ActivityContext]. Always starts as [ActivityContext.Unknown]. */
    val activityFlow: StateFlow<ActivityContext> = _activityFlow.asStateFlow()

    private val recognitionClient = ActivityRecognition.getClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ActivityUpdateReceiver::class.java).apply {
            action = ACTION_ACTIVITY_UPDATE
        }
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns `true` if the `ACTIVITY_RECOGNITION` permission has been granted.
     * On API < 29 the permission is automatically granted at install time.
     */
    fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Starts requesting activity updates if permission is granted.
     * Idempotent — safe to call multiple times.
     */
    fun start() {
        if (!hasPermission()) {
            Log.w(TAG, "ACTIVITY_RECOGNITION permission not granted — not starting")
            return
        }
        recognitionClient
            .requestActivityUpdates(DETECTION_INTERVAL_MS, pendingIntent)
            .addOnSuccessListener { Log.d(TAG, "Activity recognition started") }
            .addOnFailureListener { Log.e(TAG, "Failed to start activity recognition", it) }
    }

    /**
     * Stops requesting activity updates and resets the flow to
     * [ActivityContext.Unknown] so stale data doesn't influence recommendations.
     */
    fun stop() {
        recognitionClient
            .removeActivityUpdates(pendingIntent)
            .addOnSuccessListener { Log.d(TAG, "Activity recognition stopped") }
            .addOnFailureListener { Log.e(TAG, "Failed to stop activity recognition", it) }
        _activityFlow.value = ActivityContext.Unknown
    }
}

/**
 * Receives activity recognition PendingIntent broadcasts from the OS,
 * converts the best [DetectedActivity] into an [ActivityContext], and
 * updates the shared [ActivityRecognitionManager._activityFlow].
 *
 * Registered in `AndroidManifest.xml` with action
 * `com.zionhuang.music.ACTION_ACTIVITY_UPDATE`.
 */
class ActivityUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityRecognitionResult.hasResult(intent)) return
        val result = ActivityRecognitionResult.extractResult(intent) ?: return

        val best = result.mostProbableActivity
        val activityContext = best.toActivityContext()

        Log.d("ActivityUpdateReceiver",
            "Activity=${best.type} confidence=${best.confidence} → $activityContext")

        ActivityRecognitionManager._activityFlow.value = activityContext
    }

    private fun DetectedActivity.toActivityContext(): ActivityContext = when (type) {
        DetectedActivity.RUNNING  -> ActivityContext.Running
        DetectedActivity.WALKING  -> ActivityContext.Walking
        DetectedActivity.ON_FOOT  -> ActivityContext.Walking   // on_foot ≈ walking
        DetectedActivity.STILL    -> ActivityContext.Still
        DetectedActivity.IN_VEHICLE,
        DetectedActivity.ON_BICYCLE -> ActivityContext.Vehicle
        // TILTING, UNKNOWN, etc.
        else                      -> ActivityContext.Unknown
    }
}
