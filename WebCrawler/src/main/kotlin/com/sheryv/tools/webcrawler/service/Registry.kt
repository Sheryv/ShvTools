package com.sheryv.tools.webcrawler.service

import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.model.SDriver
import com.sheryv.tools.webcrawler.process.impl.filmweb.FilmwebCrawlerDef
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.CommonVideoServers
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.VideoServerDefinition
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.filman.FilmanCrawlerDef
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.fmovies.FMoviesCrawlerDef
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.zerion.ZerionCrawlerDef

object Registry {
  @JvmStatic
  val DEFAULT = object : RegistryProvider {
    override fun crawlers(): List<CrawlerDefinition<out SDriver, out SettingsBase>> {
      return listOf(
        FilmwebCrawlerDef(),
        ZerionCrawlerDef(),
        FilmanCrawlerDef(),
        FMoviesCrawlerDef(),
      )
    }
  
    override fun serverDefinitions() = CommonVideoServers.values().toList()
  
  }
  
  @JvmStatic
  private var registry = DEFAULT
  
  @JvmStatic
  fun use(provider: RegistryProvider) {
    registry = provider
  }
  
  @JvmStatic
  fun get() = registry
}

interface RegistryProvider {
  fun crawlers(): List<CrawlerDefinition<out SDriver, out SettingsBase>>
  
  fun serverDefinitions(): List<VideoServerDefinition>
}
