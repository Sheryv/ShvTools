package com.sheryv.tools.webcrawler.process

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.browser.DriverBuilder
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.CrawlerDef
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.SeleniumCrawler
import com.sheryv.tools.webcrawler.process.base.model.ProcessParams
import com.sheryv.tools.webcrawler.process.base.model.SDriver
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.process.base.model.Step
import com.sheryv.tools.webcrawler.process.base.model.TerminationException
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.StreamingCrawlerBase
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.StreamingWebsiteBase
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.VideoServer
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.VideoUrl
import com.sheryv.tools.webcrawler.service.streamingwebsite.moviesearch.MovieFromFile
import com.sheryv.tools.webcrawler.utils.AppError
import com.sheryv.util.logging.log
import kotlinx.coroutines.delay
import org.openqa.selenium.SessionNotCreatedException
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.time.Duration.Companion.milliseconds


class Runner(
  private val configuration: Configuration,
  private val browser: BrowserConfig,
  private val crawlerDef: CrawlerDef,
  private val params: ProcessParams
) {
  
  suspend fun prepare(settings: SettingsBase) {
    settings.validate(crawlerDef)
    configuration.updateSettings(settings)
  }
  
  suspend fun start() {
    val config = configuration.copy()
    val driverPath = browser.currentDriver().path
    if (!driverPath.exists() || !driverPath.isExecutable()) {
      log.info("Browser configuration '${browser.type.name}' was rejected because driver was not found at '${driverPath}' or it is not correct executable")
      throw AppError("Cannot start browser because driver was not found at '${driverPath}' or it is not correct executable file")
    }
    
    System.setProperty(browser.selectedDriver.propertyNameForSeleniumDriver, driverPath.toAbsolutePath().toString())
    
    try {
      (crawlerDef as CrawlerDefinition<SDriver, SettingsBase>).build(config, browser,  browser.selectedDriver.webDriverBuilder, params).use { crawler ->
        
        crawler.initialize()
        if (crawler is SeleniumCrawler<SettingsBase>) {
          GlobalState.runningProcess.value = (crawler)
        }
        log.info("Crawler '${crawler.def}' built")
        
        val steps = crawler.getSteps() as List<Step<Any, Any>>
        var any: Any? = null
        for (step in steps) {
          crawler.waitIfPaused()
          log.info("Running step '${step.name}' by '${crawler.def}'")
          any = step.run(any)
        }
        
        log.info("Crawler '${crawler.def}' finished successfully")
      }
    } catch (e: SessionNotCreatedException) {
      throw AppError("Cannot start browser: " + e.message, e)
    } catch (e: TerminationException) {
      log.info(e.message)
    } catch (e: Exception) {
      throw e
    } finally {
      GlobalState.runningProcess.value = null
    }
  }
  
  suspend fun startStreamingWebsiteSingleRun() {
    if (crawlerDef !is StreamingCrawlerBase) {
      throw AppError("This function works only for video streaming websites")
    }
    require(params.streamingUrlOverride != null)
    
    val config = configuration.copy()
    val driverPath = browser.currentDriver().path
    if (!driverPath.exists() || !driverPath.isExecutable()) {
      log.info("Browser configuration '${browser.type.name}' was rejected because driver was not found at '${driverPath}' or it is not correct executable")
      throw AppError("Cannot start browser because driver was not found at '${driverPath}' or it is not correct executable file")
    }
    
    System.setProperty(browser.selectedDriver.propertyNameForSeleniumDriver, driverPath.toAbsolutePath().toString())
    
    try {
      ((crawlerDef as StreamingCrawlerBase).build(
        config,
        browser,
        browser.selectedDriver.webDriverBuilder as DriverBuilder<SeleniumDriver>,
        params
      ) as StreamingWebsiteBase).use { crawler ->
        
        crawler.initialize()
        val driver = crawler.driver
        GlobalState.runningProcess.value = crawler as SeleniumCrawler<SettingsBase>
        log.info("Crawler '${crawler.def}' built")
        
        driver.enableDevToolsWithNetworkModule()
        
        val downloadUrl = crawler.openStandaloneStreamingPage(params.streamingUrlOverride)
//      if (downloadUrl)
        log.info("URL: $downloadUrl")
        log.info("Crawler '${crawler.def}' finished successfully")
      }
    } catch (e: SessionNotCreatedException) {
      throw AppError("Cannot start browser: " + e.message, e)
    } catch (e: TerminationException) {
      log.info(e.message)
    } catch (e: Exception) {
      throw e
    } finally {
      GlobalState.runningProcess.value = null
    }
  }
  
  suspend fun searchAllMovies(movies: List<MovieFromFile>): Map<MovieFromFile, Pair<VideoUrl, VideoServer>> {
    val config = configuration.copy()
    val driverPath = browser.currentDriver().path
    if (!driverPath.exists() || !driverPath.isExecutable()) {
      log.info("Browser configuration '${browser.type.name}' was rejected because driver was not found at '${driverPath}' or it is not correct executable")
      throw AppError("Cannot start browser because driver was not found at '${driverPath}' or it is not correct executable file")
    }
    
    if (crawlerDef !is StreamingCrawlerBase) {
      throw AppError("Cannot start because selected crawler '${crawlerDef.id()}' does not support streaming websites")
    }
    
    System.setProperty(browser.selectedDriver.propertyNameForSeleniumDriver, driverPath.toAbsolutePath().toString())
    
    try {
      return (crawlerDef.build(config, browser, browser.selectedDriver.webDriverBuilder as DriverBuilder<SeleniumDriver>, params) as StreamingWebsiteBase).use { crawler ->
        
        crawler.initialize()
        GlobalState.runningProcess.value = (crawler) as SeleniumCrawler<SettingsBase>
        log.info("Crawler '${crawler.def}' built")
        
        val result = movies.mapNotNull { m ->
          
          try {
            crawler.waitIfPaused()
            val url = crawler.findMovieDlUrlByTitleAndYear(m.title, m.year)
            delay(10000.milliseconds)
            if (url != null) {
              return@mapNotNull m to url
            }
            log.warn("No url found for '{}'", m.title)
          } catch (e: Exception) {
            log.error("Cannot search movie '{}'", m.title, e)
            delay(10000.milliseconds)
          }
          null
        }.toMap()
        
        log.info("Crawler '${crawler.def}' finished successfully")
        result
      }
    } catch (e: SessionNotCreatedException) {
      throw AppError("Cannot start browser: " + e.message, e)
    } catch (e: TerminationException) {
      log.info(e.message)
    } catch (e: Exception) {
      throw e
    } finally {
      GlobalState.runningProcess.value = null
    }
    return emptyMap()
  }
}
