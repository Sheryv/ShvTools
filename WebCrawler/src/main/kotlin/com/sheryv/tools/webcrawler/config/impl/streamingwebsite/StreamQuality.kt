package com.sheryv.tools.webcrawler.config.impl.streamingwebsite

import com.sheryv.tools.webcrawler.config.impl.ApplicableEntry
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Qualities

data class StreamQuality(val kind: Qualities, override val enabled: Boolean = true) : ApplicableEntry {
  
  override fun changeActivation(isEnabled: Boolean): ApplicableEntry {
    return copy(enabled = isEnabled)
  }
  
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    other as StreamQuality
    
    if (kind != other.kind) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    return kind.hashCode()
  }
  
  
  companion object {
    @JvmStatic
    fun all() = Qualities.values().sortedBy { it.priority }.map { StreamQuality(it) }
  }
}
