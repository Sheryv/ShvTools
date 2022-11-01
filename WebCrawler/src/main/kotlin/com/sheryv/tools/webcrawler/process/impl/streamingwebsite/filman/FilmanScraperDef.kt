package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.filman

import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.Groups
import com.sheryv.tools.webcrawler.process.base.Scraper
import com.sheryv.tools.webcrawler.process.base.ScraperDefinition
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.service.SystemSupport

class FilmanScraperDef :
  ScraperDefinition<SeleniumDriver, StreamingWebsiteSettings>("filman", StreamingWebsiteSettings::class.java, Groups.STREAMING_WEBSITE) {
  
  override fun build(
    configuration: Configuration,
    browser: BrowserConfig,
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
      SystemSupport.get.userDownloadDir.resolve(SystemSupport.get.removeForbiddenFileChars(id)).toAbsolutePath().toString()
    )
  }
}
