package com.sheryv.tools.webcrawler.service

import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.model.SDriver
import com.sheryv.tools.webcrawler.process.impl.filmweb.FilmwebCrawlerDef
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.filman.FilmanCrawlerDef
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.filman.FilserCrawlerDef
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.fmovies.FMoviesCrawlerDef
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.zerion.ZerionCrawlerDef
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

object Registry {
  fun buildModule(): Module {
    return module(createdAtStart = true) {
      single { FilmwebCrawlerDef() }.bind<CrawlerDefinition<out SDriver, out SettingsBase>>()
      single { ZerionCrawlerDef() }.bind<CrawlerDefinition<out SDriver, out SettingsBase>>()
      single { FilmanCrawlerDef() }.bind<CrawlerDefinition<out SDriver, out SettingsBase>>()
      single { FMoviesCrawlerDef() }.bind<CrawlerDefinition<out SDriver, out SettingsBase>>()
      single { FilserCrawlerDef() }.bind<CrawlerDefinition<out SDriver, out SettingsBase>>()
    }
  }
}
