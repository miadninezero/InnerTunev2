package com.zionhuang.music.models

import androidx.compose.runtime.Immutable
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.music.db.entities.*
import com.zionhuang.music.ui.utils.resize
import java.io.Serializable

@Immutable
data class MediaMetadata(
    val id: String,
    val title: String,
    val artists: List<Artist>,
    val duration: Int,
    val thumbnailUrl: String? = null,
    val album: Album? = null,
    val explicit: Boolean = false,
) : Serializable {
    data class Artist(
        val id: String?,
        val name: String,
    ) : Serializable

    data class Album(
        val id: String,
        val title: String,
    ) : Serializable

    fun toSongEntity() = SongEntity(
        id = id,
        title = title,
        duration = duration,
        thumbnailUrl = thumbnailUrl,
        albumId = album?.id,
        albumName = album?.title
    )
}

fun Song.toMediaMetadata() = MediaMetadata(
    id = song.id,
    title = song.title,
    artists = artists.map {
        MediaMetadata.Artist(
            id = it.id,
            name = it.name
        )
    },
    duration = song.duration,
    thumbnailUrl = song.thumbnailUrl,
    album = album?.let {
        MediaMetadata.Album(
            id = it.id,
            title = it.title
        )
    } ?: song.albumId?.let { albumId ->
        MediaMetadata.Album(
            id = albumId,
            title = song.albumName.orEmpty()
        )
    }
)

fun SongItem.toMediaMetadata() = MediaMetadata(
    id = id,
    title = title,
    artists = artists.map {
        MediaMetadata.Artist(
            id = it.id,
            name = it.name
        )
    },
    duration = duration ?: -1,
    thumbnailUrl = thumbnail.resize(544, 544),
    album = album?.let {
        MediaMetadata.Album(
            id = it.id,
            title = it.name
        )
    },
    explicit = explicit
)

fun Song.toSongItem() = SongItem(
    id = song.id,
    title = song.title,
    artists = artists.map { com.zionhuang.innertube.models.Artist(name = it.name, id = it.id) },
    album = album?.let { com.zionhuang.innertube.models.Album(name = it.title, id = it.id) }
        ?: song.albumId?.let { com.zionhuang.innertube.models.Album(name = song.albumName.orEmpty(), id = it) },
    duration = song.duration.takeIf { it > 0 },
    thumbnail = song.thumbnailUrl.orEmpty(),
    explicit = false
)

fun SongItem.toSong() = Song(
    song = SongEntity(
        id = id,
        title = title,
        duration = duration ?: -1,
        thumbnailUrl = thumbnail,
        albumId = album?.id,
        albumName = album?.name
    ),
    artists = artists.map { ArtistEntity(id = it.id ?: ArtistEntity.generateArtistId(), name = it.name) },
    album = album?.let { AlbumEntity(id = it.id, title = it.name, songCount = 0, duration = 0) }
)

fun MediaMetadata.toSong() = Song(
    song = toSongEntity(),
    artists = artists.map { ArtistEntity(id = it.id ?: ArtistEntity.generateArtistId(), name = it.name) },
    album = album?.let { AlbumEntity(id = it.id, title = it.title, songCount = 0, duration = 0) }
)


