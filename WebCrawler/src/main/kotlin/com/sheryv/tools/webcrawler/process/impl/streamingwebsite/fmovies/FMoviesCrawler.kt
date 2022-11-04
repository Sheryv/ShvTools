package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.fmovies

import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.StreamingWebsiteBase
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.EpisodeAudioTypes
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.VideoData
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.VideoServer
import kotlinx.coroutines.delay
import org.openqa.selenium.By

class FMoviesCrawler(
  configuration: Configuration,
  browser: BrowserConfig,
  def: CrawlerDefinition<SeleniumDriver, StreamingWebsiteSettings>,
  driver: SeleniumDriver
) : StreamingWebsiteBase(configuration, browser, def, driver) {
  
  override suspend fun getMainLang() = "en"
  
  override suspend fun findEpisodeItems(serverIndex: String?): List<VideoData> {
    
    driver.waitForVisibility(By.cssSelector(".episodes .episode"), 5)
    
    val js =
      "return Array.from(document.querySelectorAll('.episodes .episode a')).map(a=>({u:a.href,t:a.getAttribute('data-kname'),e:a.querySelector('.name').textContent}))"
    
    return driver.executeScriptFetchList(js)?.filter { t ->
      val parts = t["t"].toString().split("-")
      parts.first().startsWith(series.season.toString()) && (parts[1].toIntOrNull() ?: 1) > 0
    }?.mapIndexed { i, t ->
      val name = t["e"].toString().trim { it <= ' ' }
      val link = t["u"] as String
      
      VideoData(link, name, i + 1)
    } ?: throw IllegalArgumentException("Cannot fetch list from ${series.seriesUrl}")
  }
  
  override suspend fun loadItemDataFromSummaryPageAndGetServers(data: VideoData): List<VideoServer> {
    wait(By.cssSelector("#servers .server"), 10)
    val servers = "return Array.from(document.querySelectorAll('#servers .server div')).map(a=>({h: a.textContent}))"
    
    return driver.executeScriptFetchList(servers)?.mapIndexed { i, map ->
      val name = map["h"]!!.toString().trim { it <= ' ' }.lowercase()
      
      VideoServer(name, i, EpisodeAudioTypes.UNKNOWN)
    } ?: emptyList()
  }
  
  override suspend fun <T> goToExternalServerVideoPage(data: VideoData, blockExecutedOnPage: (suspend () -> T)?): T? {
    
    val js = "document.querySelectorAll('#servers .server')[" + data.server.index + "].click()"
    driver.executeScript(js)
    
    delay(1000)
    
    val iframe = waitForAttributeCheckBy(By.cssSelector("#player iframe"), "src", 10) {
      it?.getAttribute("src")?.contains(data.server.matchedServerDef!!.domain()) == true
    }
    driver.switchTo().frame(iframe)
    return blockExecutedOnPage?.invoke()
  }
  
  override suspend fun checkForCaptchaAndOtherOverlays(data: VideoData) {
  
  }
}
