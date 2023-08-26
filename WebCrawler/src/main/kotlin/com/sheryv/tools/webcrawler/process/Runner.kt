package com.sheryv.tools.webcrawler.process

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.CrawlerDef
import com.sheryv.tools.webcrawler.process.base.SeleniumCrawler
import com.sheryv.tools.webcrawler.process.base.model.*
import com.sheryv.tools.webcrawler.utils.AppError
import com.sheryv.util.logging.log
import org.openqa.selenium.SessionNotCreatedException
import kotlin.io.path.exists
import kotlin.io.path.isExecutable


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
    
    var driver: SDriver? = null
    try {
      driver = browser.selectedDriver.webDriverBuilder.build(config, browser)
      val crawler = crawlerDef.build(config, browser, driver, params)
      
      driver.initialize(crawler)
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
    } catch (e: SessionNotCreatedException) {
      throw AppError("Cannot start browser: " + e.message, e)
    } catch (e: TerminationException) {
      log.info(e.message)
    } catch (e: Exception) {
      throw e
    } finally {
      GlobalState.runningProcess.value = null
      (driver as? SeleniumDriver)?.quit()
    }
  }
}
