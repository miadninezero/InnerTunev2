package com.zionhuang.music.utils

import android.content.Context
import com.zionhuang.music.R

object PlayabilityUtil {
    fun userMessageForReason(context: Context, reason: String?): String {
        if (reason.isNullOrBlank()) return context.getString(R.string.error_no_stream)
        val r = reason.lowercase()
        return when {
            "geo" in r || "country" in r || "restricted" in r && "geo" in r -> context.getString(R.string.error_playability_geo)
            "private" in r -> context.getString(R.string.error_playability_private)
            "login" in r || "auth" in r || "sign" in r -> context.getString(R.string.error_playability_login)
            "age" in r || "adult" in r || "nsfw" in r -> context.getString(R.string.error_playability_age)
            else -> context.getString(R.string.error_playability_reason, reason)
        }
    }
}
