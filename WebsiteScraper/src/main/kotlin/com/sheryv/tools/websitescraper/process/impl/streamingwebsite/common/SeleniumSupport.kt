package com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common

import com.sheryv.tools.websitescraper.process.base.model.SeleniumDriver
import com.sheryv.tools.websitescraper.utils.lg
import kotlinx.coroutines.delay
import org.openqa.selenium.By
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class SeleniumSupport(private val driver: SeleniumDriver, private val wait: WebDriverWait = WebDriverWait(driver, Duration.ofSeconds(15))) {
  
  suspend fun waitForAttribute(selector: By, attribute: String, timeoutSeconds: Int = 30): WebElement? {
    return waitForAttributeCheckBy(selector, attribute, timeoutSeconds) { it?.getAttribute(attribute) != null }
  }
  
  suspend fun waitForNonEmptyAttribute(selector: By, attribute: String, timeoutSeconds: Int = 30): WebElement? {
    return waitForAttributeCheckBy(selector, attribute, timeoutSeconds) { !it?.getAttribute(attribute).isNullOrEmpty() }
  }
  
  fun wait(selector: By, timeoutSeconds: Int = 3): WebElement? {
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
      try {
        val start = System.currentTimeMillis()
        element = wait.withTimeout(Duration.ofSeconds(waitValue.toLong())).until(ExpectedConditions.presenceOfElementLocated(selector))
        if (checkIsCorrect(element)) {
          break
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
}
