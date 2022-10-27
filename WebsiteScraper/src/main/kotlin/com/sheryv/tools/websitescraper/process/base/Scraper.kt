package com.sheryv.tools.websitescraper.process.base

import com.sheryv.tools.websitescraper.browser.BrowserDef
import com.sheryv.tools.websitescraper.config.Configuration
import com.sheryv.tools.websitescraper.config.SettingsBase
import com.sheryv.tools.websitescraper.process.base.model.SDriver
import com.sheryv.tools.websitescraper.process.base.model.Step
import com.sheryv.tools.websitescraper.utils.lg

abstract class Scraper<T : SDriver, S : SettingsBase>(
  protected val configuration: Configuration,
  val browser: BrowserDef,
  val def: ScraperDefinition<T, S>,
  protected val driver: T
) {
  abstract fun getSteps(): List<Step<out Any, out Any>>
  
  fun log(text: String, vararg params: Any) {
    lg(javaClass).debug("[${def.id}|${browser.type.name}] " + text, *params)
  }
  
  override fun toString(): String = "${settings.name} [${def.id}]"
  
  val settings: S = def.findSettings(configuration)
}
