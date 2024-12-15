package com.sheryv.tools.webcrawler.process.impl.filmweb

import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.FilmwebSettings
import com.sheryv.tools.webcrawler.process.base.Crawler
import com.sheryv.tools.webcrawler.process.base.CrawlerAttributes
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.model.ProcessParams
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver

class FilmwebCrawlerDef : CrawlerDefinition<SeleniumDriver, FilmwebSettings>(
  CrawlerAttributes(
    "filmweb",
    "Filmweb",
    "https://www.filmweb.pl",
  ),
  FilmwebSettings::class.java
) {
  
  override fun build(
    configuration: Configuration,
    browser: BrowserConfig,
    driver: SeleniumDriver,
    params: ProcessParams
  ): Crawler<SeleniumDriver, FilmwebSettings> {
    return FilmwebCrawler(configuration, browser, this, driver, params)
  }
  
  override fun createDefaultSettings(): FilmwebSettings {
    return FilmwebSettings(id(), defaultOutputPath())
  }
}
