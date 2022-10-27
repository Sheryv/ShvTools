package com.sheryv.tools.websitescraper.browser

import com.sheryv.tools.websitescraper.config.Configuration
import java.io.File

class BrowserDef(
  val type: BrowserType,
  var binaryPath: String? = null,
  val driverDef: DriverDef,
) {
  
  companion object {
    @JvmStatic
    fun driverPath(prefix: String, driverType: DriverType): String {
      val version = Configuration.property(driverType.propertyNameForVersion)
        ?.let { if (it.isNotBlank()) "-$it" else "" }
        .orEmpty()
      
      return File("drivers/${prefix.lowercase()}driver$version.exe").absolutePath
    }
    
    @JvmStatic
    fun registryKey(name: String) = "HKCR\\${name}\\shell\\open\\command"
  }
  
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    other as BrowserDef
    
    if (type != other.type) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    return type.hashCode()
  }
  
  override fun toString(): String {
    return type.title
  }
}
