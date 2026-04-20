package com.zionhuang.music.playback

import android.content.Context
import android.os.Environment
import com.zionhuang.music.db.MusicDatabase
import com.zionhuang.music.db.entities.ArtistEntity
import com.zionhuang.music.db.entities.LyricsEntity
import com.zionhuang.music.db.entities.SongArtistMap
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
    // Matches: [YouTubeID] Artist Name - Song Title.m4a
    // YouTube IDs are exactly 11 chars: letters, digits, _ and -
    private val FILE_PATTERN = Regex("""^\[([A-Za-z0-9_-]{11})\] (.+?) - (.+?)\.m4a$""")
    private val LRC_PATTERN  = Regex("""^\[([A-Za-z0-9_-]{11})\]""")

    suspend fun scanDownloadsFolder(): Int = withContext(Dispatchers.IO) {
        // ──────────────────────────────────────────────────────────────────
        // Use the File API directly on the public Downloads directory.
        // This is the ONLY reliable method after a reinstall because
        // MediaStore.Downloads only returns files owned by the current
        // app UID — files written by a previous install become invisible
        // to MediaStore queries even though they physically still exist.
        // ──────────────────────────────────────────────────────────────────
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "InnerTune"
        )

        if (!dir.exists() || !dir.isDirectory) return@withContext 0

        val allFiles = dir.listFiles() ?: return@withContext 0

        val audioFiles = mutableListOf<ParsedLocalSong>()
        val lyricsMap  = mutableMapOf<String, String>() // youtubeId -> lrc text

        for (file in allFiles) {
            when {
                file.name.endsWith(".m4a") -> {
                    val parsed = parseFilename(file.name)
                    if (parsed != null) audioFiles.add(parsed)
                }
                file.name.endsWith(".lrc") -> {
                    val ytId = LRC_PATTERN.find(file.name)?.groupValues?.get(1) ?: continue
                    runCatching { lyricsMap[ytId] = file.readText() }
                }
            }
        }

        if (audioFiles.isEmpty()) return@withContext 0

        // ── Insert each discovered song into the database ──────────────────
        var importedCount = 0
        for (parsed in audioFiles) {
            val songId = parsed.youtubeId

            val artists = parsed.artistNames.mapIndexed { index, name ->
                Triple(
                    ArtistEntity(id = ArtistEntity.generateArtistId(), name = name),
                    index,
                    songId
                )
            }

            // transaction{} is the correct wrapper for @Transaction DAO methods
            database.transaction {
                // toSongEntity() builds a SongEntity; copy inLibrary so it shows in Library tab
                val meta = com.zionhuang.music.models.MediaMetadata(
                    id = songId,
                    title = parsed.title,
                    artists = parsed.artistNames.map {
                        com.zionhuang.music.models.MediaMetadata.Artist(id = null, name = it)
                    },
                    duration = -1,
                    thumbnailUrl = null
                )
                val rowId = insert(meta.toSongEntity().copy(inLibrary = LocalDateTime.now()))
                if (rowId != -1L) {
                    // New song — insert artist entities and mapping rows
                    artists.forEachIndexed { idx, (artistEntity, _, _) ->
                        insert(artistEntity)
                        insert(
                            SongArtistMap(
                                songId = songId,
                                artistId = artistEntity.id,
                                position = idx
                            )
                        )
                    }
                    importedCount++
                }
            }

            // Lyrics can be inserted separately (idempotent upsert)
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
        val youtubeId  = match.groupValues[1]
        val artistStr  = match.groupValues[2]
        val title      = match.groupValues[3]
        val artistNames = artistStr.split(", ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return ParsedLocalSong(youtubeId = youtubeId, artistNames = artistNames, title = title)
    }

    private data class ParsedLocalSong(
        val youtubeId:   String,
        val artistNames: List<String>,
        val title:       String
    )
}
