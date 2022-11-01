package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.zerion

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.ProcessingStates
import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.ScraperDefinition
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.StreamingWebsiteBase
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions

class ZerionScraper(
  configuration: Configuration,
  browser: BrowserConfig,
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
      
      VideoServer(name, i, EpisodeAudioTypes.UNKNOWN, VideoServerFormat(format))
    } ?: emptyList()
  }
  
  override suspend fun <T> goToExternalServerVideoPage(data: VideoData, blockExecutedOnPage: (suspend () -> T)?): T? {
    val js = "document.querySelectorAll('.video-list tr .btn.watch-btn')[" + data.server.index + "].click()"
    driver.executeScript(js)
    return blockExecutedOnPage?.invoke()
  }
  
  override suspend fun checkForCaptchaAndOtherOverlays(data: VideoData) {
    val captacha = wait(By.cssSelector(".hcaptcha"))
    if (captacha != null) {
      GlobalState.processingState.set(ProcessingStates.PAUSED)
      runBlocking(Dispatchers.Main) {
        GlobalState.view.showMessageDialog("Captcha detected! Solve it and resume process.")
      }
      waitIfPaused()
    }
  }
  
}
