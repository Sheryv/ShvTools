package com.sheryv.tools.websitescrapper.browser

import com.sheryv.tools.websitescrapper.SystemType
import com.sheryv.tools.websitescrapper.SystemUtils
import com.sheryv.tools.websitescrapper.utils.DriverUtils
import com.sheryv.tools.websitescrapper.utils.lg
import java.io.File


enum class BrowserType(
  val title: String,
  val defaultDriverDef: DriverDef? = null,
  val defaultUserProfilePathProvider: (BrowserType) -> String? = { null }
) {
  FIREFOX("Mozilla Firefox", DriverDef(DriverType.FIREFOX, BrowserDef.driverPath("gecko", DriverType.FIREFOX)), DriverUtils::findUserDataPathForBrowser),
  CHROME("Google Chrome", DriverDef(DriverType.CHROME, BrowserDef.driverPath("chrome", DriverType.CHROME)), DriverUtils::findUserDataPathForBrowser),
  EDGE("Microsoft Edge", DriverDef(DriverType.EDGE, BrowserDef.driverPath("msedge", DriverType.EDGE)), DriverUtils::findUserDataPathForBrowser),
  BRAVE("Brave", DriverDef(DriverType.CHROME, BrowserDef.driverPath("chrome", DriverType.CHROME)), DriverUtils::findUserDataPathForBrowser),
  OTHER("- Custom configuration -"),
  ;
  
  fun toDefWithDefaults(
    defaultRegistryKeyPart: String?,
    windowsPathPrefix: String,
    linuxPathPrefix: String,
    macPathPrefix: String,
    driverDef: DriverDef? = defaultDriverDef
  ): BrowserDef? {
    if (driverDef == null) {
      throw IllegalArgumentException("Driver is required")
    }
    
    val file = File(driverDef.path)
    if (!file.exists() || !file.isFile) {
      lg().info("Browser '${name}' rejected because driver not found at '${driverDef.path}'")
      return null
    }
    
    val system = SystemUtils.currentSystem()
    val prefix = when (system) {
      SystemType.WINDOWS -> windowsPathPrefix
      SystemType.LINUX -> linuxPathPrefix
      SystemType.MAC -> macPathPrefix
    }
    val tempPaths = SystemUtils.buildDefaultBrowserPaths(system, prefix)
    
    val path = SystemUtils.findBrowserPath(defaultRegistryKeyPart, tempPaths) ?: return null
    
    
    return BrowserDef(this, path, driverDef)
  }
  
  override fun toString(): String {
    return title
  }
  
  companion object {
    fun prepareDefaults(): List<BrowserDef> {
      return listOfNotNull(
        CHROME.toDefWithDefaults(
          BrowserDef.registryKey("ChromeHTML"),
          "Google\\Chrome\\Application\\chrome.exe",
          "google-chrome",
          "google-chrome",
        ),
        FIREFOX.toDefWithDefaults(
          BrowserDef.registryKey("FirefoxHTML"),
          "Mozilla Firefox\\firefox.exe",
          "firefox",
          "firefox",
        ),
        EDGE.toDefWithDefaults(
          BrowserDef.registryKey("MSEdgeHTM"),
          "Microsoft\\Edge\\Application\\msedge.exe",
          "msedge",
          "msedge",
        ),
        BRAVE.toDefWithDefaults(
          BrowserDef.registryKey("BraveHTML"),
          "BraveSoftware\\Brave-Browser\\Application\\brave.exe",
          "brave",
          "brave",
        ),
      )
    }
  }
}
