package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.videoserver

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.process.base.SeleniumCrawler
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.StreamingWebsiteBase
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.FileFormats
import com.sheryv.util.CoreUtils
import com.sheryv.util.logging.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import kotlin.time.Duration.Companion.milliseconds

open class VideoServerHandler<D : VideoServerDefinition>(
  val def: D,
  protected val scraper: StreamingWebsiteBase,
  protected val scriptToActivatePlayer: String? = null,
  protected val scriptToCheckPlayerReady: String? = null,
  protected val innerIframeCssSelector: By? = null,
  protected val overrideFileFormat: FileFormats? = null,
) {
  protected val driver = scraper.driver
  
  suspend fun findVideoContainer(): WebElement? {
    val byVideo = By.cssSelector("video:not(.hidden)")
    val container = driver.closeTabsOpenedInBackground {
      
      innerIframeCssSelector?.let {
        scraper.wait(it)?.also { frame ->
          delay(1000.milliseconds)
          log.debug("Switching to iframe {}", frame)
          driver.switchTo().frame(frame)
          delay(300.milliseconds)
          activatePlayer(scraper)
        } ?: runBlocking(Dispatchers.Main) {
          GlobalState.view.showMessageDialog("Iframe not found")
        }
      } ?: activatePlayer(scraper)
      
      
      val container = CoreUtils.waitForNotNull(200, 10000) {
        val container = scraper.wait(byVideo, 1) { it.getAttribute("src")?.isNotBlank() == true }
        log.trace("video container {}", container)
        
        if (container?.getAttribute("src").isNullOrBlank()) {
          activatePlayer(scraper)
          null
        } else {
          container
        }
      }
      container
    }
    
    if (container == null) {
      log.error("video container not found on page (Did link expired?)")
      return null
    }

//    if (container == null) {
//      byVideo = By.cssSelector("video:not(.hidden) > source")
//      if (scraper.wait(byVideo, 5) == null) {
//        log.error("video container not found on page (Did link expired?)")
//        return null
//      }
//    }
//    activatePlayer()
    return container
  }
  
  open suspend fun findVideoSrcUrl(timeout: Int = 12): String? {
//    val byVideo = findVideoContainer() ?: return null
    return findVideoContainer()?.getAttribute("src")?.takeIf { it.isNotBlank() }
  }
  
  
  suspend fun activatePlayer(crawler: SeleniumCrawler<*>): Boolean {
    if (scriptToCheckPlayerReady == null) return false
    
    if (scriptToActivatePlayer != null) {
      
      val script = if (!scriptToCheckPlayerReady.startsWith("return ")) "return $scriptToCheckPlayerReady" else scriptToCheckPlayerReady
      
      if (crawler.driver.executeScript(script) == true) {
        log.debug("Activating player ${def.id}")
        crawler.driver.executeScript(scriptToActivatePlayer)
        return true
      }
      return false
    }
    return false
  }
}
