package com.sheryv.tools.webcrawler.config.impl.streamingwebsite

import com.sheryv.tools.webcrawler.config.impl.ApplicableEntry
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.CommonVideoServers

data class VideoServerConfig(
  val name: String,
  val searchTerm: String = name,
  override val enabled: Boolean = true
) : ApplicableEntry {
  
  override fun changeActivation(isEnabled: Boolean): ApplicableEntry {
    return copy(enabled = isEnabled)
  }
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    other as VideoServerConfig
    
    if (name != other.name) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    return name.hashCode()
  }
  
  companion object {
    @JvmStatic
    fun all() = CommonVideoServers.values().map { VideoServerConfig(it.label(), it.searchTerm()) }
  }
  
}
