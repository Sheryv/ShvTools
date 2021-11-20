package com.sheryv.tools.websitescrapper.process.base

import com.sheryv.tools.websitescrapper.SystemUtils
import com.sheryv.tools.websitescrapper.browser.BrowserDef
import com.sheryv.tools.websitescrapper.config.Configuration
import com.sheryv.tools.websitescrapper.process.base.model.SDriver
import com.sheryv.tools.websitescrapper.process.base.model.Format

abstract class ScraperDef<T : SDriver>(
  val id: String,
  val name: String,
  val websiteUrl: String,
  val outputFile: String = SystemUtils.removeForbiddenFileChars(id),
  val outputFormat: Format,
  protected val driverClass: Class<in T> = SDriver::class.java
) {
  
  abstract fun build(
    configuration: Configuration,
    browser: BrowserDef,
    driver: T
  ): Scraper<T>
  
  override fun toString(): String {
    return name
  }
  
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    other as ScraperDef<*>
    
    if (id != other.id) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    return id.hashCode()
  }
}
