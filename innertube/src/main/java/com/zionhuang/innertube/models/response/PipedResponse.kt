package com.zionhuang.innertube.models.response

import com.zionhuang.innertube.models.ResponseContext
import com.zionhuang.innertube.models.Thumbnail
import com.zionhuang.innertube.models.Thumbnails
import kotlinx.serialization.Serializable

/**
 * Response from the Piped API `/streams/:videoId` endpoint.
 * Piped handles YouTube's signatureCipher decoding server-side and
 * returns direct, playable stream URLs.
 */
@Serializable
data class PipedResponse(
    val audioStreams: List<AudioStream> = emptyList(),
    val title: String? = null,
    val uploader: String? = null,
    val uploaderUrl: String? = null,
    val duration: Int = 0, // seconds
    val thumbnailUrl: String? = null,
) {
    @Serializable
    data class AudioStream(
        val itag: Int,
        val url: String,
        val bitrate: Int,
        val mimeType: String? = null,
        val codec: String? = null,
        val quality: String? = null,
        val contentLength: Long = -1,
    )

    /**
     * Convert this Piped response into a [PlayerResponse] so the rest of the
     * playback pipeline (MusicService / ResolvingDataSource) can consume it
     * without any special-casing.
     */
    fun toPlayerResponse(videoId: String): PlayerResponse {
        val adaptiveFormats = audioStreams.map { stream ->
            PlayerResponse.StreamingData.Format(
                itag = stream.itag,
                url = stream.url,
                mimeType = stream.mimeType ?: "audio/mp4; codecs=\"mp4a.40.2\"",
                bitrate = stream.bitrate,
                width = null,       // audio-only
                height = null,      // audio-only
                contentLength = if (stream.contentLength > 0) stream.contentLength else null,
                quality = stream.quality ?: "medium",
                fps = null,
                qualityLabel = null,
                averageBitrate = stream.bitrate,
                audioQuality = stream.quality,
                approxDurationMs = (duration * 1000L).toString(),
                audioSampleRate = 44100,
                audioChannels = 2,
                loudnessDb = null,
                lastModified = null,
            )
        }

        return PlayerResponse(
            responseContext = ResponseContext(
                visitorData = null,
                serviceTrackingParams = null,
            ),
            playabilityStatus = PlayerResponse.PlayabilityStatus(
                status = "OK",
                reason = null,
            ),
            playerConfig = null,
            streamingData = if (adaptiveFormats.isNotEmpty()) {
                PlayerResponse.StreamingData(
                    formats = null,
                    adaptiveFormats = adaptiveFormats,
                    expiresInSeconds = 3600, // Piped URLs typically last ~1 hour
                )
            } else null,
            videoDetails = PlayerResponse.VideoDetails(
                videoId = videoId,
                title = title ?: "Unknown",
                author = uploader ?: "Unknown",
                channelId = uploaderUrl?.removePrefix("/channel/") ?: "",
                lengthSeconds = duration.toString(),
                musicVideoType = null,
                viewCount = "0",
                thumbnail = Thumbnails(
                    thumbnails = if (thumbnailUrl != null) {
                        listOf(Thumbnail(
                            url = thumbnailUrl,
                            width = 480,
                            height = 360,
                        ))
                    } else emptyList()
                ),
            ),
        )
    }
}
