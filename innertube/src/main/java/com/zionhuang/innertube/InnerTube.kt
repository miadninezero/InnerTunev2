package com.zionhuang.innertube

import com.zionhuang.innertube.encoder.brotli
import com.zionhuang.innertube.models.Context
import com.zionhuang.innertube.models.YouTubeClient
import com.zionhuang.innertube.models.YouTubeLocale
import com.zionhuang.innertube.models.body.*
import com.zionhuang.innertube.utils.parseCookieString
import com.zionhuang.innertube.utils.sha1
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.encodeBase64
import kotlinx.coroutines.delay
import java.io.IOException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.net.Proxy
import java.util.*

/**
 * Provide access to InnerTube endpoints.
 * For making HTTP requests, not parsing response.
 */
class InnerTube {
    private var httpClient = createClient()

    var locale = YouTubeLocale(
        gl = Locale.getDefault().country,
        hl = Locale.getDefault().toLanguageTag()
    )
    // visitorData is nullable — it starts null and gets set from DataStore / sw.js_data
    var visitorData: String? = null
    var cookie: String? = null
        set(value) {
            field = value
            cookieMap = if (value == null) emptyMap() else parseCookieString(value)
        }
    private var cookieMap = emptyMap<String, String>()

    var proxy: Proxy? = null
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }

    var useLoginForBrowse: Boolean = false

    @OptIn(ExperimentalSerializationApi::class)
    private fun createClient() = HttpClient(OkHttp) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            })
        }

        install(ContentEncoding) {
            brotli(1.0F)
            gzip(0.9F)
            deflate(0.8F)
        }

        // Timeout config matching Estrella-Music — prevents search hanging forever
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000   // 30s total request timeout
            connectTimeoutMillis = 15_000   // 15s connect timeout
            socketTimeoutMillis = 30_000    // 30s socket read timeout
        }

        engine {
            config {
                connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                retryOnConnectionFailure(true)
            }
            if (proxy != null) {
                proxy = this@InnerTube.proxy
            }
        }

        defaultRequest {
            url("https://music.youtube.com/youtubei/v1/")
            header("Accept", "application/json")
            header("Accept-Language", "en-US,en;q=0.9")
            header("Cache-Control", "no-cache")
        }
    }

    /**
     * Simple retry wrapper for transient IO errors (socket aborts, timeouts).
     * Retries the given block up to [maxAttempts] times with exponential backoff.
     * Cancellation is respected since [delay] will throw if the coroutine is cancelled.
     */
    private suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelay: Long = 500L,
        factor: Double = 2.0,
        block: suspend () -> T,
    ): T {
        var currentDelay = initialDelay
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: IOException) {
                attempt++
                if (attempt >= maxAttempts) throw e
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
    }

    private fun HttpRequestBuilder.ytClient(client: YouTubeClient, setLogin: Boolean = false) {
        contentType(ContentType.Application.Json)
        val origin = if (client == YouTubeClient.WEB_REMIX || client == YouTubeClient.ANDROID_MUSIC) {
            YouTubeClient.ORIGIN_YOUTUBE_MUSIC
        } else {
            "https://www.youtube.com"
        }
        headers {
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", client.clientId)
            append("X-YouTube-Client-Version", client.clientVersion)
            append("X-Origin", origin)
            append("Referer", "$origin/")
            visitorData?.takeIf { it.isNotBlank() }?.let { append("X-Goog-Visitor-Id", it) }
        }
        userAgent(client.userAgent)
        parameter("prettyPrint", false)
    }

    suspend fun search(
        client: YouTubeClient,
        query: String? = null,
        params: String? = null,
        continuation: String? = null,
    ) = withRetry {
        httpClient.post("search") {
            ytClient(client, setLogin = useLoginForBrowse)
            setBody(
                SearchBody(
                    context = client.toContext(locale, visitorData),
                    query = query,
                    params = params
                )
            )
            parameter("continuation", continuation)
            parameter("ctoken", continuation)
        }
    }

    suspend fun player(
        client: YouTubeClient,
        videoId: String,
        playlistId: String?,
    ) = withRetry {
        val endpoint = if (client == YouTubeClient.WEB_REMIX || client == YouTubeClient.ANDROID_MUSIC) {
            "https://music.youtube.com/youtubei/v1/player"
        } else {
            "https://www.youtube.com/youtubei/v1/player"
        }
        val origin = if (client == YouTubeClient.WEB_REMIX || client == YouTubeClient.ANDROID_MUSIC) {
            YouTubeClient.ORIGIN_YOUTUBE_MUSIC
        } else {
            "https://www.youtube.com"
        }
        httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            headers {
                append("X-Goog-Api-Format-Version", "1")
                append("X-YouTube-Client-Name", client.clientId)
                append("X-YouTube-Client-Version", client.clientVersion)
                append("X-Origin", origin)
                append("Referer", "$origin/")
                visitorData?.takeIf { it.isNotBlank() }?.let { append("X-Goog-Visitor-Id", it) }
            }
            userAgent(client.userAgent)
            parameter("prettyPrint", false)
            setBody(
                PlayerBody(
                    context = client.toContext(locale, visitorData).let {
                        if (client.clientName == "TVHTML5_SIMPLY_EMBEDDED_PLAYER") {
                            it.copy(
                                thirdParty = Context.ThirdParty(
                                    embedUrl = "https://www.youtube.com/watch?v=${videoId}"
                                )
                            )
                        } else it
                    },
                    videoId = videoId,
                    playlistId = playlistId
                )
            )
        }
    }

    suspend fun browse(
        client: YouTubeClient,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
        setLogin: Boolean = false,
    ) = withRetry {
        httpClient.post("browse") {
            ytClient(client, setLogin = setLogin || useLoginForBrowse)
            setBody(
                BrowseBody(
                    context = client.toContext(locale, visitorData),
                    browseId = browseId,
                    params = params
                )
            )
            parameter("continuation", continuation)
            parameter("ctoken", continuation)
            if (continuation != null) {
                parameter("type", "next")
            }
        }
    }

    suspend fun next(
        client: YouTubeClient,
        videoId: String?,
        playlistId: String?,
        playlistSetVideoId: String?,
        index: Int?,
        params: String?,
        continuation: String? = null,
    ) = withRetry {
        httpClient.post("next") {
            ytClient(client, setLogin = true)
            setBody(
                NextBody(
                    context = client.toContext(locale, visitorData),
                    videoId = videoId,
                    playlistId = playlistId,
                    playlistSetVideoId = playlistSetVideoId,
                    index = index,
                    params = params,
                    continuation = continuation
                )
            )
        }
    }

    suspend fun getSearchSuggestions(
        client: YouTubeClient,
        input: String,
    ) = withRetry {
        httpClient.post("music/get_search_suggestions") {
            ytClient(client)
            setBody(
                GetSearchSuggestionsBody(
                    context = client.toContext(locale, visitorData),
                    input = input
                )
            )
        }
    }

    suspend fun getQueue(
        client: YouTubeClient,
        videoIds: List<String>?,
        playlistId: String?,
    ) = withRetry {
        httpClient.post("music/get_queue") {
            ytClient(client)
            setBody(
                GetQueueBody(
                    context = client.toContext(locale, visitorData),
                    videoIds = videoIds,
                    playlistId = playlistId
                )
            )
        }
    }

    suspend fun getTranscript(
        client: YouTubeClient,
        videoId: String,
    ) = withRetry {
        httpClient.post("https://music.youtube.com/youtubei/v1/get_transcript") {
            headers {
                append("Content-Type", "application/json")
            }
            setBody(
                GetTranscriptBody(
                    context = client.toContext(locale, null),
                    params = "\n${11.toChar()}$videoId".encodeBase64()
                )
            )
        }
    }

    suspend fun getSwJsData() = withRetry { httpClient.get("https://music.youtube.com/sw.js_data") }

    suspend fun accountMenu(client: YouTubeClient) = withRetry {
        httpClient.post("account/account_menu") {
            ytClient(client, setLogin = true)
            setBody(AccountMenuBody(client.toContext(locale, visitorData)))
        }
    }

    /**
     * Fetch stream information from a Piped API instance.
     * Piped decodes YouTube's signatureCipher server-side, so the returned
     * URLs are directly playable without any client-side signature logic.
     *
     * @param videoId YouTube video ID
     * @param instanceUrl Base URL of the Piped instance (e.g. "https://pipedapi.kavin.rocks")
     */
    suspend fun pipedStreams(videoId: String, instanceUrl: String) =
        httpClient.get("$instanceUrl/streams/$videoId") {
            header("Accept", "application/json")
            // Piped API requires no auth — intentionally skip all YouTube headers
        }
}
