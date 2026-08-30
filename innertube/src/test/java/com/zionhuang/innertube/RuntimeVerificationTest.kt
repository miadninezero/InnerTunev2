package com.zionhuang.innertube

import com.zionhuang.innertube.models.YouTubeClient
import com.zionhuang.innertube.models.response.PlayerResponse
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test

class RuntimeVerificationTest {

    @Test
    fun testRealPlayerEndpoints() = runBlocking {
        val videoId = "dQw4w9WgXcQ" // Rick Astley - Never Gonna Give You Up (known public music video)
        val innerTube = InnerTube()
        val clients = listOf(
            YouTubeClient.ANDROID_VR_NO_AUTH,
            YouTubeClient.IOS,
            YouTubeClient.WEB_REMIX,
            YouTubeClient.ANDROID_MUSIC
        )

        println("=================================================================")
        println("STARTING REAL RUNTIME VERIFICATION FOR VIDEO: $videoId")
        println("=================================================================")

        for (client in clients) {
            println("\n>>> TESTING CLIENT: ${client.clientName} (v${client.clientVersion}, clientId=${client.clientId})")
            try {
                val httpResponse = innerTube.player(client, videoId, null)
                val statusCode = httpResponse.status.value
                println("1. HTTP Status Code: $statusCode")

                val playerResponse = httpResponse.body<PlayerResponse>()

                println("2. playabilityStatus.status: ${playerResponse.playabilityStatus.status}")
                println("3. playabilityStatus.reason: ${playerResponse.playabilityStatus.reason ?: "None"}")

                val streamingDataExists = playerResponse.streamingData != null
                println("4. streamingData exists: $streamingDataExists")

                val formats = playerResponse.streamingData?.adaptiveFormats.orEmpty()
                println("5. Number of adaptiveFormats: ${formats.size}")

                val audioFormats = formats.filter { it.isAudio }
                println("   - Audio formats count: ${audioFormats.size}")

                val selectedAudio = audioFormats.maxByOrNull { it.bitrate }
                if (selectedAudio != null) {
                    println("6. Selected audio format:")
                    println("   - itag: ${selectedAudio.itag}")
                    println("   - mimeType: ${selectedAudio.mimeType}")
                    println("   - bitrate: ${selectedAudio.bitrate}")
                    println("   - contentLength: ${selectedAudio.contentLength}")
                    println("   - url present: ${!selectedAudio.url.isNullOrBlank()}")

                    val streamUrl = selectedAudio.url
                    if (!streamUrl.isNullOrBlank()) {
                        println("7. Testing actual media GET request...")
                        val okHttpClient = OkHttpClient.Builder().build()
                        val mediaReq = Request.Builder()
                            .url(streamUrl)
                            .header("User-Agent", client.userAgent)
                            .header("Range", "bytes=0-1024")
                            .build()
                        val mediaResp = okHttpClient.newCall(mediaReq).execute()
                        println("   - Media GET response code: ${mediaResp.code}")
                        println("   - Media Content-Type: ${mediaResp.header("Content-Type")}")
                        println("   - Media Content-Length: ${mediaResp.header("Content-Length")}")
                        println("   - Media GET success: ${mediaResp.isSuccessful}")
                        mediaResp.close()
                    } else {
                        println("7. Media GET request skipped: URL is null or cipher/signature protected")
                    }
                } else {
                    println("6. No audio formats found.")
                    println("7. Media GET test: N/A")
                }
            } catch (e: Exception) {
                println("ERROR during test for ${client.clientName}: ${e.message}")
                e.printStackTrace()
            }
        }
        println("\n=================================================================")
        println("RUNTIME VERIFICATION COMPLETED")
        println("=================================================================")
    }
}
