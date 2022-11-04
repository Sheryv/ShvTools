package com.sheryv.tools.webcrawler.config.impl.streamingwebsite

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sheryv.tools.webcrawler.config.impl.ApplicableEntry
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.VideoServerDefinition
import com.sheryv.tools.webcrawler.service.Registry

data class VideoServerConfig(
  val id: String,
  override val enabled: Boolean = true
) : ApplicableEntry {
  
  @JsonIgnore
  var definition: VideoServerDefinition = Registry.get().serverDefinitions().first { it.id() == id }
  
  override fun changeActivation(isEnabled: Boolean): ApplicableEntry {
    return copy(enabled = isEnabled)
  }
  
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    other as VideoServerConfig
    
    if (id != other.id) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    return id.hashCode()
  }
  
  companion object {
    @JvmStatic
    fun all() = Registry.get().serverDefinitions().map { VideoServerConfig(it.id()) }
  }
}
