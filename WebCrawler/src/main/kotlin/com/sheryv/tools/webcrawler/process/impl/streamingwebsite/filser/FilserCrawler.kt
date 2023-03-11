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

class FilserCrawler(
  configuration: Configuration,
  browser: BrowserConfig,
  def: CrawlerDefinition<SeleniumDriver, StreamingWebsiteSettings>,
  driver: SeleniumDriver,
  params: ProcessParams
) : StreamingWebsiteBase(configuration, browser, def, driver, params) {
  private val urlPattern = Regex("(" +Regex.escape(def.attributes.websiteUrl) + """)?/?(title|watch)/(\w+)/?.*""")
  
  
  override suspend fun getMainLang() = "pl"
  
  override suspend fun findEpisodeItems(serverIndex: String?): List<VideoData> {
    val id = urlPattern.matchEntire(series.seriesUrl)!!.groupValues[3]
    driver.navigate().to("${def.attributes.websiteUrl}/title/$id/${settings.seasonNumber}")
    delay(300)
    
    wait.until<List<WebElement>>(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("#episode_list")))
    val js =
      "return Array.from(document.querySelectorAll('#episode_list .episode-box a')).map(n=>({e: n.querySelector(':scope > span').textContent, u:n.getAttribute('href')}))"
    
    val fixNameRegex = Regex("""^\d+\.""")
    
    return driver.executeScriptFetchList(js)?.mapIndexed { i, t ->
      val nameRaw = t["e"].toString()
      val name = nameRaw.trim { it <= ' ' }.replace(fixNameRegex, "").trim()
      val link = t["u"].toString().trim()
//      val link = "${def.attributes.websiteUrl}/title/$id/${settings.seasonNumber}/${i + 1}"
      
      VideoData(link, name, i + 1)
    } ?: throw IllegalArgumentException("Cannot fetch list from ${series.seriesUrl}")
  }
  
  override suspend fun loadItemDataFromSummaryPageAndGetServers(data: VideoData): List<VideoServer> {
    wait.until<List<WebElement>>(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("#playerContent > div > div")))
    val js = "return Array.from(document.querySelectorAll('#playerContent > div > div > div > a')).map(n=>({u: n.getAttribute('href'), " +
        "q: n.children[0].children[2].textContent, h: n.children[0].children[1].textContent}))"
    
    return driver.executeScriptFetchList(js)?.mapIndexed { i, map ->
      val name = map["h"]!!.toString().trim { it <= ' ' }.lowercase()
      val url = getFullUrl(map["u"]!!.toString().trim { it <= ' ' }.lowercase())
      val formatBlock = map["q"]!!.toString().trim { it <= ' ' }.lowercase().split("\n").map { it.trim() }.filter { it.isNotEmpty() }
      val format = formatBlock.last()
      
      val audio = when (formatBlock[formatBlock.lastIndex - 1]) {
        "lektor" -> EpisodeAudioTypes.LECTOR
        "napisy" -> EpisodeAudioTypes.SUBS
        "dubbing" -> EpisodeAudioTypes.DUBBING
        "oryginalna", "eng" -> EpisodeAudioTypes.ORIGIN
        else -> EpisodeAudioTypes.UNKNOWN
      }
      
      VideoServer(name, i, audio, VideoServerFormat(format), url)
    } ?: emptyList()
  }

//  override suspend fun <T> goToExternalServerVideoPage(data: VideoData, blockExecutedOnPage: (suspend () -> T)?): T? {
//    driver.navigate().to(data.episodePageUrl)
//    delay(300)
//    val res = blockExecutedOnPage?.invoke()
//
////    val tabs = driver.windowHandles.toList()
////    val current = driver.windowHandle
////
////    val js = "\$('#links > tbody > tr').get()[${data.server.index}].children[0].children[0].click()"
////    driver.executeScript(js)
////    wait(By.cssSelector("#player-container #frame"))
////    driver.executeScript("\$('#player-container #frame a.btn').get()[0].click()")
////    delay(500)
////
////    if (driver.windowHandles.size > tabs.size) {
////      driver.switchTo().window(driver.windowHandles.first { !tabs.contains(it) })
////    }
////
////    val res = blockExecutedOnPage?.invoke()
////
////    if (driver.windowHandle != current) {
////      driver.close()
////    }
////    driver.switchTo().window(current)
//    return res
//  }
  
  override suspend fun isSingleBuiltinHostingPerEpisode(data: VideoData): Boolean {
    val listOfServers = wait(By.cssSelector("#playerContent > div"), 2) != null
    if (listOfServers) {
      return false
    }
    throw UnsupportedOperationException("No support for builtin streaming hosting")
  }
}
