package com.sheryv.tools.websitescrapper.process.base

import com.sheryv.tools.websitescrapper.browser.BrowserDef
import com.sheryv.tools.websitescrapper.config.Configuration
import com.sheryv.tools.websitescrapper.process.base.model.SDriver
import com.sheryv.tools.websitescrapper.process.base.model.Step
import com.sheryv.tools.websitescrapper.utils.lg

abstract class Scraper<T : SDriver>(
  protected val configuration: Configuration,
  val browser: BrowserDef,
  val def: ScraperDef<T>,
  protected val driver: T
) {
  abstract fun getSteps(): List<Step<out Any>>
  
  fun log(text: String, vararg params: Any) {
    lg(javaClass).debug("[${def.id}|${browser.type.name}] " + text, *params)
  }
}
