package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.zerion

import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.Crawler
import com.sheryv.tools.webcrawler.process.base.CrawlerAttributes
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.Groups
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.service.SystemSupport

class ZerionCrawlerDef : CrawlerDefinition<SeleniumDriver, StreamingWebsiteSettings>(
  CrawlerAttributes(
    "zerion",
    "Zerion",
    "https://zerion.cc",
    Groups.STREAMING_WEBSITE
  ),
  StreamingWebsiteSettings::class.java
) {
  
  override fun build(
    configuration: Configuration,
    browser: BrowserConfig,
    driver: SeleniumDriver
  ): Crawler<SeleniumDriver, StreamingWebsiteSettings> {
    return ZerionCrawler(configuration, browser, this, driver)
  }
  
  override fun createDefaultSettings(): StreamingWebsiteSettings {
    return StreamingWebsiteSettings(
      id(),
      downloadDir = SystemSupport.get.userDownloadDir.resolve(SystemSupport.get.removeForbiddenFileChars(attributes.id)).toString()
    )
  }
}
