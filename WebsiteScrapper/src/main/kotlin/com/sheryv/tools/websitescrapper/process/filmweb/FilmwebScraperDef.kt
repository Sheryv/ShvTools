package com.sheryv.tools.websitescrapper.process.filmweb

import com.sheryv.tools.websitescrapper.browser.BrowserDef
import com.sheryv.tools.websitescrapper.config.Configuration
import com.sheryv.tools.websitescrapper.process.base.Scraper
import com.sheryv.tools.websitescrapper.process.base.model.Format
import com.sheryv.tools.websitescrapper.process.base.ScraperDef
import com.sheryv.tools.websitescrapper.process.base.model.SeleniumDriver

class FilmwebScraperDef :
  ScraperDef<SeleniumDriver>("filmweb", "Filmweb - filmy takie jak Ty! - www.filmweb.pl", "https://www.filmweb.pl/", "filmweb", Format.JSON) {
  
//  override fun build(configuration: Configuration,
//                     browser: BrowserDef,
//                     driver: SeleniumDriver): Scrapper<SeleniumDriver> {
//    TODO("Not yet implemented")
//  }
  
  override fun build(configuration: Configuration, browser: BrowserDef, driver: SeleniumDriver): Scraper<SeleniumDriver> {
    return FilmwebScraper(configuration, browser, this, driver)
  }
}
