package com.sheryv.tools.webcrawler.browser

import com.sheryv.tools.webcrawler.SystemType
import com.sheryv.tools.webcrawler.SystemUtils
import com.sheryv.tools.webcrawler.utils.DriverUtils


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
}
