package com.sheryv.tools.websitescrapper.process.base

import com.sheryv.tools.websitescrapper.browser.BrowserDef
import com.sheryv.tools.websitescrapper.config.Configuration
import com.sheryv.tools.websitescrapper.process.base.model.SeleniumDriver
import org.openqa.selenium.By

abstract class SeleniumScraper(
  configuration: Configuration,
  browser: BrowserDef,
  def: ScraperDef<SeleniumDriver>,
  driver: SeleniumDriver
) :
  Scraper<SeleniumDriver>(configuration, browser, def, driver) {
  
  protected fun loadInitPage(): String {
    driver.get(def.websiteUrl)
    driver.waitForVisibility(By.tagName("body"))
    return driver.title
  }
}
