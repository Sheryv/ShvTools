package com.sheryv.tools.webcrawler.service.videosearch

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.service.SystemSupport
import com.sheryv.util.io.HttpSupport
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.logging.log

class TmdbApi {
  
  fun searchTv(search: String): List<SearchItem> {
    val support = HttpSupport()
    val query = SystemSupport.get.encodeNameForWeb(search)
    val settings = GlobalState.settingsForCurrentScraper() as StreamingWebsiteSettings
    require(!settings.tmdbKey.isNullOrBlank()) { "TMDB API key is required to use this feature" }
    val json = support.sendString("https://api.themoviedb.org/3/search/tv?api_key=${settings.tmdbKey}&language=en-US&query=$query&page=1")
    val result: SearchResult = SerialisationUtils.fromJson(json)
    val items = result.results.sortedByDescending { it.popularity }
    log.info("Found {} items: {}", items.size, items.joinToString("\n", "\n", "\n") { it.toString() })
    return items
  }
  
  fun getTvSeries(id: Long): SearchItem {
    val support = HttpSupport()
    val settings = GlobalState.settingsForCurrentScraper() as StreamingWebsiteSettings
    val json = support.sendString("https://api.themoviedb.org/3/tv/$id?api_key=${settings.tmdbKey}&language=en-US&append_to_response=external_ids")
    return SerialisationUtils.fromJson(json)
  }
  
  fun getTvEpisodes(id: Long, season: Int): TmdbSeason {
    val support = HttpSupport()
    val settings = GlobalState.settingsForCurrentScraper() as StreamingWebsiteSettings
    val json = support.sendString("https://api.themoviedb.org/3/tv/$id/season/$season?api_key=${settings.tmdbKey}&language=en-US")
    return SerialisationUtils.fromJson(json)
  }
  
  fun getImdbId(id: Long): String {
    val support = HttpSupport()
    val settings = GlobalState.settingsForCurrentScraper() as StreamingWebsiteSettings
    val json = support.sendString("https://api.themoviedb.org/3/tv/$id/external_ids?api_key=${settings.tmdbKey}&language=en-US")
    return SerialisationUtils.fromJson<Map<*, *>>(json)["imdb_id"].toString()
  }
}
