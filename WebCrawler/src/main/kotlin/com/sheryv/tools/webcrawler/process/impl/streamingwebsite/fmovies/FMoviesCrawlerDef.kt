package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.fmovies

import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.Crawler
import com.sheryv.tools.webcrawler.process.base.model.ProcessParams
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.StreamingCrawlerBase

class FMoviesCrawlerDef : StreamingCrawlerBase(
  "fmovies",
  "FMovies",
  "https://fmovies.to",
) {
  
  override fun build(
    configuration: Configuration,
    browser: BrowserConfig,
    driver: SeleniumDriver,
    params: ProcessParams
  ): Crawler<SeleniumDriver, StreamingWebsiteSettings> {
    return FMoviesCrawler(configuration, browser, this, driver, params)
  }
}
