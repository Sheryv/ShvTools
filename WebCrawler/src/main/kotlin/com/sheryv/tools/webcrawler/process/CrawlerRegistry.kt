package com.sheryv.tools.webcrawler.process

import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.CrawlerDef
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.model.SDriver
import com.sheryv.tools.webcrawler.process.impl.filmweb.FilmwebCrawlerDef
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.filman.FilmanCrawlerDef
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.zerion.ZerionCrawlerDef

class CrawlerRegistry {
  private val registry: MutableMap<String, CrawlerDefinition<out SDriver, out SettingsBase>> = mutableMapOf()
  
  fun register(crawlerDefinition: CrawlerDefinition<out SDriver, out SettingsBase>) {
    if (registry.containsKey(crawlerDefinition.attributes.id))
      throw IllegalArgumentException("Crawler with id ${crawlerDefinition.attributes.id} already exists in registry")
    
    registry[crawlerDefinition.attributes.id] = crawlerDefinition
  }
  
  fun <T : SDriver> getTyped(name: String): CrawlerDefinition<T, *>? {
    return registry[name] as CrawlerDefinition<T, *>?
  }
  
  fun get(name: String): CrawlerDef? {
    return registry[name] as? CrawlerDef
  }
  
  fun all() = registry.values.toSet()
  
  fun names() = registry.keys.toList()
  
  companion object {
    @JvmStatic
    val DEFAULT by lazy {
      fill(CrawlerRegistry())
    }
    
    fun fill(registry: CrawlerRegistry): CrawlerRegistry {
      registry.register(FilmwebCrawlerDef())
      registry.register(ZerionCrawlerDef())
      registry.register(FilmanCrawlerDef())
      
      return registry
    }
  }
  
}
