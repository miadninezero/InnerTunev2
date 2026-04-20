package com.zionhuang.music.playback

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.zionhuang.music.db.MusicDatabase
import com.zionhuang.music.db.entities.ArtistEntity
import com.zionhuang.music.db.entities.LyricsEntity
import com.zionhuang.music.db.entities.SongArtistMap
import com.zionhuang.music.models.MediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil
) {
    private val FILE_PATTERN = Regex("""^\[([A-Za-z0-9_-]{11})\] (.+?) - (.+?)\.m4a$""")
    private val LRC_ID_PATTERN = Regex("""^\[([A-Za-z0-9_-]{11})\]""")

    suspend fun scanFolder(folderUri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(folderUri, flags)
        } catch (_: Exception) { }

        val rootDir = DocumentFile.fromTreeUri(context, folderUri)
            ?: return@withContext 0

        val allFiles = rootDir.listFiles()
        if (allFiles.isEmpty()) return@withContext 0

        val audioFiles = mutableListOf<Pair<ParsedLocalSong, DocumentFile>>()
        val lyricsMap  = mutableMapOf<String, DocumentFile>()

        for (docFile in allFiles) {
            val name = docFile.name ?: continue
            when {
                name.endsWith(".m4a") || docFile.type == "audio/mp4" -> {
                    val nameToParse = if (!name.endsWith(".m4a")) "$name.m4a" else name
                    val parsed = parseFilename(nameToParse)
                    if (parsed != null) {
                        audioFiles.add(parsed to docFile)
                    }
                }
                name.endsWith(".lrc") -> {
                    val ytId = LRC_ID_PATTERN.find(name)?.groupValues?.get(1) ?: continue
                    lyricsMap[ytId] = docFile
                }
            }
        }

        var importedCount = 0

        for ((parsed, _) in audioFiles) {
            val songId = parsed.youtubeId

            val lrcDoc = lyricsMap[songId]
            var lyricsStr: String? = null
            if (lrcDoc != null) {
                try {
                    context.contentResolver.openInputStream(lrcDoc.uri)?.use { stream ->
                        lyricsStr = stream.bufferedReader().readText()
                    }
                } catch (_: Exception) { }
            }

            val meta = MediaMetadata(
                id = songId,
                title = parsed.title,
                artists = parsed.artistNames.map { MediaMetadata.Artist(id = null, name = it) },
                duration = -1,
                thumbnailUrl = null
            )

            // Insert or update the song in the library database
            val rowId = database.insert(meta.toSongEntity().copy(inLibrary = LocalDateTime.now()))
            if (rowId != -1L) {
                parsed.artistNames.forEachIndexed { idx, name ->
                    val artist = ArtistEntity(
                        id   = ArtistEntity.generateArtistId(),
                        name = name
                    )
                    database.insert(artist)
                    database.insert(SongArtistMap(songId = songId, artistId = artist.id, position = idx))
                }
                importedCount++
            } else {
                database.inLibrary(songId, LocalDateTime.now())
                importedCount++
            }

            // Insert lyrics if found
            if (lyricsStr != null) {
                database.upsert(LyricsEntity(id = songId, lyrics = lyricsStr!!))
            }

            // Register in ExoPlayer's download index so songs appear in the "Downloaded" filter.
            // We check if the song is already registered to avoid duplicates.
            val existingDownload = downloadUtil.downloads.first()[songId]
            if (existingDownload == null || existingDownload.state != Download.STATE_COMPLETED) {
                val downloadRequest = DownloadRequest.Builder(songId, songId.toUri())
                    .setCustomCacheKey(songId)
                    .setData(parsed.title.toByteArray())
                    .build()
                DownloadService.sendAddDownload(
                    context,
                    ExoDownloadService::class.java,
                    downloadRequest,
                    false
                )
            }
        }

        return@withContext importedCount
    }

    private fun parseFilename(filename: String): ParsedLocalSong? {
        val match = FILE_PATTERN.find(filename) ?: return null
        return ParsedLocalSong(
            youtubeId   = match.groupValues[1],
            artistNames = match.groupValues[2].split(", ").map { it.trim() }.filter { it.isNotEmpty() },
            title       = match.groupValues[3]
        )
    }

    private data class ParsedLocalSong(
        val youtubeId:   String,
        val artistNames: List<String>,
        val title:       String
    )
}
