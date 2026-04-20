package com.zionhuang.music.playback

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import com.zionhuang.music.db.MusicDatabase
import com.zionhuang.music.db.entities.LyricsEntity
import com.zionhuang.music.di.DownloadCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSyncUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    @DownloadCache private val downloadCache: SimpleCache
) {
    // Use the exact relative path Android uses in MediaStore (no leading slash, trailing slash required)
    private val RELATIVE_PATH = "Download/InnerTune/"

    suspend fun exportDownloadToMediaStore(songId: String) = withContext(Dispatchers.IO) {
        // ── Step 1: delete any old copies first to prevent duplicates ──
        deleteDownloadFromMediaStore(songId)

        val song = database.song(songId).firstOrNull() ?: return@withContext
        val artistName = song.artists.joinToString(", ") { it.name }
        val title = song.song.title
        val fileNameBase = "[$songId] $artistName - $title"
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .take(200) // cap length to avoid filesystem limits
        val audioFileName = "$fileNameBase.m4a"
        val lyricsFileName = "$fileNameBase.lrc"

        val resolver = context.contentResolver

        // ── Step 2: Export audio ──
        val audioValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, audioFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val audioUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, audioValues)
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "InnerTune")
            dir.mkdirs()
            val file = File(dir, audioFileName)
            file.createNewFile()
            file.toUri()
        }

        if (audioUri != null) {
            try {
                resolver.openOutputStream(audioUri)?.use { output ->
                    val dataSource = CacheDataSource.Factory()
                        .setCache(downloadCache)
                        .setUpstreamDataSourceFactory(null)
                        .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                        .createDataSource()

                    val dataSpec = DataSpec.Builder()
                        .setKey(songId)
                        .setUri("innertune://download/$songId".toUri())
                        .build()
                    try {
                        dataSource.open(dataSpec)
                        val buffer = ByteArray(65536)
                        var read: Int
                        while (dataSource.read(buffer, 0, buffer.size).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                        }
                    } finally {
                        dataSource.close()
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    audioValues.clear()
                    audioValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(audioUri, audioValues, null, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Delete the pending entry so it doesn't appear as a ghost file
                resolver.delete(audioUri, null, null)
            }
        }

        // ── Step 3: Export lyrics ──
        val lyricsEntity = database.lyrics(songId).firstOrNull()
        if (lyricsEntity != null && lyricsEntity.lyrics != LyricsEntity.LYRICS_NOT_FOUND) {
            val lyricsValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, lyricsFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
                }
            }
            try {
                val lyricsUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, lyricsValues)
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "InnerTune")
                    File(dir, lyricsFileName).also { it.createNewFile() }.toUri()
                }
                lyricsUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { output ->
                        output.write(lyricsEntity.lyrics.toByteArray(Charsets.UTF_8))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteDownloadFromMediaStore(songId: String) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android stores RELATIVE_PATH as "Download/InnerTune/" — must be exact match
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf(RELATIVE_PATH, "[$songId]%")
            resolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs)
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "InnerTune"
            )
            dir.listFiles { _, name -> name.startsWith("[$songId]") }?.forEach { it.delete() }
        }
    }
}
