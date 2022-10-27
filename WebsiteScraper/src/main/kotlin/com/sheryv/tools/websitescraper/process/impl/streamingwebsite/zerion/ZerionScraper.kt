package com.sheryv.tools.websitescraper.process.impl.streamingwebsite.zerion

import com.sheryv.tools.websitescraper.GlobalState
import com.sheryv.tools.websitescraper.browser.BrowserDef
import com.sheryv.tools.websitescraper.config.Configuration
import com.sheryv.tools.websitescraper.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.websitescraper.process.base.ScraperDefinition
import com.sheryv.tools.websitescraper.process.base.model.SeleniumDriver
import com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.CommonVideoServers
import com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.StreamingWebsiteBase
import com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.model.EpisodeFormat
import com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.model.EpisodeTypes
import com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.model.VideoData
import com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.model.VideoServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions

class ZerionScraper(
  configuration: Configuration,
  browser: BrowserDef,
  def: ScraperDefinition<SeleniumDriver, StreamingWebsiteSettings>,
  driver: SeleniumDriver
) : StreamingWebsiteBase(configuration, browser, def, driver) {
  
  override suspend fun getMainLang() = "pl"
  
  override suspend fun findEpisodeItems(serverIndex: String?): List<VideoData> {
    wait.until<List<WebElement>>(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("#series-page .season")))
    val js =
      "return [...document.querySelector('#series-page .season:nth-child(${settings.seasonNumber}) > ul').childNodes.values()]" +
          ".map(v=>v.querySelector('.title-date-block a'))" +
          ".filter(v=>!!v)" +
          ".map(v=>{return {e: v.childNodes[0].textContent, u:v.attributes['href'].value}});"
    
    return driver.executeScriptFetchList(js)?.mapIndexed { i, t ->
      val name = t["e"].toString().trim { it <= ' ' }.replace(Regex("""[sS]\d+[eE]\d+"""), "").trim()
      val link = t["u"] as String
      
      VideoData(link, name, i + 1)
    } ?: throw IllegalArgumentException("Cannot fetch list from ${series.seriesUrl}")
  }
  
  override suspend fun loadItemDataFromSummaryPageAndGetServers(data: VideoData): List<VideoServer> {
    val js =
      "return [...document.querySelectorAll('.video-list tr')]" +
          ".filter(v=>v.childNodes[0].nodeName == 'TD')" +
          ".map(v=>{return {h: v.childNodes[0].textContent, q: v.childNodes[1].textContent}})"
    
    return driver.executeScriptFetchList(js)?.mapIndexed { i, map ->
      val name = map["h"]!!.toString().trim { it <= ' ' }.lowercase()
      val format = map["q"]!!.toString().trim { it <= ' ' }.lowercase()
      
      VideoServer(name, i, EpisodeTypes.UNKNOWN, EpisodeFormat(null, format))
    } ?: emptyList()
  }
  
  override suspend fun goToExternalServerVideoPage(data: VideoData) {
    val js = "document.querySelectorAll('.video-list tr .btn.watch-btn')[" + data.server.index + "].click()"
    driver.executeScript(js)
  }
  
  override suspend fun findLoadedVideoDownloadUrl(data: VideoData): String? {
    delay(200)
  
    val serverHandler = streamingServersHandlers()[CommonVideoServers.forName(data.server.serverName)]!!
    val found = serverHandler.findVideoSrcUrl()
    if (found == null) {
      runBlocking(Dispatchers.Main) {
        GlobalState.view.showMessageDialog("Cannot find URL. Video not started?")
      }
    }
  
    return serverHandler.findVideoSrcUrl()
  }
  
}
