package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common

import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.config.impl.streamingwebsite.HistoryItem
import com.sheryv.tools.webcrawler.process.base.CrawlerAttributes
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.Groups
import com.sheryv.tools.webcrawler.process.base.event.FetchedDataExternalChangeEvent
import com.sheryv.tools.webcrawler.process.base.event.FetchedDataStatusChangedEvent
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Episode
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Series
import com.sheryv.tools.webcrawler.service.SystemSupport
import com.sheryv.tools.webcrawler.service.videosearch.TmdbApi
import com.sheryv.util.DateUtils
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.emitEvent
import com.sheryv.util.event.AsyncEvent
import com.sheryv.util.inBackground
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter

abstract class StreamingCrawlerBase(
  id: String,
  name: String,
  websiteUrl: String
) : CrawlerDefinition<SeleniumDriver, StreamingWebsiteSettings>(
  CrawlerAttributes(id, name, websiteUrl, Groups.STREAMING_WEBSITE),
  StreamingWebsiteSettings::class.java
) {
  
  override fun handleEvent(e: AsyncEvent) {
    when (e) {
      is FetchedDataExternalChangeEvent -> onFetchedDataExternalChangeEvent(e)
    }
  }
  
  override fun createDefaultSettings(): StreamingWebsiteSettings {
    return StreamingWebsiteSettings(
      id(),
      outputPath = defaultOutputPath(),
      downloadDir = SystemSupport.get.userDownloadDir.resolve(SystemSupport.get.removeForbiddenFileChars(attributes.id)).toString()
    )
  }
  
  fun onLoadFromHistory(history: HistoryItem) {
    val config = Configuration.get()
    val settings = findSettings(config)
    synchronized(StreamingCrawlerBase::class.java) {
      config.updateSettings(
        settings.copy(
          seriesUrl = history.url,
          seasonNumber = history.seasonNumber,
          seriesName = history.title,
        )
      ).save()
    }
    
    history.tmdbId?.also {
      inBackground {
        synchronized(StreamingCrawlerBase::class.java) {
          val api = TmdbApi()
          val foundSeries = api.getTvSeries(it)
          val season = api.getTvEpisodes(it, history.seasonNumber)
          val series = Series(
            season.id,
            foundSeries.name,
            season.season,
            "",
            history.url,
            foundSeries.posterUrl(),
            season.id.toString(),
            foundSeries.imdbId(),
            foundSeries.firstAirDate?.let { LocalDate.parse(it) },
            season.episodes.map { ep ->
              Episode(ep.id, ep.name, ep.episodeNumber, null, "")
            },
          )
          SerialisationUtils.jsonMapper.writeValue(settings.outputPath.toFile(), series)
          emitEvent(FetchedDataExternalChangeEvent())
        }
      }
    }
  }
  
  protected fun onFetchedDataExternalChangeEvent(e: FetchedDataExternalChangeEvent) {
    val path = findSettings(Configuration.get()).outputPath
    if (!Files.exists(path)) {
      emitEvent(FetchedDataStatusChangedEvent(""))
      return
    }
    val series = SerialisationUtils.jsonMapper.readValue(path.toFile(), Series::class.java)
    emitEvent(FetchedDataStatusChangedEvent(series.formattedString()))
  }
}
