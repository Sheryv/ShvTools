package com.sheryv.tools.websitescraper.process

import com.sheryv.tools.websitescraper.config.SettingsBase
import com.sheryv.tools.websitescraper.process.base.ScraperDef
import com.sheryv.tools.websitescraper.process.base.ScraperDefinition
import com.sheryv.tools.websitescraper.process.base.model.SDriver
import com.sheryv.tools.websitescraper.process.impl.filmweb.FilmwebScraperDef
import com.sheryv.tools.websitescraper.process.impl.streamingwebsite.zerion.ZerionScraperDef

class ScraperRegistry {
  private val registry: MutableMap<String, ScraperDefinition<out SDriver, out SettingsBase>> = mutableMapOf()
  
  fun register(scraperDef: ScraperDefinition<out SDriver, out SettingsBase>) {
    if (registry.containsKey(scraperDef.id))
      throw IllegalArgumentException("Scrapper with id ${scraperDef.id} already exists in registry")
    
    registry[scraperDef.id] = scraperDef
  }
  
  fun <T : SDriver> getTyped(name: String): ScraperDefinition<T, *>? {
    return registry[name] as ScraperDefinition<T, *>?
  }
  
  fun get(name: String): ScraperDef? {
    return registry[name] as? ScraperDef
  }
  
  fun all() = registry.values.toSet()
  
  fun names() = registry.keys.toList()
  
  companion object {
    @JvmField
    val DEFAULT = ScraperRegistry()
    
    fun fill(registry: ScraperRegistry): ScraperRegistry {
      registry.register(FilmwebScraperDef())
      registry.register(ZerionScraperDef())
      
      return registry
    }
  }
  
}
