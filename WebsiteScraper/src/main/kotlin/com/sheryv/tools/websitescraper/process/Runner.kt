package com.sheryv.tools.websitescraper.process

import com.sheryv.tools.websitescraper.browser.BrowserDef
import com.sheryv.tools.websitescraper.config.Configuration
import com.sheryv.tools.websitescraper.config.SettingsBase
import com.sheryv.tools.websitescraper.process.base.ScraperDef
import com.sheryv.tools.websitescraper.process.base.model.SDriver
import com.sheryv.tools.websitescraper.process.base.model.SeleniumDriver
import com.sheryv.tools.websitescraper.process.base.model.Step
import com.sheryv.tools.websitescraper.utils.AppError
import com.sheryv.tools.websitescraper.utils.lg
import org.openqa.selenium.SessionNotCreatedException
import java.io.File


class Runner(
  private val configuration: Configuration,
  private val browser: BrowserDef,
  private val scraperDef: ScraperDef
) {
  
  suspend fun prepare(settings: SettingsBase) {
    settings.validate(scraperDef)
    configuration.settings[scraperDef.id] = settings
  }
  
  suspend fun start() {
    val config = configuration.copy()
    val file = File(browser.driverDef.path)
    if (!file.exists() || !file.isFile) {
      lg().info("Browser configuration '${browser.type.name}' was rejected because driver was not found at '${browser.driverDef.path}'")
      throw AppError("Cannot start browser because driver was not found at '${browser.driverDef.path}'")
    }
    
    System.setProperty(browser.driverDef.type.propertyNameForPath, browser.driverDef.path)
    
    var driver: SDriver? = null
    try {
      driver = browser.driverDef.type.webDriverBuilder.build(config, browser)
      val scrapper = scraperDef.build(config, browser, driver)
      
      driver.initialize(scrapper)
      lg().info("Scrapper '${scrapper.settings.name}' [${scrapper.def.id}] built")
      
      val steps = scrapper.getSteps() as List<Step<Any, Any>>
      var any: Any? = null
      for (step in steps) {
        lg().info("Running step '${step.name}' by '${scrapper.settings.name}' [${scrapper.def.id}]")
        any = step.run(any)
      }
      lg().info("Scrapper '${scrapper.settings.name}' [${scrapper.def.id}] finished successfully")
    } catch (e: SessionNotCreatedException) {
      throw AppError("Cannot start browser: " + e.message, e)
    } catch (e: Exception) {
      throw e
    } finally {
      (driver as? SeleniumDriver)?.quit()
    }
  }
}
