package com.sheryv.tools.webcrawler.browser

import java.nio.file.Path


data class DriverConfig(val type: DriverTypes, var path: Path) {
  
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    other as DriverConfig
    
    if (type != other.type) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    return type.hashCode()
  }
}
