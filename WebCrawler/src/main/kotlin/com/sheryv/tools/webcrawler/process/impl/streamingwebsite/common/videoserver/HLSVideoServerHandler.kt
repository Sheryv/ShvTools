package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.videoserver

import com.sheryv.tools.webcrawler.process.base.SeleniumCrawler
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.StreamingWebsiteBase
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.FileFormats
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Series
import com.sheryv.util.io.HttpSupport
import com.sheryv.util.logging.log
import com.sheryv.util.singleAssign
import org.openqa.selenium.By

open class HLSVideoServerHandler(
  def: VideoServerDefinition,
  scraper:StreamingWebsiteBase,
  scriptToActivatePlayer: String? = null,
  scriptToCheckPlayerReady: String? = null,
  innerIframeCssSelector: By? = null,
  overrideFileFormat: FileFormats? = null,
) : VideoServerHandler<VideoServerDefinition>(def, scraper,scriptToActivatePlayer, scriptToCheckPlayerReady, innerIframeCssSelector, overrideFileFormat) {
  
  init {
    require(def.isHLS) { "$javaClass supports only HLS servers" }
  }
  
  protected val m3u8ManifestFileName = "master.m3u8"
  
  open fun isUrlMatchingRequestWithM3U8Manifest(url: String): Boolean {
    return url.contains(m3u8ManifestFileName)
  }
  
  open fun checkIfM3U8UrlCorrect(url: String, series: Series): Boolean = series.episodes.none { it.downloadUrl?.isSameUrl(url) == true }
  
  open fun tryToGetCorrectM3U8Url(incorrectUrl: String, series: Series): String? {
    if (incorrectUrl.contains(m3u8ManifestFileName)) {
      val prefix = incorrectUrl.substringBefore(m3u8ManifestFileName)
      val file = HttpSupport(securityEnabled = false).sendGet(incorrectUrl).body()

//      val file = HttpClients.createDefault().use { httpclient ->
//        val httpGet = ClassicRequestBuilder.get(incorrectUrl).build()
//
//        httpclient.execute(httpGet) { response: ClassicHttpResponse ->
//          EntityUtils.toString(response.entity)
//        }
//      }

//      val file = HttpSupport(false).sendGet(incorrectUrl).body()
      if (file.startsWith("#EXTM3U")) {
        return file.lines().firstOrNull { it.startsWith("index") && checkIfM3U8UrlCorrect(prefix + it, series) }?.let { prefix + it }
      }
    }
    return null
  }
  
  open fun getM3U8UrlFromEvents(): String? {
    val url = try {
      val events = scraper.getNetworkResponseEventsFromBrowserTools().filter { it.response?.mimeType == "application/vnd.apple.mpegurl" }.toList()
      val m3u8events = events.filter { it.response!!.url.contains(".m3u8") }
      val index = m3u8events.indexOfLast { checkIfM3U8UrlCorrect(it.response!!.url, scraper.series) }
      if (events.isNotEmpty()) {
        log.debug("Filtered stream urls from browser tools (matching: ${events.size})\n" + m3u8events.mapIndexed { i, e ->
          (if (i == index) "[#] " else "[ ] ") + e.response!!.url
        }.joinToString("\n"))
      }
      
      m3u8events.getOrNull(index)?.response?.url ?: m3u8events.firstNotNullOfOrNull {
        tryToGetCorrectM3U8Url(
          it.response?.url!!,
          scraper.series
        )
      }?.also {
        log.debug("Found alternative url from browser tools: $it")
      }
    } catch (e: IllegalArgumentException) {
      null
    }
    if (url == null) {
      val events = scraper.getNetworkResponseEventsFromJS()
      val m3u8events = events.filter { it.name.contains(".m3u8") }
      val index = m3u8events.indexOfLast { checkIfM3U8UrlCorrect(it.name, scraper.series) }
      if (events.isNotEmpty()) {
        log.debug("Filtered stream urls from JavaScript performance objects (all: ${events.size})\n" + m3u8events.mapIndexed { i, e ->
          (if (i == index) "[#] " else "[ ] ") + e.name
        }.joinToString("\n"))
      }
      
      return m3u8events.getOrNull(index)?.name ?: m3u8events
        .firstNotNullOfOrNull { tryToGetCorrectM3U8Url(it.name, scraper.series) }
        ?.also {
          log.debug("Found alternative url from JavaScript performance objects: $it")
        }
    }
    return url
  }
}
