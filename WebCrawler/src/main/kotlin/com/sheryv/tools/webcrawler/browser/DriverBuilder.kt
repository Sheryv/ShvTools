package com.sheryv.tools.webcrawler.browser

import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.process.base.model.SDriver
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.utils.lg
import org.openqa.selenium.MutableCapabilities
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.FirefoxProfile
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.logging.LoggingPreferences
import java.io.File
import java.util.logging.Level

abstract class DriverBuilder {
  abstract fun build(config: Configuration, browser: BrowserDef): SDriver
  
  protected fun setDefaults(options: MutableCapabilities, config: Configuration, browser: BrowserDef) {
    if (config.useUserProfile == true) {
      val userDataPath = browser.type.defaultUserProfilePathProvider(browser.type)
      if (userDataPath != null) {
        when (options) {
          is ChromeOptions -> {
            options.addArguments("--user-data-dir=$userDataPath")
            lg().info(
              "Using user profile from '{}'. Driver options class: {}, browser: {}",
              userDataPath,
              options::class.java,
              browser.type
            )
          }
          is FirefoxOptions -> {
            options.profile = FirefoxProfile(File(userDataPath))
            lg().info(
              "Using user profile from '{}'. Driver options class: {}, browser: {}",
              userDataPath,
              options::class.java,
              browser.type
            )
          }
          else -> {
            lg().warn(
              "User data/profile directory not used because driver is not supported. Driver options class: {}, browser: {}",
              options::class.java,
              browser.type
            )
          }
        }
      } else {
        lg().warn(
          "User data/profile directory not used because it was not found. Driver options class: {}, browser: {}",
          options::class.java,
          browser.type
        )
      }
    }
    
    options.setCapability("applicationCacheEnabled", true)
  }
}

class FirefoxDriverBuilder : DriverBuilder() {
  override fun build(config: Configuration, browser: BrowserDef): SDriver {
    val options = FirefoxOptions()
    setDefaults(options, config, browser)
    options.setBinary(browser.binaryPath)
    return SeleniumDriver(FirefoxDriver(options))
  }
  
}

class ChromeDriverBuilder : DriverBuilder() {
  override fun build(config: Configuration, browser: BrowserDef): SDriver {
    val options = ChromeOptions()
  
    val logPrefs = LoggingPreferences()
    logPrefs.enable(LogType.PERFORMANCE, Level.ALL)
    options.setCapability(ChromeOptions.LOGGING_PREFS, logPrefs)
    
    setDefaults(options, config, browser)
    options.setBinary(browser.binaryPath)
    return SeleniumDriver(ChromeDriver(options))
  }
  
}

class EdgeDriverBuilder : DriverBuilder() {
  override fun build(config: Configuration, browser: BrowserDef): SDriver {
    val options = EdgeOptions()
  
    val logPrefs = LoggingPreferences()
    logPrefs.enable(LogType.PERFORMANCE, Level.ALL)
    options.setCapability(EdgeOptions.LOGGING_PREFS, logPrefs)
    
    setDefaults(options, config, browser)
    options.setBinary(browser.binaryPath)
    return SeleniumDriver(EdgeDriver(options))
  }
  
}
