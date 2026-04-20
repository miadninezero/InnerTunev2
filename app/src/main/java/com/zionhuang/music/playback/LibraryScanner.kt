package com.zionhuang.music.playback

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.zionhuang.music.db.MusicDatabase
import com.zionhuang.music.db.entities.ArtistEntity
import com.zionhuang.music.db.entities.LyricsEntity
import com.zionhuang.music.db.entities.SongArtistMap
import com.zionhuang.music.db.entities.SongEntity
import com.zionhuang.music.models.MediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase
) {
    // Must match exactly what Android stores - "Download/InnerTune/"
    private val RELATIVE_PATH = "Download/InnerTune/"
    // Regex: [YouTubeID] Artist Name - Song Title.m4a
    private val FILE_PATTERN = Regex("""^\[([A-Za-z0-9_-]{11})\] (.+?) - (.+?)\.m4a$""")

    suspend fun scanDownloadsFolder(): Int = withContext(Dispatchers.IO) {
        val audioFiles = mutableListOf<ParsedLocalSong>()
        val lyricsMap = mutableMapOf<String, String>() // youtubeId -> lyrics text

        // ── Step 1: Enumerate files ──
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns._ID
            )
            // Use exact match on RELATIVE_PATH
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf(RELATIVE_PATH)

            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: continue
                    when {
                        name.endsWith(".m4a") -> {
                            val parsed = parseFilename(name)
                            if (parsed != null) audioFiles.add(parsed)
                        }
                        name.endsWith(".lrc") -> {
                            // Extract YT ID from lrc filename
                            val ytId = parseLrcYouTubeId(name) ?: return@use
                            val file = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                "InnerTune/$name"
                            )
                            if (file.exists()) {
                                runCatching { lyricsMap[ytId] = file.readText() }
                            }
                        }
                    }
                }
            }
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "InnerTune"
            )
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    when {
                        file.name.endsWith(".m4a") -> {
                            val parsed = parseFilename(file.name)
                            if (parsed != null) audioFiles.add(parsed)
                        }
                        file.name.endsWith(".lrc") -> {
                            val ytId = parseLrcYouTubeId(file.name) ?: return@forEach
                            runCatching { lyricsMap[ytId] = file.readText() }
                        }
                    }
                }
            }
        }

        // ── Step 2: Insert each discovered song into the database ──
        var importedCount = 0
        audioFiles.forEach { parsed ->
            val songId = parsed.youtubeId

            // Build and insert the MediaMetadata object using the same path the rest of the app uses
            val artists = parsed.artistNames.map { name ->
                MediaMetadata.Artist(
                    id = ArtistEntity.generateArtistId(),
                    name = name
                )
            }
            val meta = MediaMetadata(
                id = songId,
                title = parsed.title,
                artists = artists,
                duration = -1,
                thumbnailUrl = null,
                album = null
            )

            // transaction{} is the correct wrapper for @Transaction DAO methods
            database.transaction {
                // insert returns -1L if conflict (song already exist) - ignore safely
                val songEntity = meta.toSongEntity().copy(inLibrary = LocalDateTime.now())
                val rowId = insert(songEntity)
                if (rowId != -1L) {
                    // Insert artist entities + mapping rows
                    artists.forEachIndexed { index, artist ->
                        val artistId = artist.id ?: ArtistEntity.generateArtistId()
                        insert(ArtistEntity(id = artistId, name = artist.name))
                        insert(SongArtistMap(songId = songId, artistId = artistId, position = index))
                    }
                    importedCount++
                }
            }

            // Insert lyrics separately
            val lyricsText = lyricsMap[songId]
            if (lyricsText != null) {
                database.query {
                    upsert(LyricsEntity(id = songId, lyrics = lyricsText))
                }
            }
        }

        importedCount
    }

    private fun parseFilename(filename: String): ParsedLocalSong? {
        val match = FILE_PATTERN.find(filename) ?: return null
        val youtubeId = match.groupValues[1]
        val artistStr = match.groupValues[2]
        val title = match.groupValues[3]
        return ParsedLocalSong(
            youtubeId = youtubeId,
            artistNames = artistStr.split(", ").map { it.trim() }.filter { it.isNotEmpty() },
            title = title
        )
    }

    private fun parseLrcYouTubeId(filename: String): String? {
        val regex = Regex("""^\[([A-Za-z0-9_-]{11})\]""")
        return regex.find(filename)?.groupValues?.get(1)
    }

    private data class ParsedLocalSong(
        val youtubeId: String,
        val artistNames: List<String>,
        val title: String
    )
}
