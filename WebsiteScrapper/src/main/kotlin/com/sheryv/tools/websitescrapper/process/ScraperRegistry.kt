package com.sheryv.tools.websitescrapper.process

import com.sheryv.tools.websitescrapper.process.base.ScraperDef
import com.sheryv.tools.websitescrapper.process.base.model.SDriver

class ScraperRegistry {
  private val registry: MutableMap<String, ScraperDef<out SDriver>> = mutableMapOf()
  
  fun register(scraperDef: ScraperDef<out SDriver>) {
    if (registry.containsKey(scraperDef.id))
      throw IllegalArgumentException("Scrapper with id ${scraperDef.id} already exists in registry")
    
    registry[scraperDef.id] = scraperDef
  }
  
  fun <T: SDriver> getTyped(name: String): ScraperDef<T>? {
    return registry[name] as ScraperDef<T>?
  }
  
  fun get(name: String): ScraperDef<*>? {
    return registry[name]
  }
  
  fun all() = registry.values.toSet()
  
  fun names() = registry.keys.toList()
  
  companion object {
    @JvmField
    val DEFAULT = ScraperRegistry()
  }
}
