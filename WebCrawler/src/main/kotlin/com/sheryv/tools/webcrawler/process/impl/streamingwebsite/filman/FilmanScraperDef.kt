package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.filman

import com.sheryv.tools.webcrawler.SystemUtils
import com.sheryv.tools.webcrawler.browser.BrowserDef
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.Groups
import com.sheryv.tools.webcrawler.process.base.Scraper
import com.sheryv.tools.webcrawler.process.base.ScraperDefinition
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import java.nio.file.Path

class FilmanScraperDef : ScraperDefinition<SeleniumDriver, StreamingWebsiteSettings>("filman", StreamingWebsiteSettings::class.java, Groups.STREAMING_WEBSITE) {
  
  override fun build(
    configuration: Configuration,
    browser: BrowserDef,
    driver: SeleniumDriver
  ): Scraper<SeleniumDriver, StreamingWebsiteSettings> {
    return FilmanScraper(configuration, browser, this, driver)
  }
  
  override fun createDefaultSettings(): StreamingWebsiteSettings {
    return StreamingWebsiteSettings(
      "Filman",
      "https://filman.cc",
      defaultOutputPath().toString(),
      defaultOutputFormat(),
      Path.of(SystemUtils.userDownloadDir(), SystemUtils.removeForbiddenFileChars(id)).toAbsolutePath().toString()
    )
  }
}
