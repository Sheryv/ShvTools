package com.sheryv.tools.websitescrapper.process

import com.sheryv.tools.websitescrapper.browser.BrowserDef
import com.sheryv.tools.websitescrapper.config.Configuration
import com.sheryv.tools.websitescrapper.process.base.Scraper
import com.sheryv.tools.websitescrapper.process.base.ScraperDef
import com.sheryv.tools.websitescrapper.process.base.model.SDriver
import com.sheryv.tools.websitescrapper.process.base.model.SeleniumDriver
import com.sheryv.tools.websitescrapper.process.base.model.Step
import com.sheryv.tools.websitescrapper.utils.AppError
import com.sheryv.tools.websitescrapper.utils.lg
import org.openqa.selenium.SessionNotCreatedException


class Runner(
  private val configuration: Configuration,
  private val browser: BrowserDef,
  private val scraperDef: ScraperDef<in SDriver>
) {
  
  fun start() {
    val config = configuration.copy()
    System.setProperty(browser.driverDef.type.propertyNameForPath, browser.driverDef.path)
    
    var driver: SDriver? = null
    try {
      driver = browser.driverDef.type.webDriverBuilder.build(config, browser)
      val scrapper = scraperDef.build(config, browser, driver)
      driver.initialize(scrapper)
      lg().info("Scrapper '${scrapper.def.name}' [${scrapper.def.id}] built")
      
      val steps: List<Step<Any>> = scrapper.getSteps() as List<Step<Any>>
      var any: Any? = null
      for (step in steps) {
        lg().info("Running step '${step.name}' by '${scrapper.def.name}' [${scrapper.def.id}]")
        any = step.runBlock(any)
      }
      lg().info("Scrapper '${scrapper.def.name}' [${scrapper.def.id}] finished successfully")
    } catch (e: SessionNotCreatedException) {
      throw AppError("Cannot start browser: " + e.message, e)
    } catch (e: Exception) {
      throw e
    } finally {
      (driver as? SeleniumDriver)?.quit()
    }
  }
}
