package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.videoserver

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.process.base.SeleniumCrawler
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.StreamingWebsiteBase
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.videoserver.VideoServerDefinition
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.FileFormats
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Series
import com.sheryv.util.CoreUtils
import com.sheryv.util.io.HttpSupport
import com.sheryv.util.logging.log
import com.sheryv.util.singleAssign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.openqa.selenium.By

open class VideoServerHandler<D: VideoServerDefinition>(
  val def: D,
  protected val scraper: StreamingWebsiteBase,
  protected val scriptToActivatePlayer: String? = null,
  protected val scriptToCheckPlayerReady: String? = null,
  protected val innerIframeCssSelector: By? = null,
  protected val overrideFileFormat: FileFormats? = null,
) {
  protected val driver = scraper.driver
  
  suspend fun findVideoContainer(): By? {
    innerIframeCssSelector?.let {
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
    CoreUtils.wait(200, 2000) {
      activatePlayer(scraper)
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


  suspend fun activatePlayer(crawler: SeleniumCrawler<*>): Boolean {
    if (scriptToCheckPlayerReady == null) return false
    
    if (scriptToActivatePlayer != null) {
      
      val script = if (!scriptToCheckPlayerReady.startsWith("return ")) "return $scriptToActivatePlayer" else scriptToActivatePlayer
      
      if (crawler.driver.executeScript(script) == true) {
        crawler.driver.executeScript(scriptToActivatePlayer)
      }
    }
    return false
  }
}
