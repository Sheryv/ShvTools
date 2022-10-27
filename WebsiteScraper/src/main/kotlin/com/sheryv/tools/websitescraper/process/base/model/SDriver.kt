package com.sheryv.tools.websitescraper.process.base.model

import com.sheryv.tools.websitescraper.config.SettingsBase
import com.sheryv.tools.websitescraper.process.base.Scraper
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

interface SDriver {
  fun initialize(scraper: Scraper<out SDriver, SettingsBase>)
  
  fun findElementsByTag(tag: String): Elements
  
  fun findElementsBySelector(selector: String): Elements
  
  fun findElementById(id: String): Element?
  
  fun getPageDocument(): Document
  
  fun get(url: String)
}
