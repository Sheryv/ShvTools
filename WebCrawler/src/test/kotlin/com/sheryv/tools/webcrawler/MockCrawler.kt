package com.sheryv.tools.webcrawler

import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.browser.BrowserTypes
import com.sheryv.tools.webcrawler.browser.DriverConfig
import com.sheryv.tools.webcrawler.browser.DriverTypes
import com.sheryv.tools.webcrawler.config.BrowserSettings
import com.sheryv.tools.webcrawler.config.CommonConfiguration
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.CrawlerAttributes
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.SeleniumCrawler
import com.sheryv.tools.webcrawler.process.base.model.ProcessParams
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.process.base.model.SimpleStep
import com.sheryv.tools.webcrawler.process.base.model.Step
import com.sheryv.tools.webcrawler.view.settings.SettingsPanelReader
import com.sheryv.tools.webcrawler.view.settings.SettingsViewRow
import com.sheryv.util.DateUtils
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.nio.file.Paths
import java.time.format.DateTimeFormatter

class MockCrawler(
  val exec: suspend MockCrawler.() -> Unit,
  configuration: Configuration,
  browser: BrowserConfig,
  def: CrawlerDefinition<SeleniumDriver, SettingsBase>,
  driver: SeleniumDriver,
  params: ProcessParams
) : SeleniumCrawler<SettingsBase>(configuration, browser, def, driver, params) {
  
  override fun getSteps(): List<Step<out Any, out Any>> {
    return listOf(SimpleStep("mock", { exec() }))
  }
  
  companion object {
    val browserConfig: BrowserConfig = BrowserConfig(
      BrowserTypes.OTHER,
      setOf(DriverConfig(DriverTypes.CHROME, Path.of("""WebCrawler\drivers\chromedriver-131.exe"""))),
      Path.of("""brave.exe"""),
      Path.of("""User Data"""),
      DriverTypes.CHROME
    )
    
    val config = Configuration(CommonConfiguration(), BrowserSettings(setOf(browserConfig)), settings = mutableSetOf())
    
    fun createMock(
      exec: suspend MockCrawler.() -> Unit,
      website: String,
      name: String = "Mock",
      configuration: Configuration = config,
      browser: BrowserConfig = browserConfig,
      driver: SeleniumDriver = createDriver(browserConfig),
      params: ProcessParams = ProcessParams()
    ): MockCrawler {
      try {
        return runBlocking {
          val def = Def(exec, website, name)
          val crawler = def.build(configuration, browser, driver, params)
          driver.initialize(crawler)
          for (step in crawler.getSteps()) {
            step.run()
          }
          crawler
        }
      } finally {
        driver.quit()
      }
    }
    
    fun createDriver(
      browser: BrowserConfig = browserConfig,
    ): SeleniumDriver {
      System.setProperty(browser.selectedDriver.propertyNameForSeleniumDriver, browser.currentDriver().path.toAbsolutePath().toString())
      
      return browser.selectedDriver.webDriverBuilder.build(config, browser) as SeleniumDriver
    }
    
    class Def(val exec: suspend MockCrawler.() -> Unit, website: String, name: String = "Mock") :
      CrawlerDefinition<SeleniumDriver, SettingsBase>(
        CrawlerAttributes("mock", name, website),
        SettingsBase::class.java,
        SeleniumDriver::class.java
      ) {
      override fun createDefaultSettings(): SettingsBase {
        return object : SettingsBase() {
          override val crawlerId: String
            get() = "mock"
          override val outputPath: Path
            get() = Paths.get("mock_${DateUtils.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.json")
          
          override fun copyAll(): SettingsBase {
            TODO("Not yet implemented")
          }
          
          override fun buildSettingsPanelDef(): Pair<List<SettingsViewRow<*>>, SettingsPanelReader> {
            TODO("Not yet implemented")
          }
          
        }
      }
      
      val settings = createDefaultSettings()
      
      override fun build(
        configuration: Configuration,
        browser: BrowserConfig,
        driver: SeleniumDriver,
        params: ProcessParams
      ): MockCrawler {
        if (!configuration.settings.contains(settings)) {
          configuration.settings.add(settings)
        }
        return MockCrawler(exec, configuration, browser, this, driver, params)
      }
    }
  }
}
