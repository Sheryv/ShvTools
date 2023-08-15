package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.filman

import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.model.ProcessParams
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.StreamingWebsiteBase
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.EpisodeAudioTypes
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.VideoData
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.VideoServer
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.VideoServerFormat
import kotlinx.coroutines.delay
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions

class FilmanCrawler(
  configuration: Configuration,
  browser: BrowserConfig,
  def: CrawlerDefinition<SeleniumDriver, StreamingWebsiteSettings>,
  driver: SeleniumDriver,
  params: ProcessParams
) : StreamingWebsiteBase(configuration, browser, def, driver, params) {
  
  override suspend fun getMainLang() = "pl"
  
  override suspend fun findEpisodeItems(serverIndex: String?): List<VideoData> {
    wait.until<List<WebElement>>(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("#episode-list")))
    val js =
      "return \$('#episode-list > li:nth(-${settings.seasonNumber}) > ul a').get().map(n=>({e: n.textContent, u:n.getAttribute('href')}))"
    
    val incorrectEpisodesRegex = Regex(""".*[eE](\d{4,}|99\d+).*""")
    val fixNameRegex = Regex("""\[[sS]\d+[eE]\d+]""")
    
    return driver.executeScriptFetchList(js)?.reversed()?.mapIndexedNotNull { i, t ->
      val nameRaw = t["e"].toString()
      if (nameRaw.matches(incorrectEpisodesRegex)) {
        return@mapIndexedNotNull null
      }
      val name = nameRaw.trim { it <= ' ' }.replace(fixNameRegex, "").trim()
      val link = t["u"] as String
      
      VideoData(link, name, i + 1)
    } ?: throw IllegalArgumentException("Cannot fetch list from ${series.seriesUrl}")
  }
  
  override suspend fun loadItemDataFromSummaryPageAndGetServers(data: VideoData): List<VideoServer> {
    val js = "return \$('#links > tbody > tr').get().filter(n => n.children.length > 0).map(n => ({a: n.children[1].textContent, " +
        "q: n.children[2].textContent, h: n.children[0].children[0].children[0].getAttribute('alt'), d: n.children[0].innerText}))"
    
    return driver.executeScriptFetchList(js)?.mapIndexed { i, map ->
      val name = map["h"]!!.toString().trim { it <= ' ' }.lowercase()
      val format = map["q"]!!.toString().trim { it <= ' ' }.lowercase()
      val audio = when (map["a"].toString().lowercase()) {
        "lektor" -> EpisodeAudioTypes.LECTOR
        "napisy" -> EpisodeAudioTypes.SUBS
        "dubbing" -> EpisodeAudioTypes.DUBBING
        "napisy_tansl", "eng" -> EpisodeAudioTypes.ORIGIN
        else -> EpisodeAudioTypes.UNKNOWN
      }
      
      VideoServer(name, i, audio, VideoServerFormat(format))
    } ?: emptyList()
  }
  
  override suspend fun <T> goToExternalServerVideoPage(data: VideoData, blockExecutedOnPage: (suspend () -> T)?): T? {
    val tabs = driver.windowHandles.toList()
    val current = driver.windowHandle
    
    val js = "\$('#links > tbody > tr').get()[${data.server.index}].children[0].children[0].click()"
    driver.executeScript(js)
    wait(By.cssSelector("#player-container #frame"))
    
    if (driver.findElements(By.cssSelector("#player-container #frame a.btn")).isNotEmpty()) {
      
      driver.executeScript("\$('#player-container #frame a.btn').get()[0].click()")
      delay(500)
      
      if (driver.windowHandles.size > tabs.size) {
        driver.switchTo().window(driver.windowHandles.first { !tabs.contains(it) })
      }
      
      val res = blockExecutedOnPage?.invoke()
      
      if (driver.windowHandle != current) {
        driver.close()
      }
      driver.switchTo().window(current)
      return res
    } else {
      driver.switchTo().frame(driver.findElement(By.cssSelector("#player-container #frame iframe")))
      return blockExecutedOnPage?.invoke()
    }
  }
  
}
