package com.sheryv.tools.websitescraper.browser

class BrowserRegistry {
  private val registry: MutableMap<BrowserType, BrowserDef> = mutableMapOf()
  
  fun register(browser: BrowserDef) {
    if (registry.containsKey(browser.type))
      throw IllegalArgumentException("Browser with type $browser already exists in registry")
    
    registry[browser.type] = browser
  }
  
  fun get(type: BrowserType): BrowserDef? {
    return registry[type]
  }
  
  fun all() = registry.values.toSet()
  
  fun names() = registry.keys.toList()
  
  companion object {
    @JvmField
    val DEFAULT = BrowserRegistry()
    
    fun fill(registry: BrowserRegistry): BrowserRegistry {
      BASIC.forEach { registry.register(it) }
      return registry
    }
    
    @JvmStatic
    private val BASIC = listOfNotNull(
      BrowserType.CHROME.toDefWithDefaults(
        BrowserDef.registryKey("ChromeHTML"),
        "Google\\Chrome\\Application\\chrome.exe",
        "google-chrome",
        "google-chrome",
      ),
      BrowserType.FIREFOX.toDefWithDefaults(
        BrowserDef.registryKey("FirefoxHTML"),
        "Mozilla Firefox\\firefox.exe",
        "firefox",
        "firefox",
      ),
      BrowserType.EDGE.toDefWithDefaults(
        BrowserDef.registryKey("MSEdgeHTM"),
        "Microsoft\\Edge\\Application\\msedge.exe",
        "msedge",
        "msedge",
      ),
      BrowserType.BRAVE.toDefWithDefaults(
        BrowserDef.registryKey("BraveHTML"),
        "BraveSoftware\\Brave-Browser\\Application\\brave.exe",
        "brave",
        "brave",
      ),
    )
  }
}
