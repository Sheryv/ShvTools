package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.zerion

import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.Groups
import com.sheryv.tools.webcrawler.process.base.Scraper
import com.sheryv.tools.webcrawler.process.base.ScraperDefinition
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.service.SystemSupport
import java.nio.file.Path

class ZerionScraperDef : ScraperDefinition<SeleniumDriver, StreamingWebsiteSettings>("zerion", StreamingWebsiteSettings::class.java, Groups.STREAMING_WEBSITE) {
  
  override fun build(
    configuration: Configuration,
    browser: BrowserConfig,
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
      SystemSupport.get.userDownloadDir.resolve(SystemSupport.get.removeForbiddenFileChars(id)).toAbsolutePath().toString()
    )
  }
}
