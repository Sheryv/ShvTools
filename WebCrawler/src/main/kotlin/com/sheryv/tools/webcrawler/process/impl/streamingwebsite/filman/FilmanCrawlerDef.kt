package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.filman

import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.Crawler
import com.sheryv.tools.webcrawler.process.base.CrawlerAttributes
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.Groups
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.service.SystemSupport

class FilmanCrawlerDef :
  CrawlerDefinition<SeleniumDriver, StreamingWebsiteSettings>(
    CrawlerAttributes(
      "filman",
      "Filman",
      "https://filman.cc",
      Groups.STREAMING_WEBSITE
    ), StreamingWebsiteSettings::class.java
  ) {
  
  override fun build(
    configuration: Configuration,
    browser: BrowserConfig,
    driver: SeleniumDriver
  ): Crawler<SeleniumDriver, StreamingWebsiteSettings> {
    return FilmanCrawler(configuration, browser, this, driver)
  }
  
  override fun createDefaultSettings(): StreamingWebsiteSettings {
    return StreamingWebsiteSettings(
      id(),
      downloadDir = SystemSupport.get.userDownloadDir.resolve(SystemSupport.get.removeForbiddenFileChars(attributes.id)).toString()
    )
  }
}
