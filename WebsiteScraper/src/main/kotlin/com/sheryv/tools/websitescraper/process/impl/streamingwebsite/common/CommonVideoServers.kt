package com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common

import com.sheryv.tools.websitescraper.config.impl.streamingwebsite.VideoServerConfig
import org.openqa.selenium.By

enum class CommonVideoServers(
  private val label: String,
  private val searchTerm: String,
  private val innerIframeCssSelector: String? = null,
  private val scriptToActivatePlayer: String? = null
) : VideoServerDefinition {
  HIGHLOAD("Highload", "highload"),
  VIDOZA("Vidoza", "vidoza"),
  UPSTREAM("Upstream", "upstream"),
  
  ;
  
  override fun id() = name.lowercase()
  
  override fun innerIframeCssSelector(): By? = innerIframeCssSelector?.let { By.cssSelector(it) }
  
  override fun scriptToActivatePlayer() = scriptToActivatePlayer
  
  override fun label() = label
  
  override fun searchTerm() = searchTerm
  
  override fun toConfig(): VideoServerConfig = VideoServerConfig(label, searchTerm)
  
  companion object {
    fun forName(name: String) = values().first { it.id().equals(name, true) }
  }
}
