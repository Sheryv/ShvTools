package com.sheryv.tools.webcrawler.browser

import com.sheryv.tools.webcrawler.service.BrowserSupport
import com.sheryv.tools.webcrawler.service.SystemType
import java.nio.file.Path


enum class BrowserTypes(
  val title: String,
  val pathParts: BrowserDefaultPathParts,
  vararg val supportedDrivers: DriverTypes,
) {
  CHROME(
    "Google Chrome", BrowserDefaultPathParts(
      mapOf(
        SystemType.WINDOWS to "Google\\Chrome\\Application\\chrome.exe",
        SystemType.LINUX to "google-chrome",
        SystemType.MAC to "google-chrome",
      ), "ChromeHTML"
    ), DriverTypes.CHROME
  ),
  FIREFOX(
    "Mozilla Firefox", BrowserDefaultPathParts(
      mapOf(
        SystemType.WINDOWS to "Mozilla Firefox\\firefox.exe",
        SystemType.LINUX to "firefox",
        SystemType.MAC to "firefox",
      ), "FirefoxHTML"
    ), DriverTypes.FIREFOX
  ),
  BRAVE(
    "Brave", BrowserDefaultPathParts(
      mapOf(
        SystemType.WINDOWS to "BraveSoftware\\Brave-Browser\\Application\\brave.exe",
        SystemType.LINUX to "brave",
        SystemType.MAC to "brave",
      ), "BraveHTML"
    ), DriverTypes.CHROME
  ),
  EDGE(
    "Microsoft Edge", BrowserDefaultPathParts(
      mapOf(
        SystemType.WINDOWS to "Microsoft\\Edge\\Application\\msedge.exe",
        SystemType.LINUX to "msedge",
        SystemType.MAC to "msedge",
      ), "MSEdgeHTM"
    ), DriverTypes.EDGE
  ),
  OTHER("- Custom configuration -", BrowserDefaultPathParts(), *DriverTypes.values()),
  ;
  
  fun toConfig(): BrowserConfig {
    return BrowserConfig(this, supportedDrivers.map { it.toConfig() }.toSet(), BrowserSupport.get.findBrowserPath(this))
  }
  
  fun getPathForUserProfileInBrowser(browser: BrowserConfig): Path? {
    return BrowserSupport.get.getPathForUserProfileInBrowser(browser, this)
  }
  
  override fun toString(): String {
    return title
  }
  
}

data class BrowserDefaultPathParts(
  val prefixes: Map<SystemType, String> = emptyMap(),
  private val defaultRegistryKeyPart: String? = null
) {
  fun registryKey() = defaultRegistryKeyPart?.let { "HKCR\\${it}\\shell\\open\\command" }
}
