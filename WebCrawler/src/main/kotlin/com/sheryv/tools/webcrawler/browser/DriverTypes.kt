package com.sheryv.tools.webcrawler.browser

import com.sheryv.tools.webcrawler.service.SystemSupport
import com.sheryv.tools.webcrawler.service.SystemType
import java.nio.file.Path

enum class DriverTypes(
  val title: String,
  val fileName: String,
  val propertyNameForSeleniumDriver: String,
  val downloadUrl: String,
  val webDriverBuilder: DriverBuilder
) {
  CHROME(
    "Chromium based (Google Chrome)",
    "chromedriver",
    "webdriver.chrome.driver",
    "https://chromedriver.chromium.org/downloads",
    ChromeDriverBuilder()
  ),
  FIREFOX(
    "Gecko (Firefox)",
    "geckodriver",
    "webdriver.gecko.driver",
    "https://github.com/mozilla/geckodriver/releases",
    FirefoxDriverBuilder()
  ),
  EDGE(
    "Microsoft Edge",
    "msedgedriver",
    "webdriver.edge.driver",
    "https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/#downloads",
    EdgeDriverBuilder()
  );
  
  fun toConfig(): DriverConfig {
    return DriverConfig(this, defaultDriverPath())
  }
  
  fun defaultDriverPath(): Path {
    return SystemSupport.get.findExecutablePath(fileName) ?: Path.of("drivers", formattedFileName())
  }
  
  fun formattedFileName(browserVersion: String? = null): String {
    val v = browserVersion?.let { "-$it" }.orEmpty()
    
    return if (SystemSupport.currentSystem == SystemType.WINDOWS)
      "$fileName$v.exe"
    else
      fileName + v
  }
  
  override fun toString(): String {
    return title
  }
}
