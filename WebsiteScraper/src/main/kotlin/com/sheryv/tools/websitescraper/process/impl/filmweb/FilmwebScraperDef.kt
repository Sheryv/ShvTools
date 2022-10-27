package com.sheryv.tools.websitescraper.process.impl.filmweb

import com.sheryv.tools.websitescraper.browser.BrowserDef
import com.sheryv.tools.websitescraper.config.Configuration
import com.sheryv.tools.websitescraper.config.impl.FilmwebSettings
import com.sheryv.tools.websitescraper.process.base.Scraper
import com.sheryv.tools.websitescraper.process.base.ScraperDefinition
import com.sheryv.tools.websitescraper.process.base.model.SeleniumDriver

class FilmwebScraperDef : ScraperDefinition<SeleniumDriver, FilmwebSettings>("filmweb", FilmwebSettings::class.java) {
  
  override fun build(configuration: Configuration, browser: BrowserDef, driver: SeleniumDriver): Scraper<SeleniumDriver, FilmwebSettings> {
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
