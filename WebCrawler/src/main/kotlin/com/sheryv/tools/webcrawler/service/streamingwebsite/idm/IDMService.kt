package com.sheryv.tools.webcrawler.service.streamingwebsite.idm

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Episode
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Series
import com.sheryv.tools.webcrawler.utils.lg
import com.sheryv.util.FileUtils
import kotlinx.coroutines.delay

class IDMService(val configuration: Configuration) {
  
  suspend fun addToIDM(series: Series): Pair<Int, Int> {
    val settings = GlobalState.currentCrawler.findSettings(configuration) as StreamingWebsiteSettings
    var c = 0
    val episodes = series.episodes.filter { settings.searchStartIndex <= it.number && it.number <= settings.searchStopIndex }
    for (episode in episodes) {
      if (episode.errors.isEmpty() && episode.downloadUrl?.isDirect == true) {
        addSingle(series, episode, settings)
        c++
        delay(1000)
      } else {
        lg().info("No download link for $episode")
      }
    }
    return c to episodes.size
  }
  
  fun addSingle(series: Series, episode: Episode, settings: StreamingWebsiteSettings) {
    val idmExePath: String = settings.idmExePath!!
    require(idmExePath.isNotBlank()) { "Path to IDM exe cannot be empty" }
    require(episode.downloadUrl?.isDirect == true) { "Only direct download urls are supported by IDM" }
    val ex = String.format(
      "\"%s\" /n /f \"%s\" /p \"%s\\%s\"" +
          " /a /d %s",
      idmExePath,
      episode.generateFileName(series, settings),
      settings.downloadDir,
      FileUtils.fixFileNameWithCollonSupport(String.format("%s %02d", series.title, series.season)),
      episode.downloadUrl!!.base
    )
    
    try {
      lg().info("\n> IDM {} {}", episode.number, ex)
      Runtime.getRuntime().exec(ex)
    } catch (e: Exception) {
      lg().error("Error while adding to IDM", e)
    }
  }
}
