package com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common

import com.sheryv.tools.websitescraper.GlobalState
import com.sheryv.tools.websitescraper.ProcessingStates
import com.sheryv.tools.websitescraper.process.base.model.SeleniumDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.openqa.selenium.By

open class VideoServerHandler(
  private val server: VideoServerDefinition,
  private val driver: SeleniumDriver,
  private val support: SeleniumSupport
) {
  
  open suspend fun findVideoSrcUrl(timeout: Int = 30): String? {
    val captacha = support.wait(By.cssSelector(".hcaptcha"))
    if (captacha != null) {
      GlobalState.processingState.set(ProcessingStates.PAUSED)
      runBlocking(Dispatchers.Main) {
        GlobalState.view.showMessageDialog("Captcha detected! Solve it and resume process.")
      }
      do {
        delay(500)
      } while (GlobalState.processingState.value == ProcessingStates.PAUSED)
    }
    
    server.innerIframeCssSelector()?.let {
      support.wait(it)?.also { frame ->
        driver.switchTo().frame(frame)
        delay(300)
        server.scriptToActivatePlayer()?.also { driver.executeScript(it) }
      } ?: runBlocking(Dispatchers.Main) {
        GlobalState.view.showMessageDialog("Iframe not found")
      }
    } ?: server.scriptToActivatePlayer()?.also { driver.executeScript(it) }
    
    val byVideo = By.cssSelector("video:not(.hidden)")
    val found = support.waitForNonEmptyAttribute(byVideo, "src", timeout)
    if (found != null) {
      return found.getAttribute("src").takeIf { it.isNotBlank() }
    }
    return null
  }
}
