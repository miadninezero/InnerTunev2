package com.zionhuang.music.playback

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.zionhuang.music.db.MusicDatabase
import com.zionhuang.music.db.entities.ArtistEntity
import com.zionhuang.music.db.entities.LyricsEntity
import com.zionhuang.music.db.entities.SongArtistMap
import com.zionhuang.music.models.MediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LibraryScanner uses Android's Storage Access Framework (SAF) to scan a user-selected folder.
 * SAF bypasses all scoped storage restrictions — the user explicitly grants folder access via
 * the system folder picker, which works reliably across reinstalls and all Android versions.
 *
 * File naming convention: [YoutubeID] Artist - Title.m4a
 * YouTube IDs are exactly 11 alphanumeric characters (A-Z, a-z, 0-9, _, -)
 */
@Singleton
class LibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase
) {
    // Matches: [YouTubeID] Artist Name - Song Title.m4a
    private val FILE_PATTERN = Regex("""^\[([A-Za-z0-9_-]{11})\] (.+?) - (.+?)\.m4a$""")
    private val LRC_ID_PATTERN = Regex("""^\[([A-Za-z0-9_-]{11})\]""")

    /**
     * Scans the folder at [folderUri] (obtained via ACTION_OPEN_DOCUMENT_TREE).
     * Returns the number of newly imported songs.
     */
    suspend fun scanFolder(folderUri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(folderUri, flags)
        } catch (_: Exception) { }

        val rootDir = DocumentFile.fromTreeUri(context, folderUri)
            ?: return@withContext 0

        val allFiles = rootDir.listFiles()
        if (allFiles.isEmpty()) return@withContext 0

        // Separate .m4a and .lrc files
        val audioFiles = mutableListOf<Pair<ParsedLocalSong, DocumentFile>>()
        val lyricsMap  = mutableMapOf<String, DocumentFile>() // youtubeId -> lrc DocumentFile

        val resolver = context.contentResolver
        for (docFile in allFiles) {
            val name = docFile.name ?: continue
            when {
                name.endsWith(".m4a") || docFile.type == "audio/mp4" -> {
                    // Try parsing the name even if it misses the extension
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

        if (audioFiles.isEmpty()) return@withContext 0

        var importedCount = 0

        for ((parsed, _) in audioFiles) {
            val songId = parsed.youtubeId

            val meta = MediaMetadata(
                id           = songId,
                title        = parsed.title,
                artists      = parsed.artistNames.map { MediaMetadata.Artist(id = null, name = it) },
                duration     = -1,
                thumbnailUrl = null
            )

            database.transaction {
                // Returns -1L if song already in DB (IGNORE conflict strategy)
                val rowId = insert(meta.toSongEntity().copy(inLibrary = LocalDateTime.now()))
                if (rowId != -1L) {
                    parsed.artistNames.forEachIndexed { idx, name ->
                        val artist = ArtistEntity(
                            id   = ArtistEntity.generateArtistId(),
                            name = name
                        )
                        insert(artist)
                        insert(SongArtistMap(songId = songId, artistId = artist.id, position = idx))
                    }
                    importedCount++
                } else {
                    // The song was already in the database (e.g. from search history or cache).
                    // We just need to update its inLibrary status to make it show up in the Library tab.
                    inLibrary(songId, LocalDateTime.now())
                    importedCount++
                }
            }

            // Read lyrics via ContentResolver stream (SAF URI) and upsert
            val lrcFile = lyricsMap[songId]
            if (lrcFile != null) {
                try {
                    val lyricsText = resolver.openInputStream(lrcFile.uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    }
                    if (!lyricsText.isNullOrBlank()) {
                        database.query {
                            upsert(LyricsEntity(id = songId, lyrics = lyricsText))
                        }
                    }
                } catch (_: Exception) { }
            }
        }

        importedCount
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
