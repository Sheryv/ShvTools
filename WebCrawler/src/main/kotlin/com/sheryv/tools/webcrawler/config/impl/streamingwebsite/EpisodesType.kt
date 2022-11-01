package com.sheryv.tools.webcrawler.config.impl.streamingwebsite

import com.sheryv.tools.webcrawler.config.impl.ApplicableEntry
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.EpisodeAudioTypes

data class EpisodeType(val kind: EpisodeAudioTypes, override val enabled: Boolean = true) : ApplicableEntry {
  override fun changeActivation(isEnabled: Boolean): ApplicableEntry {
    return copy(enabled = isEnabled)
  }
  
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    other as EpisodeType
    
    if (kind != other.kind) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    return kind.hashCode()
  }
  
  companion object {
    @JvmStatic
    
    fun all() = EpisodeAudioTypes.values().sortedBy { it.priority }.map { EpisodeType(it) }
  }
}
