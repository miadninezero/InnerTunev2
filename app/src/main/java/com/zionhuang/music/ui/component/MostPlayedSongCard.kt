package com.zionhuang.music.ui.component

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zionhuang.music.constants.GridThumbnailHeight
import com.zionhuang.music.constants.ThumbnailCornerRadius
import com.zionhuang.music.data.local.SongPlaySummary
import com.zionhuang.music.db.entities.Song
import com.zionhuang.music.utils.makeTimeString

/**
 * A compact grid-style card used in the "Most Played" horizontal row on the
 * Home screen.
 *
 * When [song] is non-null (i.e. the track exists in the local MusicDatabase),
 * we use the richer metadata (thumbnail, duration, liked badge) from the full
 * [Song]. When it is null (interaction was logged but the song hasn't been
 * added to the library), we fall back to the [SongPlaySummary] data so the
 * card is always populated.
 *
 * An animated [PlayingIndicator] overlay is shown whenever [isActive] &&
 * [isPlaying] — matching the behaviour of all other item cards in the app.
 */
@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
fun MostPlayedSongCard(
    summary: SongPlaySummary,
    song: Song?,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbnailUrl = song?.song?.thumbnailUrl
    val title = song?.song?.title ?: summary.title
    val artist = song?.artists?.joinToString { it.name } ?: summary.artist
    val durationMs = summary.totalMs

    Column(
        modifier = modifier
            .width(GridThumbnailHeight)            // square card, same width as other grid items
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
    ) {
        // Thumbnail with playing indicator overlay
        ItemThumbnail(
            thumbnailUrl = thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = RoundedCornerShape(ThumbnailCornerRadius),
            modifier = Modifier.size(GridThumbnailHeight)
        )

        Spacer(Modifier.height(6.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        // Artist · total play time
        Text(
            text = buildString {
                if (artist.isNotBlank()) {
                    append(artist)
                    append(" · ")
                }
                append(makeTimeString(durationMs))
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
