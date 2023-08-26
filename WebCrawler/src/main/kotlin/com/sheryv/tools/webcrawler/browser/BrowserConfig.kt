package com.sheryv.tools.webcrawler.browser

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.nio.file.Path
import java.util.LinkedHashSet

class BrowserConfig(
  val type: BrowserTypes,
  @JsonDeserialize(`as` = LinkedHashSet::class)
  val drivers: Set<DriverConfig>,
  var binaryPath: Path? = null,
  var selectedDriver: DriverTypes = drivers.first().type,
) {
  
  fun currentDriver() = drivers.first { it.type == selectedDriver }
  
  companion object {
    @JvmStatic
    fun all() = BrowserTypes.entries.map { it.toConfig() }.toSet()
  }
  
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    other as BrowserConfig
    
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
