package com.zionhuang.music.playback

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.zionhuang.music.db.MusicDatabase
import com.zionhuang.music.db.entities.Artist
import com.zionhuang.music.db.entities.LyricsEntity
import com.zionhuang.music.db.entities.Song
import com.zionhuang.music.db.entities.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase
) {
    suspend fun scanDownloadsFolder() = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val relativePath = Environment.DIRECTORY_DOWNLOADS + "/InnerTune"

        val audioFiles = mutableListOf<ParsedLocalSong>()
        val lyricsMap = mutableMapOf<String, String>() // Base name -> Lyrics text

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns._ID
            )
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("$relativePath%")
            
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameColumn)
                    if (name.endsWith(".m4a")) {
                        audioFiles.add(ParsedLocalSong(name))
                    } else if (name.endsWith(".lrc")) {
                        // We would need to read the content to get the lyrics.
                        // For simplicity, let's just use the File API since Downloads/InnerTune
                        // is public and we can construct the path.
                        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "InnerTune/$name")
                        if (file.exists()) {
                            try {
                                lyricsMap[name.removeSuffix(".lrc")] = file.readText()
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                }
            }
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "InnerTune")
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".m4a")) {
                        audioFiles.add(ParsedLocalSong(file.name))
                    } else if (file.name.endsWith(".lrc")) {
                        try {
                            lyricsMap[file.name.removeSuffix(".lrc")] = file.readText()
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }
        }

        // Insert parsing results into database
        audioFiles.forEach { parsed ->
            val songId = parsed.youtubeId ?: return@forEach // Skip if not matching pattern
            
            val artistsList = parsed.artistName.split(", ").map { 
                com.zionhuang.music.models.MediaMetadata.Artist(id = "LA${it.hashCode()}", name = it)
            }
            
            val mediaMetadata = com.zionhuang.music.models.MediaMetadata(
                id = songId,
                title = parsed.title,
                artists = artistsList,
                duration = -1,
                thumbnailUrl = null,
                album = null
            )

            // Insert to database (upserting to avoid conflicts)
            database.query {
                insert(mediaMetadata) { it.copy(inLibrary = java.time.LocalDateTime.now()) }
            }
            
            // Check for lyrics
            val baseName = parsed.filename.removeSuffix(".m4a")
            val lyricsText = lyricsMap[baseName]
            if (lyricsText != null) {
                database.query {
                    upsert(LyricsEntity(id = songId, lyrics = lyricsText))
                }
            }
        }
    }
    
    private data class ParsedLocalSong(val filename: String) {
        val youtubeId: String?
        val artistName: String
        val title: String
        
        init {
            // Expected format: "[ID] Artist - Title.m4a"
            val regex = Regex("\\[(.*?)\\] (.*?) - (.*?)\\.m4a")
            val match = regex.find(filename)
            if (match != null) {
                youtubeId = match.groupValues[1]
                artistName = match.groupValues[2]
                title = match.groupValues[3]
            } else {
                youtubeId = null
                artistName = "Unknown"
                title = filename.removeSuffix(".m4a")
            }
        }
    }
}
