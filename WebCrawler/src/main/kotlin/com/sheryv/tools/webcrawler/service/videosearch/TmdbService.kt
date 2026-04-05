package com.sheryv.tools.webcrawler.service.videosearch

import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.DirectUrl
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Episode
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.M3U8Url
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Series
import java.time.LocalDate

class TmdbService(private val configuration: Configuration) {
  val api: TmdbApi = TmdbApi()
  
  fun searchSeriesBestMatch(current: Series, title: String, years: List<Int> = emptyList()): Series? {
    val item = api.searchTv(title)
      .firstOrNull { years.isEmpty() || it.firstAirDate?.let { LocalDate.parse(it) }?.year in years }
      ?: return null
    return updateSeriesFromTmdb(current, item)
  }
  
  fun updateSeriesFromTmdb(series: Series, item: SearchItem): Series {
    val updatedEpisodes = api.getTvEpisodes(item.id, series.season).episodes.map { ep: TmdbEpisode ->
      
      val found = series.episodes.firstOrNull { l -> l.number == ep.episodeNumber }
      if (found != null) {
        return@map found.copy(
          id = ep.id,
          title = ep.name,
          downloadUrl = found.url.takeIf { it.value.isNotBlank() }?.let {
            if (it.value == found.downloadUrl?.url) found.downloadUrl
            else if (it.value.contains(".m3u8")) M3U8Url(it.value)
            else DirectUrl(it.value)
          })
          .apply { lastSize.set(found.lastSize.value) }
      }
      Episode(ep.id, ep.name, ep.episodeNumber, null, "")
    }
    val imdb = api.getImdbIdForTv(item.id)
    
    return Series(
      item.id,
      item.name,
      series.season,
      series.lang,
      series.seriesUrl,
      item.posterUrl(),
      item.id.toString(),
      imdb,
      item.firstAirDate?.let { LocalDate.parse(it) } ?: series.releaseDate,
      updatedEpisodes,
    )
  }
}
