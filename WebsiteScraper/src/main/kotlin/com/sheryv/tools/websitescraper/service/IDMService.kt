package com.sheryv.tools.websitescraper.service

import com.sheryv.tools.websitescraper.GlobalState
import com.sheryv.tools.websitescraper.config.Configuration
import com.sheryv.tools.websitescraper.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.model.Episode
import com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.model.Series
import com.sheryv.tools.websitescraper.utils.lg
import com.sheryv.util.FileUtils
import kotlinx.coroutines.delay

class IDMService(val configuration: Configuration) {
  
  suspend fun addToIDM(series: Series): Pair<Int, Int> {
    val settings = GlobalState.currentScrapper.findSettings(configuration) as StreamingWebsiteSettings
    var c = 0
    val episodes = series.episodes.filter { settings.searchStartIndex <= it.number && it.number <= settings.searchStopIndex }
    for (episode in episodes) {
      if (episode.errors.isEmpty()) {
        addSingle(series, episode, settings)
        c++
        delay(1000)
      } else {
        lg().info("No download link for $episode")
      }
    }
    return c to episodes.size
  }
  
  private fun addSingle(series: Series, episode: Episode, settings: StreamingWebsiteSettings) {
    val idmExePath: String = settings.idmExePath!!
    require(idmExePath.isNotBlank()) { "Path to IDM exe cannot be empty" }
    val ex = String.format(
      "\"%s\" /n /f \"%s\" /p \"" + settings.downloadDir + "\\%s\"" +
          " /a /d %s",
      idmExePath,
      episode.generateFileName(series, "mp4", settings),
      FileUtils.fixFileNameWithCollonSupport(String.format("%s %02d", series.title, series.season)),
      episode.downloadUrl
    )
    try {
      lg().info("\n> IDM {} {}", episode.number, ex)
      Runtime.getRuntime().exec(ex)
    } catch (e: Exception) {
      lg().error("Error while adding to IDM", e)
    }
  }
}
