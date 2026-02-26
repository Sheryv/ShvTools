package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common

import com.sheryv.tools.webcrawler.process.base.SeleniumCrawler
import org.openqa.selenium.By

private const val JW_PLAYER_ACTIVATION_SCRIPT =
  "document.querySelector('.jw-controls .jw-button-color').click();document.querySelector('button[data-plyr=play]')?.click();"
private const val JW_PLAYER_CHECK_READY_SCRIPT =
  "document.querySelector('.jw-controls .jw-button-color') != null"

enum class CommonVideoServers(
  private val label: String,
  private val domains: List<String>,
  private val scriptToActivatePlayer: String? = null,
  private val scriptToCheckPlayerReady: String? = null,
  private val searchTerm: Regex = Regex(Regex.escape(label.lowercase())),
  private val innerIframeCssSelector: String? = null,
  override val isStreaming: Boolean = true,
  private val m3u8ManifestFileName: String = "master.m3u8"
) : VideoServerDefinition {
  HIGHLOAD(
    "Highload",
    listOf("highload.to"),
    "document.querySelector('#videerlay')?.click();document.querySelector('.vjs-control-bar button.vjs-play-control')?.click()",
    "document.querySelector('#videerlay') != null",
  ),
  UPSTREAM(
    "Upstream",
    listOf("upstream.to"),
  ),
  VOE(
    "Voe",
    listOf("voe.sx", "lauradaydo.com"),
    "$JW_PLAYER_ACTIVATION_SCRIPT;document.querySelector('.voe-play')?.click()",
    JW_PLAYER_CHECK_READY_SCRIPT
  ),
  VTUBE(
    "VTube",
    listOf("vtbe.to"),
    JW_PLAYER_ACTIVATION_SCRIPT,
    JW_PLAYER_CHECK_READY_SCRIPT,
    innerIframeCssSelector = "#pframe",
  ),
  VIDOZA(
    "Vidoza",
    listOf("vidoza.net"),
    isStreaming = false
  ),
  EMBEDO(
    "Embedo",
    listOf("embedo.co"),
    "document.querySelector('#videerlay')?.click();document.querySelector('.vjs-control-bar button.vjs-play-control')?.click()",
    "document.querySelector('#videerlay') != null"
  ),
  USERLOAD(
    "Userload",
    listOf("userload.co"),
    "document.querySelector('#videooverlay')?.click()",
    "document.querySelector('#videooverlay') != null"
  ),
  STREAMTAPE(
    "Streamtape",
    listOf("streamtape.com"),
    "document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click()",
    "document.querySelector('.play-overlay') != null"
  ),
  VIDGUARD(
    "Vidguard",
    listOf("embedv.net"),
    JW_PLAYER_ACTIVATION_SCRIPT,
    JW_PLAYER_CHECK_READY_SCRIPT,
  ),
  VIDSTREAM(
    "Vidstream",
    listOf("vidstream.pro"),
//    "#player iframe",
//    "document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click()"
  ),
  MYCLOUD(
    "MyCloud",
    listOf("mcloud.to")
//    "#player iframe",
//    "document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click()"
  ),
  FILEMOON(
    "Filemoon",
    listOf("filemoon.sx")
//    "#player iframe",
//    "document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click()"
  ),
  LULUVDO(
    "Luluvdo",
    listOf("luluvdo.com"),
    JW_PLAYER_ACTIVATION_SCRIPT,
    JW_PLAYER_CHECK_READY_SCRIPT,
  ),
  VIDMOLY(
    "vidmoly",
    listOf("vidmoly.biz"),
    JW_PLAYER_ACTIVATION_SCRIPT,
    JW_PLAYER_CHECK_READY_SCRIPT,
  ),
  ;
  
  override fun id() = name.lowercase()
  
  override fun innerIframeCssSelector(): By? = innerIframeCssSelector?.let { By.cssSelector(it) }
  
  override fun label() = label
  
  override fun domains() = domains
  
  override fun searchTerm() = searchTerm
  
  override fun isUrlMatchingRequestWithM3U8Manifest(url: String): Boolean {
    if (isStreaming) {
      return url.contains(m3u8ManifestFileName)
    }
    return true
  }
  
  override suspend fun activatePlayer(crawler: SeleniumCrawler<*>): Boolean {
    if (scriptToCheckPlayerReady == null) return false
    
    if (scriptToActivatePlayer != null) {
      
      val script = if (!scriptToCheckPlayerReady.startsWith("return ")) "return $scriptToActivatePlayer" else scriptToActivatePlayer
      
      if (crawler.driver.executeScript(script) == true) {
        crawler.driver.executeScript(scriptToActivatePlayer)
      }
    }
    return false
  }
  
  companion object {
    fun forName(name: String) = values().first { it.id().equals(name, true) }
    
  }
}
