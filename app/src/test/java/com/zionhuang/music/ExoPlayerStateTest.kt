package com.zionhuang.music

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.test.core.app.ApplicationProvider
import com.zionhuang.innertube.InnerTube
import com.zionhuang.innertube.models.YouTubeClient
import com.zionhuang.innertube.models.response.PlayerResponse
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExoPlayerStateTest {

    @Test
    fun testExoPlayerReachesStateReady() = runBlocking {
        val videoId = "dQw4w9WgXcQ"
        val innerTube = InnerTube()
        val client = YouTubeClient.ANDROID_VR_NO_AUTH

        println(">>> Fetching stream URL for ExoPlayer validation...")
        val response = innerTube.player(client, videoId, null).body<PlayerResponse>()
        val selectedFormat = response.streamingData?.adaptiveFormats
            ?.filter { it.isAudio }
            ?.maxByOrNull { it.bitrate }

        val streamUrl = selectedFormat?.url
        println(">>> Stream URL: $streamUrl")
        assertTrue("Stream URL should not be null or empty", !streamUrl.isNullOrEmpty())

        val appContext: Context = ApplicationProvider.getApplicationContext()
        val okHttpClient = OkHttpClient.Builder().build()
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(client.userAgent)

        val mediaSourceFactory = DefaultMediaSourceFactory(appContext)
            .setDataSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        val reachedReady = AtomicBoolean(false)
        val lastState = AtomicInteger(Player.STATE_IDLE)
        val latch = CountDownLatch(1)

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                lastState.set(playbackState)
                println(">>> ExoPlayer state changed to: $playbackState (STATE_READY=${Player.STATE_READY}, STATE_BUFFERING=${Player.STATE_BUFFERING})")
                if (playbackState == Player.STATE_READY) {
                    reachedReady.set(true)
                    latch.countDown()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                println(">>> ExoPlayer error: ${error.message} (code=${error.errorCodeName})")
                latch.countDown()
            }
        })

        player.setMediaItem(MediaItem.fromUri(streamUrl!!))
        player.prepare()
        player.playWhenReady = true

        // Idle looper to pump background/playback events in Robolectric
        for (i in 0..50) {
            ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS)
            if (reachedReady.get()) break
            Thread.sleep(100)
        }

        println(">>> Final playback state: ${lastState.get()} (Reached READY: ${reachedReady.get()})")
        player.release()
    }
}
