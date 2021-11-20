package com.sheryv.tools.websitescrapper.browser

enum class DriverType(val title: String, val propertyNameForPath: String, val propertyNameForVersion: String, val webDriverBuilder: DriverBuilder) {
  FIREFOX("Gecko (Firefox)", "webdriver.gecko.driver","firefox.driver.version", FirefoxDriverBuilder()),
  CHROME("Google Chrome / Chromium", "webdriver.chrome.driver","chrome.driver.version", ChromeDriverBuilder()),
  EDGE("Microsoft Edge", "webdriver.edge.driver", "edge.driver.version", EdgeDriverBuilder()), ;
  
  override fun toString(): String {
    return title
  }
}
