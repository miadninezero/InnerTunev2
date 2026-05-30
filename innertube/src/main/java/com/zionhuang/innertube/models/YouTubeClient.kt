package com.zionhuang.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val clientId: String,           // Numeric ID sent in X-YouTube-Client-Name header
    val api_key: String? = null,
    val userAgent: String,
    val osVersion: String? = null,
    val referer: String? = null,
    /**
     * Whether this client supports sending login cookies (Authorization: SAPISIDHASH).
     * Clients like ANDROID_VR, IOS, TVHTML5_SIMPLY_EMBEDDED_PLAYER must NOT receive
     * auth headers — doing so causes YouTube to reject them with "no longer supported" errors.
     */
    val loginSupported: Boolean = false,
) {
    fun toContext(locale: YouTubeLocale, visitorData: String?) = Context(
        client = Context.Client(
            clientName = clientName,
            clientVersion = clientVersion,
            osVersion = osVersion,
            gl = locale.gl,
            hl = locale.hl,
            visitorData = visitorData
        )
    )

    companion object {
        private const val REFERER_YOUTUBE_MUSIC = "https://music.youtube.com/"
        const val ORIGIN_YOUTUBE_MUSIC = "https://music.youtube.com"

        private const val USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
        private const val USER_AGENT_ANDROID_MUSIC = "com.google.android.apps.youtube.music/7.25.52 (Linux; U; Android 11) gzip"
        private const val USER_AGENT_TV = "Mozilla/5.0(SMART-TV; Linux; Tizen 4.0.0.2) AppleWebkit/605.1.15 (KHTML, like Gecko) SamsungBrowser/9.2 TV Safari/605.1.15"

        // loginSupported = true → sends Authorization: SAPISIDHASH when logged in
        // loginSupported = false → NEVER sends auth headers (these clients break with auth)

        val ANDROID_MUSIC = YouTubeClient(
            clientName = "ANDROID_MUSIC",
            // bumped minor version for compatibility testing
            clientVersion = "8.0.0",
            clientId = "21",
            api_key = "AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI",
            userAgent = USER_AGENT_ANDROID_MUSIC,
            loginSupported = true
        )

        val ANDROID = YouTubeClient(
            clientName = "ANDROID",
            clientVersion = "21.03.38",
            clientId = "3",
            api_key = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w",
            userAgent = "com.google.android.youtube/21.03.38 (Linux; U; Android 14) gzip",
            loginSupported = false   // plain ANDROID doesn't reliably support SAPISID auth
        )

        val WEB = YouTubeClient(
            clientName = "WEB",
            clientVersion = "2.20260530.00.00",
            clientId = "1",
            api_key = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3",
            userAgent = USER_AGENT_WEB,
            loginSupported = true
        )

        val WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20260530.01.00",
            clientId = "67",
            api_key = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3",
            userAgent = USER_AGENT_WEB,
            referer = REFERER_YOUTUBE_MUSIC,
            loginSupported = true   // Primary logged-in client
        )

        // TVHTML5 full TV client — supports login for age-restricted content
        val TVHTML5 = YouTubeClient(
            clientName = "TVHTML5",
            clientVersion = "7.20260530.00.00",
            clientId = "7",
            api_key = "AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8",
            userAgent = USER_AGENT_TV,
            loginSupported = true
        )

        // Embedded TV client — NO login support. Bypasses age restrictions without auth.
        val TVHTML5_SIMPLY_EMBEDDED_PLAYER = YouTubeClient(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            clientId = "85",
            userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.02) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15",
            loginSupported = false  // MUST be false — breaks with auth headers
        )

        // IOS client v21.03.1 — NO login support via SAPISID
        val IOS = YouTubeClient(
            clientName = "IOS",
            clientVersion = "21.03.2",
            clientId = "5",
            api_key = "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc",
            userAgent = "com.google.ios.youtube/21.03.2 (iPhone16,2; U; CPU iOS 18_2 like Mac OS X;)",
            osVersion = "18.2.22C152",
            loginSupported = false  // MUST be false — sending auth causes rejection
        )

        // Android VR (Oculus Quest) — PRIMARY working fallback from Estrella-Music.
        // NO login/auth headers — this is exactly why it works as a fallback.
        val ANDROID_VR_NO_AUTH = YouTubeClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.61.48",
            clientId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.61.48 (Linux; U; Android 12; en_US; Oculus Quest 3; Build/SQ3A.220605.009.A1; Cronet/132.0.6808.3)",
            loginSupported = false  // MUST be false — never send auth with this client
        )
    }
}
