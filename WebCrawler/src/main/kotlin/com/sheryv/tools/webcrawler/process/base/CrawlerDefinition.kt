package com.sheryv.tools.webcrawler.process.base

import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.event.FetchedDataStatusChangedEvent
import com.sheryv.tools.webcrawler.process.base.model.Format
import com.sheryv.tools.webcrawler.process.base.model.ProcessParams
import com.sheryv.tools.webcrawler.process.base.model.SDriver
import com.sheryv.tools.webcrawler.utils.postEvent
import java.net.URL

typealias CrawlerDef = CrawlerDefinition<in SDriver, SettingsBase>

abstract class CrawlerDefinition<T : SDriver, S : SettingsBase>(
  val attributes: CrawlerAttributes,
  internal val settingsClass: Class<S>,
  protected val driverClass: Class<in T> = SDriver::class.java,
) {
  
  fun id() = attributes.id
  
  fun findSettings(config: Configuration): S = config.settings.first { it.crawlerId == id() } as S
  
  abstract fun createDefaultSettings(): S
  
  abstract fun build(
    configuration: Configuration,
    browser: BrowserConfig,
    driver: T,
    params: ProcessParams
  ): Crawler<T, S>
  
  fun update(config: Configuration) {
  }
  
  protected open fun currentStatusToText(config: Configuration): String = ""
  
  override fun toString(): String {
    val url = URL(attributes.websiteUrl)
    return attributes.name + " - " + url.host
  }
  
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    other as CrawlerDefinition<*, *>
    
    if (attributes.id != other.attributes.id) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    return attributes.id.hashCode()
  }
}

class CrawlerAttributes(
  val id: String,
  val name: String,
  val websiteUrl: String,
  val group: ListGroup = Groups.GENERAL,
  val outputFileFormat: Format = Format.JSON,
) {

}
