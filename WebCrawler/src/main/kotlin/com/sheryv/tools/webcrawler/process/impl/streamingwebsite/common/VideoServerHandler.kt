package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.SeleniumScraper
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.FileFormats
import com.sheryv.tools.webcrawler.utils.lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.openqa.selenium.By

open class VideoServerHandler(
  private val server: VideoServerDefinition,
  private val driver: SeleniumDriver,
  private val scraper: SeleniumScraper<out SettingsBase>,
  private val overrideFileFormat: FileFormats? = null
) {
  
  open suspend fun findVideoSrcUrl(timeout: Int = 12): String? {
    server.innerIframeCssSelector()?.let {
      scraper.wait(it)?.also { frame ->
        driver.switchTo().frame(frame)
        delay(300)
        server.scriptToActivatePlayer()?.also { driver.executeScript(it) }
      } ?: runBlocking(Dispatchers.Main) {
        GlobalState.view.showMessageDialog("Iframe not found")
      }
    } ?: server.scriptToActivatePlayer()?.also { driver.executeScript(it) }
    
    val byVideo = By.cssSelector("video:not(.hidden)")
    
    if(scraper.wait(byVideo, 5) == null) {
      lg().error("video container not found on page (Did link expired?)")
      return null
    }
    val found = scraper.waitForNonEmptyAttribute(byVideo, "src", timeout)
    if (found != null) {
      return found.getAttribute("src").takeIf { it.isNotBlank() }
    }
    return null
  }
  
  open fun checkIfM3U8UrlCorrect(url: String): Boolean = true
}
