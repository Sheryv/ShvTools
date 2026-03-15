package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.SeleniumCrawler
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.FileFormats
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Series
import com.sheryv.tools.webcrawler.utils.Utils
import com.sheryv.util.io.HttpSupport
import com.sheryv.util.logging.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.openqa.selenium.By


open class VideoServerHandler(
  val server: VideoServerDefinition,
  private val driver: SeleniumDriver,
  private val scraper: SeleniumCrawler<out SettingsBase>,
  private val overrideFileFormat: FileFormats? = null
) {
  
  suspend fun findVideoContainer(): By? {
    server.innerIframeCssSelector()?.let {
      scraper.wait(it)?.also { frame ->
        driver.switchTo().frame(frame)
        delay(300)
        activatePlayer()
      } ?: runBlocking(Dispatchers.Main) {
        GlobalState.view.showMessageDialog("Iframe not found")
      }
    } ?: activatePlayer()
    var byVideo = By.cssSelector("video:not(.hidden)")
    
    if (scraper.wait(byVideo, 5) == null) {
      byVideo = By.cssSelector("video:not(.hidden) > source")
      if (scraper.wait(byVideo, 5) == null) {
        log.error("video container not found on page (Did link expired?)")
        return null
      }
    }
    activatePlayer()
    return byVideo
  }
  
  private suspend fun activatePlayer() {
    var result = server.activatePlayer(scraper)
    var counter = 10
    while (!result && counter > 0) {
      delay(200)
      result = server.activatePlayer(scraper)
      counter--
    }
  }
  
  open suspend fun findVideoSrcUrl(timeout: Int = 12): String? {
    val byVideo = findVideoContainer() ?: return null
    val found = scraper.waitForNonEmptyAttribute(byVideo, "src", timeout)
    if (found != null) {
      return found.getAttribute("src")?.takeIf { it.isNotBlank() }
    }
    return null
  }
  
  open fun checkIfM3U8UrlCorrect(url: String, series: Series): Boolean = series.episodes.none { it.downloadUrl?.isSameUrl(url) == true }
  
  open fun tryToGetCorrectM3U8Url(incorrectUrl: String, series: Series): String? {
    if (incorrectUrl.contains("master.m3u8")) {
      val prefix = incorrectUrl.substringBefore("master.m3u8")
      val file = HttpSupport(securityEnabled = false).sendGet(incorrectUrl).body()

//      val file = HttpClients.createDefault().use { httpclient ->
//        val httpGet = ClassicRequestBuilder.get(incorrectUrl).build()
//
//        httpclient.execute(httpGet) { response: ClassicHttpResponse ->
//          EntityUtils.toString(response.entity)
//        }
//      }

//      val file = HttpSupport(false).sendGet(incorrectUrl).body()
      if (file.startsWith("#EXTM3U")) {
        return file.lines().firstOrNull { it.startsWith("index") && checkIfM3U8UrlCorrect(prefix + it, series) }?.let { prefix + it }
      }
    }
    return null
  }
}
