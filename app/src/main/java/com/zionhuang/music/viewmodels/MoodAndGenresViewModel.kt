package com.zionhuang.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.pages.MoodAndGenres
import com.zionhuang.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoodAndGenresViewModel @Inject constructor() : ViewModel() {
    val moodAndGenres = MutableStateFlow<List<MoodAndGenres>?>(null)

    private fun isExcluded(title: String?): Boolean {
        if (title == null) return false
        val lower = title.lowercase().trim()
        return lower.contains("community playlist") ||
                lower.contains("trending community") ||
                lower.contains("today's") ||
                lower.contains("todays") ||
                lower.contains("today") ||
                lower.contains("ghazal") ||
                lower.contains("sufi") ||
                lower.contains("indian")
    }

    init {
        viewModelScope.launch {
            YouTube.moodAndGenres().onSuccess { list ->
                moodAndGenres.value = list.mapNotNull { category ->
                    if (isExcluded(category.title)) return@mapNotNull null
                    val filteredItems = category.items.filterNot { item ->
                        isExcluded(item.title)
                    }
                    if (filteredItems.isNotEmpty()) {
                        category.copy(items = filteredItems)
                    } else null
                }
            }.onFailure {
                reportException(it)
            }
        }
    }
}
