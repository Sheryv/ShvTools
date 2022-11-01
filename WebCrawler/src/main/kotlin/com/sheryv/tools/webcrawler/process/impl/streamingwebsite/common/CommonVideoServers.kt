package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common

import org.openqa.selenium.By

enum class CommonVideoServers(
  private val label: String,
  private val searchTerm: String,
  private val innerIframeCssSelector: String? = null,
  private val scriptToActivatePlayer: String? = null
) : VideoServerDefinition {
  HIGHLOAD(
    "highload.to",
    "highload",
    null,
    "document.querySelector('#videerlay')?.click();document.querySelector('.vjs-control-bar button.vjs-play-control')?.click()"
  ),
  UPSTREAM("upstream.to", "upstream"),
  VOE("voe.sx", "voe", null, "document.querySelector('button[data-plyr=play]')?.click()"),
  VIDOZA("vidoza", "vidoza"),
  EMBEDO(
    "embedo.co",
    "embedo",
    null,
    "document.querySelector('#videerlay')?.click();document.querySelector('.vjs-control-bar button.vjs-play-control')?.click()"
  ),
  USERLOAD("userload.co", "userload", null, "document.querySelector('#videooverlay')?.click()"),
  STREAMTAPE(
    "streamtape.com",
    "streamtape",
    null,
    "document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click()"
  ),
  
  ;
  
  override fun id() = name.lowercase()
  
  override fun innerIframeCssSelector(): By? = innerIframeCssSelector?.let { By.cssSelector(it) }
  
  override fun scriptToActivatePlayer() = scriptToActivatePlayer
  
  override fun label() = label
  
  override fun searchTerm() = searchTerm
  
  companion object {
    fun forName(name: String) = values().first { it.id().equals(name, true) }
  }
}
