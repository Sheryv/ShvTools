package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.filman

import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.browser.DriverBuilder
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.Crawler
import com.sheryv.tools.webcrawler.process.base.model.ProcessParams
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.StreamingCrawlerBase

class FilmanCrawlerDef : StreamingCrawlerBase(
  "filman",
  "Filman",
  "https://filman.cc",
) {
  
  override fun build(
    configuration: Configuration,
    browser: BrowserConfig,
    driverBuilder: DriverBuilder<SeleniumDriver>,
    params: ProcessParams
  ): Crawler<SeleniumDriver, StreamingWebsiteSettings> {
    return FilmanCrawler(configuration, browser, this, driverBuilder, params)
  }
}
