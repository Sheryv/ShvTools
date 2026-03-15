package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.zerion

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.ProcessingStates
import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.browser.DriverBuilder
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions

class ZerionCrawler(
  configuration: Configuration,
  browser: BrowserConfig,
  def: CrawlerDefinition<SeleniumDriver, StreamingWebsiteSettings>,
  driverBuilder: DriverBuilder<SeleniumDriver>,
  params: ProcessParams
) : StreamingWebsiteBase(configuration, browser, def, driverBuilder, params) {
  
  override suspend fun getMainLang() = "pl"
  
  override suspend fun findEpisodeItems(serverIndex: String?): List<VideoData> {
    wait.until<List<WebElement>>(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("#series-page .season")))
    // language=js
    val js = """
        return shv.findIn('#series-page .season:nth-child(${series.season}) > ul')
        .map(v=>v.querySelector('.title-date-block a')).filter(v=>!!v)
        .map(v=>{return {e: v.childNodes[0].textContent, u:v.attributes['href'].value}});""".trimIndent()
    
    return driver.executeScriptFetchList(js)?.mapIndexed { i, t ->
      val name = t["e"].toString().trim { it <= ' ' }.replace(Regex("""[sS]\d+[eE]\d+"""), "").trim()
      val link = t["u"] as String
      
      VideoData(link, name, i + 1)
    } ?: throw IllegalArgumentException("Cannot fetch list from ${series.seriesUrl}")
  }
  
  override suspend fun loadItemDataFromSummaryPageAndGetServers(data: VideoData): List<VideoServer> {
    // language=js
    val js ="""
      return shv.find('.video-list > table')
      .map(v => [shv.findIn(v, 'tr').filter(v=>v.childNodes[0].nodeName === 'TD'), v.getAttribute('data-key')])
      .flatMap(a => a[0].map(v=>{return {h: v.childNodes[0].textContent, q: v.childNodes[1].textContent, a: a[1]}}));
      """.trimIndent()
    
    return driver.executeScriptFetchList(js)
      ?.map { map ->
        val name = map["h"]!!.toString().trim { it <= ' ' }.lowercase()
        val format = map["q"]!!.toString().trim { it <= ' ' }.lowercase()
        val audio = map["a"]!!.toString().trim().uppercase()
        Triple(name, format, audio)
      }
      ?.filter { it.first != "premium" }
      ?.mapIndexed { i, (name, format, audio) ->
        val audioType = when(audio){
          "PL" -> EpisodeAudioTypes.LECTOR
          "DUB" -> EpisodeAudioTypes.DUBBING
          "SUBPL" -> EpisodeAudioTypes.SUBS
          else -> EpisodeAudioTypes.UNKNOWN
        }
        VideoServer(name, i, audioType, VideoServerFormat(format))
      }
      ?: emptyList()
  }
  
  override suspend fun <T> openStreamAndInitializePlayerThenRun(data: VideoData, server: VideoServer, blockExecutedOnPage: (suspend () -> T)?): T? {
    val js = "shv.click_fast(shv.find('.video-list tr .btn.watch-btn')[" + server.index + "])"
    driver.executeScript(js)
    return blockExecutedOnPage?.invoke()
  }
  
  override suspend fun checkForCaptchaAndOtherOverlays(data: VideoData): Boolean {
    val captacha = wait(By.cssSelector(".hcaptcha"))
    if (captacha != null) {
      GlobalState.processingState.value = (ProcessingStates.PAUSED)
      runBlocking(Dispatchers.Main) {
        GlobalState.view.showMessageDialog("Captcha detected! Solve it and resume process.")
      }
      waitIfPaused()
      return true
    }
    return false
  }
  
}
