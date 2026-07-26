@file:JvmName("MainLauncher")

package com.sheryv.tools.webcrawler

import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.SecretsStore
import com.sheryv.tools.webcrawler.service.Registry
import com.sheryv.tools.webcrawler.service.streamingwebsite.scraper.TmdbService
import com.sheryv.tools.webcrawler.view.MainView
import com.sheryv.tools.webcrawler.view.downloader.DownloaderView
import com.sheryv.tools.webcrawler.view.jdownloader.JDownloaderView
import com.sheryv.tools.webcrawler.view.remoteclient.HttpServerView
import com.sheryv.tools.webcrawler.view.search.SearchView
import com.sheryv.util.fx.core.app.App
import com.sheryv.util.fx.core.app.AppConfiguration
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import java.lang.management.ManagementFactory


fun main(args: Array<String>) {
  println("Main init at " + (System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().startTime))
//  System.setProperty("logback.debug", "true")

//  -Dprism.text=t2k
//  "-Djavafx.autoproxy.disable=true"
//  "-Djavafx.verbose=true"
//  System.setProperty("flatlaf.useWindowDecorations", "false")
  
  
  App.createAndStart<MainView>(args) {
    module(Registry.buildModule())
    module {
      factoryOf(::MainView)
      factoryOf(::DownloaderView)
      factoryOf(::JDownloaderView)
      factoryOf(::SearchView)
      factoryOf(::HttpServerView)
      factoryOf(::TmdbService)
      single<SecretsStore> { SecretsStore.load() }
      single { Configuration.load(getAll()) } bind AppConfiguration::class
    }
  }
}
