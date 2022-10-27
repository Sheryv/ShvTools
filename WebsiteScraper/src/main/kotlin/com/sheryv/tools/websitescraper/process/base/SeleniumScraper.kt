package com.sheryv.tools.websitescraper.process.base

import com.sheryv.tools.websitescraper.browser.BrowserDef
import com.sheryv.tools.websitescraper.config.Configuration
import com.sheryv.tools.websitescraper.config.SettingsBase
import com.sheryv.tools.websitescraper.process.base.model.SeleniumDriver
import com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.SeleniumSupport
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

abstract class SeleniumScraper<S : SettingsBase>(
  configuration: Configuration,
  browser: BrowserDef,
  def: ScraperDefinition<SeleniumDriver, S>,
  driver: SeleniumDriver
) :
  Scraper<SeleniumDriver, S>(configuration, browser, def, driver) {
  
  protected val wait: WebDriverWait by lazy { WebDriverWait(driver, Duration.ofSeconds(15)) }
  protected val support by lazy { SeleniumSupport(driver, wait) }
  protected lateinit var title: String
  
  protected fun loadInitPage() {
    driver.get(settings.websiteUrl)
    driver.waitForVisibility(By.tagName("body"))
    title = driver.title
  }
}
