package com.sheryv.tools.webcrawler.service.videosearch

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.service.SystemSupport
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.io.HttpSupport
import com.sheryv.util.logging.log

class TmdbApi(val key: String? = null) {
  
  private fun findKey(): String {
    if (key == null) {
      val settings = GlobalState.settingsForCurrentScraper() as StreamingWebsiteSettings
      require(!settings.tmdbKey.isNullOrBlank()) { "TMDB API key is required to use this feature" }
      return settings.tmdbKey!!
    }
    return key
  }
  
  fun searchTv(search: String, year: Int? = null): List<SearchItem> {
    val support = HttpSupport()
    val query = SystemSupport.get.encodeNameForWeb(search)
    val json = support.sendString(
      "https://api.themoviedb.org/3/search/tv?api_key=${findKey()}&language=en-US&query=$query&page=1${
        year?.let { "&first_air_date_year=$it" }.orEmpty()
      }"
    )
    val result: SearchResult<SearchItem> = SerialisationUtils.fromJson(json)
    val items = result.results
    log.info(
      "Found {} items for {}{}: {}",
      items.size,
      search,
      year?.let { " [$it]" }.orEmpty(),
      items.joinToString("\n", "\n", "\n") { it.toString() })
    return items
  }
  
  fun searchMovie(search: String, year: Int? = null): List<MovieSearchItem> {
    val support = HttpSupport()
    val query = SystemSupport.get.encodeNameForWeb(search)
    val json = support.sendString(
      "https://api.themoviedb.org/3/search/movie?api_key=${findKey()}&language=en-US&query=$query&page=1${
        year?.let { "&primary_release_year=$it" }.orEmpty()
      }"
    )
    val result: SearchResult<MovieSearchItem> = SerialisationUtils.fromJson(json)
    val items = result.results
    log.info(
      "Found {} items for {}{}: {}",
      items.size,
      search,
      year?.let { " [$it]" }.orEmpty(),
      items.joinToString("\n", "\n", "\n") { it.toString() })
    return items
  }
  
  fun getTvSeries(id: Long): SearchItem {
    val support = HttpSupport()
    val json = support.sendString("https://api.themoviedb.org/3/tv/$id?api_key=${findKey()}&language=en-US&append_to_response=external_ids")
    return SerialisationUtils.fromJson(json)
  }
  
  fun getTvEpisodes(id: Long, season: Int): TmdbSeason {
    val support = HttpSupport()
    val json = support.sendString("https://api.themoviedb.org/3/tv/$id/season/$season?api_key=${findKey()}&language=en-US")
    return SerialisationUtils.fromJson(json)
  }
  
  fun getImdbIdForTv(id: Long): String {
    val support = HttpSupport()
    val json = support.sendString("https://api.themoviedb.org/3/tv/$id/external_ids?api_key=${findKey()}&language=en-US")
    return SerialisationUtils.fromJson<Map<*, *>>(json)["imdb_id"].toString()
  }
  
  fun getImdbIdForMovie(id: Long): String? {
    val support = HttpSupport()
    val json = support.sendString("https://api.themoviedb.org/3/movie/$id/external_ids?api_key=${findKey()}")
    return SerialisationUtils.fromJson<Map<*, *>>(json)["imdb_id"]?.toString()
  }
  
  fun getEpisodeId(showId: Long, season: Int, episode: Int): String? {
    val json = HttpSupport().sendString("https://api.themoviedb.org/3/tv/$showId/season/$season/episode/$episode/external_ids?api_key=${findKey()}")
    return SerialisationUtils.fromJson<Map<*, *>>(json)["imdb_id"]?.toString()
  }
}
