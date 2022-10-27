package com.sheryv.tools.websitescraper.process.impl.streamingwebsite.zerion

import com.sheryv.tools.websitescraper.SystemUtils
import com.sheryv.tools.websitescraper.browser.BrowserDef
import com.sheryv.tools.websitescraper.config.Configuration
import com.sheryv.tools.websitescraper.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.websitescraper.process.base.Groups
import com.sheryv.tools.websitescraper.process.base.Scraper
import com.sheryv.tools.websitescraper.process.base.ScraperDefinition
import com.sheryv.tools.websitescraper.process.base.model.SeleniumDriver
import java.nio.file.Path

class ZerionScraperDef : ScraperDefinition<SeleniumDriver, StreamingWebsiteSettings>("zerion", StreamingWebsiteSettings::class.java, Groups.STREAMING_WEBSITE) {
  
  override fun build(
    configuration: Configuration,
    browser: BrowserDef,
    driver: SeleniumDriver
  ): Scraper<SeleniumDriver, StreamingWebsiteSettings> {
    return ZerionScraper(configuration, browser, this, driver)
  }
  
  override fun createDefaultSettings(): StreamingWebsiteSettings {
    return StreamingWebsiteSettings(
      "Zerion",
      "https://zerion.cc",
      defaultOutputPath().toString(),
      defaultOutputFormat(),
      Path.of(SystemUtils.userDownloadDir(), SystemUtils.removeForbiddenFileChars(id)).toAbsolutePath().toString()
    )
  }
}
