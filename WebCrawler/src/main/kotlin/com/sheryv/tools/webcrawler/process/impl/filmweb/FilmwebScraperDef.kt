package com.sheryv.tools.webcrawler.process.impl.filmweb

import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.FilmwebSettings
import com.sheryv.tools.webcrawler.process.base.Scraper
import com.sheryv.tools.webcrawler.process.base.ScraperDefinition
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver

class FilmwebScraperDef : ScraperDefinition<SeleniumDriver, FilmwebSettings>("filmweb", FilmwebSettings::class.java) {
  
  override fun build(configuration: Configuration, browser: BrowserConfig, driver: SeleniumDriver): Scraper<SeleniumDriver, FilmwebSettings> {
    return FilmwebScraper(configuration, browser, this, driver)
  }
  
  override fun createDefaultSettings(): FilmwebSettings {
    return FilmwebSettings(
      "Filmweb",
      "https://www.filmweb.pl",
      defaultOutputPath().toString(),
      defaultOutputFormat()
    )
  }
}
