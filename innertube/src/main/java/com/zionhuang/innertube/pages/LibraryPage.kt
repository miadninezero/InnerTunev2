package com.zionhuang.innertube.pages

import com.zionhuang.innertube.models.MusicResponsiveListItemRenderer

data class LibraryPage(
    val items: List<MusicResponsiveListItemRenderer>,
    val continuation: String?,
)
