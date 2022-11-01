package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.config.impl.streamingwebsite.VideoServerConfig
import com.sheryv.tools.webcrawler.process.base.ScraperDefinition
import com.sheryv.tools.webcrawler.process.base.SeleniumScraper
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.process.base.model.SimpleStep
import com.sheryv.tools.webcrawler.process.base.model.Step
import com.sheryv.tools.webcrawler.process.base.model.TerminationException
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.*
import com.sheryv.tools.webcrawler.utils.Utils
import com.sheryv.tools.webcrawler.utils.lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.openqa.selenium.By
import org.openqa.selenium.InvalidArgumentException
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

abstract class StreamingWebsiteBase(
  configuration: Configuration,
  browser: BrowserConfig,
  def: ScraperDefinition<SeleniumDriver, StreamingWebsiteSettings>,
  driver: SeleniumDriver
) : SeleniumScraper<StreamingWebsiteSettings>(configuration, browser, def, driver) {
  lateinit var series: Series
  
  override fun getSteps(): List<Step<out Any, out Any>> {
    return listOf(
      SimpleStep("main", ::start),
    )
  }
  
  open fun getFullUrl(url: String): String {
    if (url.startsWith("http:")
      || url.startsWith("https:")
      || url.startsWith("www")
    ) return url
    return if (!url.startsWith("/")) settings.websiteUrl + "/" + url else settings.websiteUrl + url
  }
  
  protected suspend fun start() {
    series = (if (Files.exists(Path.of(settings.outputPath))) {
      try {
        val current = Utils.jsonMapper.readValue(File(settings.outputPath), Series::class.java)
        if (current.season == settings.seasonNumber && settings.seriesName.equals(current.title, true)) {
          current.copy(title = settings.seriesName, seriesUrl = getSeriesLink())
        } else {
          null
        }
      } catch (e: Exception) {
        lg().error("Error while trying to deserialize current series", e)
        null
      }
    } else null) ?: Series(settings.seriesName, settings.seasonNumber, getMainLang(), getSeriesLink())
    
    driver.get(series.seriesUrl)
    driver.waitForVisibility(By.tagName("body"))
    title = driver.title
    waitIfPaused()
    
    val items: List<VideoData> = findEpisodeItems()
    waitIfPaused()
    var i = settings.searchStartIndex
    while (i <= items.size && i <= settings.searchStopIndex) {
      val item: VideoData = items[i - 1]
//      if (options.getRequiredIndexes() != null) {
//        if (!options.getRequiredIndexes().contains(item.getNum())) {
//          Gripper.log.info("Skipped " + item.toString())
//          i++
//          continue
//        }
//      }
      var downloadUrl: VideoUrl? = null
      val err = mutableListOf<ErrorEntry>()
      val serverHandlers = streamingServersHandlers()
      try {
        goToEpisodePage(item)
        waitIfPaused()
        val allServers: List<VideoServer> = loadItemDataFromSummaryPageAndGetServers(item).let { list ->
          if (list.any { it.type != EpisodeAudioTypes.UNKNOWN }) {
            val enabled = settings.allowedEpisodeTypes.filter { it.enabled }.map { it.kind }
            val n = mutableListOf<VideoServer>()
            for (en in enabled) {
              n.addAll(list.filter { it.type == en }.sortedBy { it.format.quality.priority })
            }
            n
          } else {
            list.sortedBy { it.format.quality.priority }
          }
        }
        waitIfPaused()
        if (allServers.isEmpty()) throw IllegalArgumentException(
          String.format(
            "No hosting found for: E%02d %s | %s%n",
            item.number,
            item.title,
            item.episodePageUrl
          )
        )
        lg().debug("Servers found for E${item.number.toString().padStart(2, '0')} \n"
            + allServers.joinToString("\n") { "${it.serverName} - ${it.type} - ${it.format}" })
        val priorities: List<VideoServerConfig> = getPriorities(i)
        for (priority in priorities) {
          val servers = allServers
            .filter {
              it.serverName.lowercase().contains(priority.searchTerm.lowercase())
            }
            .onEach { it.matchedServerDef = serverHandlers.keys.first { it.searchTerm() == priority.searchTerm } }
            .take(settings.triesBeforeStreamingProviderChange)
          
          for (server in servers) {
            try {
              item.server = server
              waitIfPaused()
              downloadUrl = goToExternalServerVideoPage(item) {
                waitIfPaused()
                findLoadedVideoDownloadUrl(item)
              }
              
              if (downloadUrl != null) {
                break
              }
            } catch (e: TerminationException) {
              throw e
            } catch (e: Exception) {
              lg().error("Error while searching download url at ${server.serverName} [${server.index}] | ${server.videoPageExternalUrl}", e)
            }
          }
          if (downloadUrl != null) {
            break
          }
        }
      } catch (e: TerminationException) {
        throw e
      } catch (e: Exception) {
        lg().error("Error while searching download url", e)
      }
      if (downloadUrl == null) {
        err.add(ErrorEntry(2, "Download url not found"))
      }
      val ep = Episode(
        item.title,
        item.number,
        downloadUrl,
        item.episodePageUrl,
        item.server.type,
        item.server.format.toEpisodeFormat(),
        err
      )
      val episodes = series.episodes.toMutableList()
      episodes.removeIf { it.number == ep.number }
      episodes.add(ep)
      episodes.sortBy { it.number }
      series = series.copy(episodes = episodes)
      waitIfPaused()
      Utils.jsonMapper.writeValue(File(settings.outputPath), series)
      lg().info("\n$ep\n")
      i++
    }
  }
  
  
  protected open suspend fun findLoadedVideoDownloadUrl(data: VideoData): VideoUrl? {
    delay(200)
    
    val serverHandler = streamingServersHandlers()[data.server.matchedServerDef]!!
    var found = serverHandler.findVideoSrcUrl()
    if (found == null) {
      checkForCaptchaAndOtherOverlays(data)
      runBlocking(Dispatchers.Main) {
        GlobalState.view.showMessageDialog("Cannot find URL. Video not started?")
      }
      found = serverHandler.findVideoSrcUrl()
    }
    if (found?.startsWith("blob:") == true) {
      var s = 12
      var streamUrl = getM3U8UrlFromEvents(serverHandler)
      while (streamUrl == null && s > 0) {
        delay(500)
        s--
        streamUrl = getM3U8UrlFromEvents(serverHandler)
      }
      
      return streamUrl?.let { M3U8Url(it) }
    }
    
    return found?.let { DirectUrl(it) }
  }
  
  protected open suspend fun getSeriesLink() = getFullUrl(settings.seriesUrl)
  
  protected open suspend fun <T> goToExternalServerVideoPage(data: VideoData, blockExecutedOnPage: (suspend () -> T)? = null): T? {
    driver.navigate().to(data.server.videoPageExternalUrl)
    return blockExecutedOnPage?.invoke()
  }
  
  protected open suspend fun goToEpisodePage(data: VideoData) {
    lg().info("Opening ${data.episodePageUrl}")
    driver.navigate().to(getFullUrl(data.episodePageUrl))
  }
  
  
  protected open fun getPriorities(indexOffset: Int): List<VideoServerConfig> {
    var offset = indexOffset
    val base: MutableList<VideoServerConfig> = settings.videoServerConfigs.filter { obj -> obj.enabled }.toMutableList()
    
    if (settings.numOfTopStreamingProvidersUsedSimultaneously <= 0) {
      return base
    }
    val top: List<VideoServerConfig> = base.take(settings.numOfTopStreamingProvidersUsedSimultaneously)
    base.removeAll(top)
    offset %= top.size
    val deque = ArrayDeque(top)
    for (i in 0 until offset) {
      val poll = deque.poll()
      deque.add(poll)
    }
    val result = deque.toMutableList()
    result.addAll(base)
    return result
  }
  
  
  protected open fun getM3U8UrlFromEvents(handler: VideoServerHandler): String? {
    val url = try {
      val events = getNetworkResponseEventsFromBrowserTools().filter { it.response.mimeType == "application/vnd.apple.mpegurl" }
      val m3u8events = events.filter { it.response.url.contains(".m3u8") }
      val index = m3u8events.indexOfFirst { handler.checkIfM3U8UrlCorrect(it.response.url) }
      lg().debug("Filtered stream urls from browser tools (matching: ${events.size})\n" + m3u8events.mapIndexed { i, e ->
        (if (i == index) "[#] " else "[ ] ") + e.response.url
      }.joinToString("\n"))
      
      m3u8events.getOrNull(index)?.response?.url
    } catch (e: InvalidArgumentException) {
      null
    }
    if (url == null) {
      val events = getNetworkResponseEventsFromJS()
      val m3u8events = events.filter { it.name.contains(".m3u8") }
      val index = m3u8events.indexOfFirst { handler.checkIfM3U8UrlCorrect(it.name) }
      lg().debug("Filtered stream urls from JavaScript performance objects (all: ${events.size})\n" + m3u8events.mapIndexed { i, e ->
        (if (i == index) "[#] " else "[ ] ") + e.name
      }.joinToString("\n"))
      return m3u8events.getOrNull(index)?.name
    }
    return url
  }
  
  protected open suspend fun checkForCaptchaAndOtherOverlays(data: VideoData) {
  
  }
  
  protected open fun streamingServersHandlers(): Map<VideoServerDefinition, VideoServerHandler> {
    val map: MutableMap<VideoServerDefinition, VideoServerHandler> =
      CommonVideoServers.values().associateWith { object : VideoServerHandler(it, driver, this) {} }.toMutableMap()
    
    map[CommonVideoServers.UPSTREAM] = object : VideoServerHandler(CommonVideoServers.UPSTREAM, driver, this) {
      private val streamRegex = Regex(""".*index.*.m3u8.*""")
      
      override fun checkIfM3U8UrlCorrect(url: String): Boolean {
        return url.matches(streamRegex)
      }
    }
    map[CommonVideoServers.VOE] = object : VideoServerHandler(CommonVideoServers.VOE, driver, this) {
      private val streamRegex = Regex(""".*index.*.m3u8.*""")
      
      override fun checkIfM3U8UrlCorrect(url: String): Boolean {
        return url.matches(streamRegex)
      }
    }
    return map
  }
  
  protected abstract suspend fun getMainLang(): String
  
  protected abstract suspend fun findEpisodeItems(serverIndex: String? = null): List<VideoData>
  
  protected abstract suspend fun loadItemDataFromSummaryPageAndGetServers(data: VideoData): List<VideoServer>
}
