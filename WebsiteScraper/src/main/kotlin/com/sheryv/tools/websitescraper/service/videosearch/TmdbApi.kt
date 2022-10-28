package com.sheryv.tools.websitescraper.service.videosearch

import com.sheryv.tools.websitescraper.GlobalState
import com.sheryv.tools.websitescraper.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.websitescraper.utils.Utils
import com.sheryv.tools.websitescraper.utils.lg
import com.sheryv.util.HttpSupport
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class TmdbApi {
  
  fun searchTv(search: String): List<SearchItem> {
    val support = HttpSupport()
    val query = URLEncoder.encode(search, StandardCharsets.UTF_8)
    val settings = GlobalState.settingsForCurrentScraper() as StreamingWebsiteSettings
    require(!settings.tmdbKey.isNullOrBlank()) { "TMDB API key is required to use this feature" }
    val json = support.sendGet("https://api.themoviedb.org/3/search/tv?api_key=${settings.tmdbKey}&language=en-US&query=$query&page=1")
    val result: SearchResult = Utils.jsonMapper.readValue(json, SearchResult::class.java)
    val items = result.results.sortedByDescending { it.popularity }
    lg().info("Found {} items: {}", items.size, items.joinToString("\n", "\n", "\n") { it.toString() })
    return items
  }
  
  fun getTvEpisodes(id: Long, season: Int): TmdbSeason {
    val support = HttpSupport()
    val settings = GlobalState.settingsForCurrentScraper() as StreamingWebsiteSettings
    val json = support.sendGet("https://api.themoviedb.org/3/tv/$id/season/$season?api_key=${settings.tmdbKey}&language=en-US")
    return Utils.jsonMapper.readValue(json, TmdbSeason::class.java)
  }
}
