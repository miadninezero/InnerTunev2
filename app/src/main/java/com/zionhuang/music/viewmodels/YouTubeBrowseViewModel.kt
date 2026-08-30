package com.zionhuang.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.AlbumItem
import com.zionhuang.innertube.models.ArtistItem
import com.zionhuang.innertube.models.PlaylistItem
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.YTItem
import com.zionhuang.innertube.pages.BrowseResult
import com.zionhuang.music.constants.HideExplicitKey
import com.zionhuang.music.utils.dataStore
import com.zionhuang.music.utils.get
import com.zionhuang.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YouTubeBrowseViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val browseId = savedStateHandle.get<String>("browseId")!!
    private val params = savedStateHandle.get<String>("params")

    val result = MutableStateFlow<BrowseResult?>(null)

    private fun isExcludedContent(text: String?): Boolean {
        if (text == null) return false
        val lower = text.lowercase().trim()
        return lower.contains("community playlist") ||
                lower.contains("trending community") ||
                lower.contains("today's") ||
                lower.contains("todays") ||
                lower.contains("today") ||
                lower.contains("ghazal") ||
                lower.contains("sufi") ||
                lower.contains("indian")
    }

    private fun isExcludedItem(item: YTItem): Boolean {
        return when (item) {
            is SongItem -> isExcludedContent(item.title) || item.artists.any { isExcludedContent(it.name) } || isExcludedContent(item.album?.name)
            is AlbumItem -> isExcludedContent(item.title) || item.artists.orEmpty().any { isExcludedContent(it.name) }
            is ArtistItem -> isExcludedContent(item.title)
            is PlaylistItem -> isExcludedContent(item.title) || isExcludedContent(item.author?.name)
        }
    }

    init {
        viewModelScope.launch {
            YouTube.browse(browseId, params).onSuccess { rawResult ->
                val explicitFiltered = rawResult.filterExplicit(context.dataStore.get(HideExplicitKey, false))
                val filteredItems = explicitFiltered.items.mapNotNull { itemSection ->
                    if (isExcludedContent(itemSection.title)) return@mapNotNull null
                    val validItems = itemSection.items.filterNot(::isExcludedItem)
                    if (validItems.isNotEmpty()) {
                        itemSection.copy(items = validItems)
                    } else null
                }
                result.value = explicitFiltered.copy(items = filteredItems)
            }.onFailure {
                reportException(it)
            }
        }
    }
}
