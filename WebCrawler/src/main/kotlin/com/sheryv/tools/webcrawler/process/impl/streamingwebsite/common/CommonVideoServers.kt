package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common

import org.openqa.selenium.By

enum class CommonVideoServers(
  private val label: String,
  private val searchTerm: String,
  private val domain: String,
  private val innerIframeCssSelector: String? = null,
  private val scriptToActivatePlayer: String? = null
) : VideoServerDefinition {
  HIGHLOAD(
    "Highload",
    "highload",
    "highload.to",
    null,
    "document.querySelector('#videerlay')?.click();document.querySelector('.vjs-control-bar button.vjs-play-control')?.click()"
  ),
  UPSTREAM(
    "Upstream",
    "upstream",
    "upstream.to",
  ),
  VOE(
    "Voe",
    "voe",
    "voe.sx",
    null, "document.querySelector('button[data-plyr=play]')?.click()"
  ),
  VTUBE(
    "VTube",
    "vtube",
    "vtbe.to",
    "#pframe", "document.querySelector('.jw-controls .jw-button-color')?.click()"
  ),
  VIDOZA(
    "Vidoza",
    "vidoza",
    "vidoza.net",
  ),
  EMBEDO(
    "Embedo",
    "embedo",
    "embedo.co",
    null,
    "document.querySelector('#videerlay')?.click();document.querySelector('.vjs-control-bar button.vjs-play-control')?.click()"
  ),
  USERLOAD(
    "Userload",
    "userload",
    "userload.co",
    null,
    "document.querySelector('#videooverlay')?.click()"
  ),
  STREAMTAPE(
    "Streamtape",
    "streamtape",
    "streamtape.com",
    null,
    "document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click()"
  ),
  VIDSTREAM(
    "Vidstream",
    "vidstream",
    "vidstream.pro",
//    "#player iframe",
//    "document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click()"
  ),
  MYCLOUD(
    "MyCloud",
    "mycloud",
    "mcloud.to"
//    "#player iframe",
//    "document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click()"
  ),
  FILEMOON(
    "Filemoon",
    "filemoon",
    "filemoon.sx"
//    "#player iframe",
//    "document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click()"
  ),
  
  ;
  
  override fun id() = name.lowercase()
  
  override fun innerIframeCssSelector(): By? = innerIframeCssSelector?.let { By.cssSelector(it) }
  
  override fun scriptToActivatePlayer() = scriptToActivatePlayer
  
  override fun label() = label
  
  override fun domain() = domain
  
  override fun searchTerm() = searchTerm
  
  companion object {
    fun forName(name: String) = values().first { it.id().equals(name, true) }
  }
}
