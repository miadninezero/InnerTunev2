package com.zionhuang.innertube

import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.YouTubeClient
import com.zionhuang.innertube.models.response.PlayerResponse
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test

class LoggedOutSearchAndPlayTest {

    @Test
    fun testCompleteLoggedOutWorkflow() = runBlocking {
        println("=================================================================")
        println("TEST: FULLY LOGGED OUT SEARCH & PLAYBACK DIAGNOSTICS")
        println("=================================================================")

        // Force strictly null/logged-out state
        YouTube.cookie = null
        YouTube.visitorData = null

        val searchQuery = "Never Gonna Give You Up"
        println("\n>>> STEP 1: EXECUTING SEARCH QUERY: \"$searchQuery\"")
        val searchEndpoint = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
        println("Search Target Endpoint: $searchEndpoint")
        println("Client: WEB_REMIX (clientId=67)")
        println("Auth headers: NONE (Logged out, cookie=null, visitorData=null)")

        val searchResult = YouTube.search(searchQuery, YouTube.SearchFilter.FILTER_SONG)
        if (searchResult.isFailure) {
            val error = searchResult.exceptionOrNull()
            println("❌ SEARCH FAILED with error: ${error?.message}")
            error?.printStackTrace()
            return@runBlocking
        }

        val searchPage = searchResult.getOrThrow()
        println("✅ SEARCH SUCCEEDED: Found ${searchPage.items.size} items.")
        if (searchPage.items.isEmpty()) {
            println("❌ No items returned in search results.")
            return@runBlocking
        }

        val firstItem = searchPage.items.first()
        println("Selected Item from Search:")
        println("  - Title: ${firstItem.title}")
        println("  - ID: ${firstItem.id}")
        if (firstItem is SongItem) {
            println("  - Artists: ${firstItem.artists.joinToString { it.name }}")
        }

        println("\n>>> STEP 2: FETCHING PLAYER RESPONSE FOR SONG ID: ${firstItem.id}")
        val playerResult = YouTube.player(firstItem.id)
        if (playerResult.isFailure) {
            val error = playerResult.exceptionOrNull()
            println("❌ PLAYER RESOLUTION FAILED: ${error?.message}")
            error?.printStackTrace()
            return@runBlocking
        }

        val playerResponse = playerResult.getOrThrow()
        println("✅ PLAYER RESOLUTION SUCCEEDED:")
        println("  - playabilityStatus.status: ${playerResponse.playabilityStatus.status}")
        println("  - playabilityStatus.reason: ${playerResponse.playabilityStatus.reason ?: "None"}")
        println("  - streamingData exists: ${playerResponse.streamingData != null}")
        val formats = playerResponse.streamingData?.adaptiveFormats.orEmpty()
        println("  - adaptiveFormats count: ${formats.size}")

        val audioFormats = formats.filter { it.isAudio }
        println("  - audioFormats count: ${audioFormats.size}")

        val selectedFormat = audioFormats.maxByOrNull { it.bitrate }
        if (selectedFormat == null || selectedFormat.url.isNullOrBlank()) {
            println("❌ NO PLAYABLE AUDIO STREAM URL FOUND")
            return@runBlocking
        }

        println("Selected Audio Format:")
        println("  - itag: ${selectedFormat.itag}")
        println("  - mimeType: ${selectedFormat.mimeType}")
        println("  - bitrate: ${selectedFormat.bitrate}")
        println("  - url: ${selectedFormat.url?.take(60)}...")

        println("\n>>> STEP 3: PERFORMING MEDIA GET RANGE REQUEST")
        val client = OkHttpClient.Builder().build()
        val req = Request.Builder()
            .url(selectedFormat.url!!)
            .header("User-Agent", YouTubeClient.ANDROID_VR_NO_AUTH.userAgent)
            .header("Range", "bytes=0-1024")
            .build()

        val resp = client.newCall(req).execute()
        println("Media GET Response Code: ${resp.code}")
        println("Media GET Content-Type: ${resp.header("Content-Type")}")
        println("Media GET Content-Length: ${resp.header("Content-Length")}")
        println("Media GET isSuccessful: ${resp.isSuccessful}")
        resp.close()

        println("\n=================================================================")
        println("DIAGNOSTICS COMPLETED")
        println("=================================================================")
    }
}
