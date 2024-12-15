package com.sheryv.tools.webcrawler.config.impl.streamingwebsite

data class HistoryItem(val title: String, val url: String, val tmdbId: Long?, val seasonNumber: Int) {
  override fun toString() = "$title S${seasonNumber.toString().padStart(2, '0')} - $url"
}
