package com.sheryv.tools.websitescrapper.browser

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
  }
}
