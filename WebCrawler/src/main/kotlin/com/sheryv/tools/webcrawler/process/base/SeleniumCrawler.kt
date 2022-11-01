package com.sheryv.tools.webcrawler.process.base

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.ProcessingStates
import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.process.base.model.TerminationException
import com.sheryv.tools.webcrawler.process.base.model.browserevent.BrowserResponseEvent
import com.sheryv.tools.webcrawler.process.base.model.browserevent.JSNetworkEvent
import com.sheryv.tools.webcrawler.utils.Utils
import com.sheryv.tools.webcrawler.utils.lg
import kotlinx.coroutines.delay
import org.openqa.selenium.By
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebElement
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

abstract class SeleniumCrawler<S : SettingsBase>(
  configuration: Configuration,
  browser: BrowserConfig,
  def: CrawlerDefinition<SeleniumDriver, S>,
  driver: SeleniumDriver
) :
  Crawler<SeleniumDriver, S>(configuration, browser, def, driver) {
  
  protected val wait: WebDriverWait by lazy { WebDriverWait(driver, Duration.ofSeconds(15)) }
  protected lateinit var title: String
  
  protected fun loadInitPage() {
    driver.get(def.attributes.websiteUrl)
    driver.waitForVisibility(By.tagName("body"))
    title = driver.title
  }
  
  internal suspend fun waitForAttribute(selector: By, attribute: String, timeoutSeconds: Int = 30): WebElement? {
    return waitForAttributeCheckBy(selector, attribute, timeoutSeconds) { it?.getAttribute(attribute) != null }
  }
  
  internal suspend fun waitForNonEmptyAttribute(selector: By, attribute: String, timeoutSeconds: Int = 30): WebElement? {
    return waitForAttributeCheckBy(selector, attribute, timeoutSeconds) { !it?.getAttribute(attribute).isNullOrEmpty() }
  }
  
  internal fun wait(selector: By, timeoutSeconds: Int = 3): WebElement? {
    var element: WebElement? = null
    try {
      element = wait.withTimeout(Duration.ofSeconds(timeoutSeconds.toLong())).until(ExpectedConditions.presenceOfElementLocated(selector))
      
    } catch (e: TimeoutException) {
      lg().warn("ERROR: Selector '{}' not found during {} seconds | {}", selector.toString(), timeoutSeconds, e.message)
    }
    return element
  }
  
  private suspend fun waitForAttributeCheckBy(
    selector: By,
    attribute: String,
    timeoutSeconds: Int,
    checkIsCorrect: (WebElement?) -> Boolean
  ): WebElement? {
    var element: WebElement? = null
    var waitValue = 5
    val repeats = if (timeoutSeconds > 10) {
      timeoutSeconds / waitValue
    } else {
      waitValue = 1
      timeoutSeconds
    }
    
    for (i in (0 until repeats)) {
      if (GlobalState.processingState.value == ProcessingStates.STOPPING) {
        throw TerminationException()
      }
      try {
        val start = System.currentTimeMillis()
        element = wait.withTimeout(Duration.ofSeconds(waitValue.toLong())).until(ExpectedConditions.presenceOfElementLocated(selector))
        if (checkIsCorrect(element)) {
          break
        }
        if (GlobalState.processingState.value == ProcessingStates.STOPPING) {
          throw TerminationException()
        }
        val end = System.currentTimeMillis()
        if (end - start < waitValue * 1000) {
          delay(waitValue * 1000 - (end - start))
        }
      } catch (ignored: TimeoutException) {
        element = null
      }
    }
    
    if (!checkIsCorrect(element)) {
      lg().warn("ERROR: Attribute '{}' not found for selector '{}' during {} seconds", attribute, selector.toString(), timeoutSeconds)
      return null
    }
    return element
  }
  
  fun getNetworkResponseEventsFromBrowserTools(): List<BrowserResponseEvent> {
    return driver.manage().logs().get(LogType.PERFORMANCE).all.asSequence()
      .map { Utils.jsonMapper.readTree(it.message).get("message") }
      .filter { it.get("method").asText() == "Network.responseReceived" }
      .map { Utils.jsonMapper.convertValue(it.get("params"), BrowserResponseEvent::class.java) }
      .toList()
  }
  
  fun getNetworkResponseEventsFromJS(): List<JSNetworkEvent> {
   return driver.executeScriptFetchList(
      "return window.performance.getEntries().filter(r => r.entryType == 'resource' && r.initiatorType == 'xmlhttprequest')"
    )?.let {
      Utils.jsonMapper.convertValue(it, Utils.jacksonTypeRef<List<JSNetworkEvent>>())
    }!!
  }
  
}
