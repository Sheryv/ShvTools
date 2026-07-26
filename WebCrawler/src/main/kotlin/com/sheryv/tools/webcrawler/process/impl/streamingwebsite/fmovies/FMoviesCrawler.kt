package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.fmovies

import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.browser.DriverBuilder
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.model.ProcessParams
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.StreamingWebsiteBase
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.SeriesDetailsFromPage
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.VideoData
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.VideoServer
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.videoserver.CommonVideoServers
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.videoserver.VideoServerHandler
import com.sheryv.util.CoreUtils
import kotlinx.coroutines.delay
import org.openqa.selenium.By
import org.openqa.selenium.WebElement

class FMoviesCrawler(
  configuration: Configuration,
  browser: BrowserConfig,
  def: CrawlerDefinition<SeleniumDriver, StreamingWebsiteSettings>,
  driverBuilder: DriverBuilder<SeleniumDriver>,
  params: ProcessParams
) : StreamingWebsiteBase(configuration, browser, def, driverBuilder, params) {
  
  override suspend fun getMainLang() = "en"
  
  override suspend fun findEpisodeItems(serverIndex: String?): List<VideoData> {
    CoreUtils.waitForNotNull(400, 10000) {
      driver.executeScript("document.querySelector('#play-now')?.click()")
      driver.findElements(By.cssSelector("#eps-list .episode")).firstOrNull()
    }
    
    val js = "return Array.from(document.querySelectorAll('#eps-list .episode')).map(a=>({u:a,e:a.textContent}))"
    
    return driver.executeScriptFetchList(js)
      ?.map { it["e"].toString().trim().toIntOrNull() to it["u"] as WebElement }
      ?.filter { t -> t.first != null }
      ?.reversed()
      ?.map { (i, t) -> VideoData("", series.title, i!!) }
      ?: throw IllegalArgumentException("Cannot fetch list from ${series.seriesUrl}")
  }
  
  override suspend fun loadItemDataFromSummaryPageAndGetServers(data: VideoData): List<VideoServer> {
//    wait(By.cssSelector("#servers .server"), 10)
//    val servers = "return Array.from(document.querySelectorAll('#servers .server div')).map(a=>({h: a.textContent}))"
//
//    return driver.executeScriptFetchList(servers)?.mapIndexed { i, map ->
//      val name = map["h"]!!.toString().trim { it <= ' ' }.lowercase()
//
//      VideoServer(name, i, EpisodeAudioTypes.UNKNOWN)
//    } ?: emptyList()
//
    return emptyList()
  }
  
  override suspend fun <T> openStreamAndInitializePlayerThenRun(
    data: VideoData,
    server: VideoServer,
    handler: VideoServerHandler<*>,
    blockExecutedOnPage: (suspend () -> T)?
  ): T? {
    
    val selector = "#eps-list #ep-${data.number}"
    val js = "document.querySelector('$selector').click()"
    waitOld(By.cssSelector(selector), 10, true)
    
    driver.executeScript(js)
    
    delay(300)
    
    val frame1 = waitOld(By.cssSelector("#playit"), 10, true)!!
    driver.switchTo().frame(frame1)
    
    delay(300)
    val frame2 = waitOld(By.cssSelector("#player iframe"), 10, true)!!
    driver.switchTo().frame(frame2)
    
    return blockExecutedOnPage?.invoke()
  }
  
  override suspend fun checkForCaptchaAndOtherOverlays(data: VideoData): Boolean {
    return false
  }
  
  override suspend fun extractSeriesDetails(): SeriesDetailsFromPage {
    val name =
      title?.let { titlePattern.matchEntire(it)?.groups?.get(1)?.value } ?: settings.seriesName.takeIf { it.isNotBlank() } ?: driver.title
    return SeriesDetailsFromPage(name!!, null)
  }
  
  override suspend fun isSingleBuiltinHostingPerEpisode(data: VideoData): Boolean {
    return true
  }
  
  override suspend fun builtinHostingServerHandler(data: VideoData): VideoServerHandler<*> {
    return CommonVideoServers.ALL.first { it.id == "fmovies" }.buildHandler(this)
  }
  
  private val titlePattern = Regex("""^Watch (.*) - Season \d+.*""")
}
