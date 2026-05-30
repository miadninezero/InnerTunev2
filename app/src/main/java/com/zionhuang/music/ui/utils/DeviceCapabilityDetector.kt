package com.zionhuang.music.ui.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Detects device capability to determine which animations/effects to show.
 * Enables progressive enhancement based on device specs.
 */
object DeviceCapabilityDetector {
    private var cachedCapabilities: DeviceCapabilities? = null

    data class DeviceCapabilities(
        val isHighEnd: Boolean,       // 2GB+ RAM, newer Android
        val isMidRange: Boolean,      // 1-2GB RAM
        val isLowEnd: Boolean,        // <1GB RAM
        val supportsBlur: Boolean,    // Ability to render blur effects
        val targetFrameRate: Int,     // 60fps for high/mid, 30fps for low
        val animationScaleFactor: Float  // 1.0f for high, 0.5-0.75f for mid, 0.25f for low
    )

    fun getCapabilities(context: Context): DeviceCapabilities {
        if (cachedCapabilities != null) {
            return cachedCapabilities!!
        }

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val androidVersion = Build.VERSION.SDK_INT

        val isHighEnd = totalRamMb >= 2048 && androidVersion >= Build.VERSION_CODES.Q
        val isMidRange = totalRamMb in 1024..2047
        val isLowEnd = totalRamMb < 1024

        val supportsBlur = androidVersion >= Build.VERSION_CODES.S // Android 12+
        val targetFrameRate = when {
            isHighEnd -> 60
            isMidRange -> 60
            else -> 30
        }

        val animationScaleFactor = when {
            isHighEnd -> 1.0f
            isMidRange -> 0.65f
            else -> 0.25f
        }

        val capabilities = DeviceCapabilities(
            isHighEnd = isHighEnd,
            isMidRange = isMidRange,
            isLowEnd = isLowEnd,
            supportsBlur = supportsBlur,
            targetFrameRate = targetFrameRate,
            animationScaleFactor = animationScaleFactor
        )

        cachedCapabilities = capabilities
        return capabilities
    }

    // Convenience getters
    fun isHighEndDevice(context: Context): Boolean = getCapabilities(context).isHighEnd
    fun isMidRangeDevice(context: Context): Boolean = getCapabilities(context).isMidRange
    fun isLowEndDevice(context: Context): Boolean = getCapabilities(context).isLowEnd
    fun supportsBlurEffects(context: Context): Boolean = getCapabilities(context).supportsBlur
}
