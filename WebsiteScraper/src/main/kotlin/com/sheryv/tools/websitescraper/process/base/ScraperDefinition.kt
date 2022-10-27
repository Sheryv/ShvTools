package com.sheryv.tools.websitescraper.process.base

import com.sheryv.tools.websitescraper.SystemUtils
import com.sheryv.tools.websitescraper.browser.BrowserDef
import com.sheryv.tools.websitescraper.config.Configuration
import com.sheryv.tools.websitescraper.config.SettingsBase
import com.sheryv.tools.websitescraper.process.base.model.Format
import com.sheryv.tools.websitescraper.process.base.model.SDriver
import com.sheryv.tools.websitescraper.utils.Utils
import java.nio.file.Path
import java.time.format.DateTimeFormatter

typealias ScraperDef = ScraperDefinition<in SDriver, SettingsBase>

abstract class ScraperDefinition<T : SDriver, S : SettingsBase>(
  val id: String,
  internal val settingsClass: Class<S>,
  val group: ListGroup = Groups.GENERAL,
  protected val driverClass: Class<in T> = SDriver::class.java
) {
  abstract fun createDefaultSettings(): S
  
  fun findSettings(config: Configuration): S = config.settings[id] as S
  
  abstract fun build(
    configuration: Configuration,
    browser: BrowserDef,
    driver: T
  ): Scraper<T, S>
  
  protected fun defaultOutputPath(): Path {
    return Path.of(
      SystemUtils.userDownloadDir(),
      "${SystemUtils.removeForbiddenFileChars(id)}-${Utils.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.${defaultOutputFormat().extension}"
    ).toAbsolutePath()
  }
  
  protected fun defaultOutputFormat(): Format = Format.JSON
  
  override fun toString(): String {
    return id
  }
  
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    other as ScraperDefinition<*, *>
    
    if (id != other.id) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    return id.hashCode()
  }
}
