package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common

import com.fasterxml.jackson.module.kotlin.convertValue
import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.config.impl.streamingwebsite.HistoryItem
import com.sheryv.tools.webcrawler.config.impl.streamingwebsite.VideoServerConfig
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.SeleniumCrawler
import com.sheryv.tools.webcrawler.process.base.model.*
import com.sheryv.tools.webcrawler.process.base.model.browserevent.BrowserResponseEvent
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.*
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.filman.FilmanCrawler
import com.sheryv.tools.webcrawler.utils.AppError
import com.sheryv.tools.webcrawler.view.remoteclient.Intercepted
import com.sheryv.tools.webcrawler.view.remoteclient.MsgType
import com.sheryv.tools.webcrawler.view.remoteclient.WS
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.inBackgroundAsync
import com.sheryv.util.logging.log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import org.openqa.selenium.By
import org.openqa.selenium.bidi.network.RequestData
import org.openqa.selenium.devtools.v142.network.model.ResourceType
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.time.Duration.Companion.seconds


abstract class StreamingWebsiteBase(
  configuration: Configuration,
  browser: BrowserConfig,
  def: CrawlerDefinition<SeleniumDriver, StreamingWebsiteSettings>,
  driver: SeleniumDriver,
  params: ProcessParams
) : SeleniumCrawler<StreamingWebsiteSettings>(configuration, browser, def, driver, params) {
  lateinit var series: Series
  lateinit var websocket: WS
  val flow: Channel<Intercepted> = Channel<Intercepted>(capacity = 500, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  
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
    return if (!url.startsWith("/")) def.attributes.websiteUrl + "/" + url else def.attributes.websiteUrl + url
  }
  
  protected suspend fun start() {
    
    series = (if (Files.exists(settings.outputPath) && params.runPreconfiguredFromLastResult) {
      try {
        val current = SerialisationUtils.jsonMapper.readValue(settings.outputPath.toFile(), Series::class.java)
//        if (current.season == settings.seasonNumber && settings.seriesName.equals(current.title, true)) {
//          current.copy(title = settings.seriesName, seriesUrl = getSeriesLink())
//        } else {
//          null
//        }
        
        if (current.seriesUrl.isBlank()) {
          current.copy(seriesUrl = getSeriesLink())
        } else {
          current
        }
      } catch (e: Exception) {
        log.error("Error while trying to deserialize current series", e)
        null
      }
    } else null) ?: Series(0, settings.seriesName, settings.seasonNumber, getMainLang(), getSeriesLink())

//    driver.enableDevToolsWithNetworkModule()
    
    driver.get(getSeriesLink())
//    driver.get("https://bot.sannysoft.com/")
    driver.waitForVisibility(By.tagName("body"))
    
    websocket = WS({ m, e ->
      if (m?.type == MsgType.INTERCEPT) {
        val intercepted = SerialisationUtils.jsonMapper.convertValue<Intercepted>(m.data)
        log.info("Intercept: {}", intercepted)
        flow.trySend(intercepted)
      }
    }, { }
    ).also {
      it.connectionLostTimeout = 10
      it.start()
    }
    
    forceSeriesPageLoaded()
    
    if (series.title.isBlank()) {
      series = series.copy(title = getSeriesName())
    }
    settings.appendHistory(HistoryItem(series.title, series.seriesUrl, series.tvdbId?.toLongOrNull(), series.season))
    Configuration.get().save()
    title = driver.title
    waitIfPaused()
    
    
    val items: List<VideoData> = findEpisodeItems()
    waitIfPaused()
    var index = settings.searchStartIndex
    try {
      
      while (index <= items.size && (index <= settings.searchStopIndex || settings.searchStopIndex < 0)) {
        
        val previous = series.episodes.firstOrNull { it.number == index }
        if (previous != null) {
          
          if (configuration.common.runOnlyForFailedOrAbsentEpisodes) {
            if (configuration.common.verifyDownloadedFilesBeforeRetrying) {
              val path = previous.generateDefaultFilePath(series, settings)
              if (path.exists() && path.fileSize() > 100) {
                log.info("Skipping episode $index as file '${path}' already exists")
                index++
                continue
              }
            } else {
              if (previous.errors.isEmpty() && previous.downloadUrl != null &&
                Duration.between(previous.updated, OffsetDateTime.now()) < settings.linkExpirationDuration
              ) {
                log.info("Skipping episode $index as its url is up-to-date")
                index++
                continue
              }
            }
          }
        }
        
        if (settings.skippedEpisodes.contains(index)) {
          log.info("Episode $index is skipped")
          index++
          continue
        }
        
        
        val item: VideoData = items[index - 1]
        
        var downloadUrl: VideoUrl? = null
        try {
          item.pageOpenTimestamp = Instant.now()
          waitIfPaused()
          goToEpisodePage(item)
          waitIfPaused()
          if (isSingleBuiltinHostingPerEpisode(item)) {
            item.server = VideoServer("_builtin", 0)
            downloadUrl = findLoadedVideoDownloadUrl(item, builtinHostingServerHandler(item))
          } else {
            val serverHandlers = streamingServersHandlers()
            
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
            log.info(
              "Servers found for E${item.number.toString().padStart(2, '0')}: "
                  + allServers.joinToString(" | ") { "(${it.index}) ${it.serverName} - ${it.type} - ${it.format}" })
            
            val priorities = getPriorities(index, allServers, serverHandlers)
            if (priorities.isNotEmpty()) {
              log.info("Using servers: " + priorities.joinToString(" | ") { (k, v) ->
                val indexes = v.joinToString(",") { it.index.toString() }
                "${k.definition.label()}=($indexes)"
              })
            } else {
              log.warn("No server matches criteria")
            }
            
            for (pair in priorities) {
              for (server in pair.second) {
                try {
                  item.server = server
                  waitIfPaused()
                  item.pageOpenTimestamp = Instant.now()
                  val handler = streamingServersHandlers()[server.matchedServerDef]!!
                  
                  clearListeners()
                  var asyncIntercepted: Deferred<Intercepted>? = null
                  if (handler.server.isStreaming) {
                    asyncIntercepted = setupListenerAndWaitForCorrectEvent { e ->
                      handler.server.isUrlMatchingRequestWithM3U8Manifest(e.url)
                    }
                  }
                  downloadUrl = openStreamAndInitializePlayerThenRun(item, server) {
                    waitIfPaused()
                    if (handler.server.isStreaming) {
                      handler.findVideoContainer()
                      val response = withTimeout(10.seconds) {
                        asyncIntercepted!!.await()
                      }
                      return@openStreamAndInitializePlayerThenRun extractUrlFromRequest(response)
                    } else {
                      findDirectVideoDownloadUrlFromHtml(item, handler)
                    }
                  }


//                clearNetworkInterceptor()
//                var asyncIntercepted: Deferred<HttpRequest>? = null
//                if (handler.server.isStreaming) {
//                  asyncIntercepted = inBackgroundAsync {
//                    val (req, _) = setupNetworkInterceptor(true, true) { req ->
//                      handler.server.isUrlMatchingRequestWithM3U8Manifest(req.uri.toString())
//                    }
//                    return@inBackgroundAsync req
//                  }
//                }
//                downloadUrl = openStreamAndInitializePlayerThenRun(item, server) {
//                  waitIfPaused()
//                  if (handler.server.isStreaming) {
//                    handler.findVideoContainer()
//                    val request = withTimeout(10.seconds) {
//                      asyncIntercepted!!.await()
//                    }
//                    return@openStreamAndInitializePlayerThenRun extractUrlFromRequest(request)
//                  } else {
//                    findDirectVideoDownloadUrlFromHtml(item, handler)
//                  }

//                  findLoadedVideoDownloadUrl(item, streamingServersHandlers()[server.matchedServerDef]!!)
//                }
                  
                  if (downloadUrl != null) {
                    break
                  }
                } catch (e: TerminationException) {
                  throw e
                } catch (e: Exception) {
                  log.error(
                    "Error while searching download url at ${server.serverName} [${server.index}] | ${server.videoPageExternalUrl}",
                    e
                  )
                }
              }
              if (downloadUrl != null) {
                break
              }
            }
          }
        } catch (e: TerminationException) {
          throw e
        } catch (e: Exception) {
          log.error("Error while searching download url", e)
          try {
            driver.saveScreenshot(Path.of("").resolve("screens").resolve("screen-shot-${System.currentTimeMillis() / 1000}.png"))
          } catch (e: Exception) {
            log.warn("Error saving screenshot {}", e.message)
          }
        } finally {
          clearNetworkInterceptor()
        }
        val err = mutableListOf<ErrorEntry>()
        if (downloadUrl == null) {
          err.add(ErrorEntry(2, "Download url not found"))
        }
        val ep = previous?.copy(
          downloadUrl = downloadUrl,
          sourcePageUrl = item.episodePageUrl,
          type = item.server?.type,
          format = item.server?.format?.toEpisodeFormat(),
          errors = err
        ) ?: Episode(
          0,
          item.title,
          item.number,
          downloadUrl,
          item.episodePageUrl,
          item.server?.type,
          item.server?.format?.toEpisodeFormat(),
          err
        )
        val episodes = series.episodes.toMutableList()
        episodes.removeIf { it.number == ep.number }
        episodes.add(ep)
        episodes.sortBy { it.number }
        series = series.copy(episodes = episodes)
        waitIfPaused()
        SerialisationUtils.jsonMapper.writeValue(settings.outputPath.toFile(), series)
        log.info("\n$ep\n")
        index++
      }
    } finally {
      websocket.stop()
      flow.cancel()
    }
    
    if (index - 1 < series.episodes.size) {
      val e = series.episodes.take(index - 1) +
          series.episodes.drop(index - 1).map { it.copy(errors = listOf(ErrorEntry(3, "Record not processed"))) }
      series = series.copy(episodes = e)
      SerialisationUtils.jsonMapper.writeValue(settings.outputPath.toFile(), series)
    }
  }
  
  open suspend fun openStandaloneStreamingPage(url: String): VideoUrl? {
    delay(300)
    val handlers = streamingServersHandlers()
    val handler = handlers.filterKeys { it.domains().any { url.contains(it) } }.firstNotNullOfOrNull { it.value }
      ?: throw AppError("There is no handler defined for hosting provider at $url")

//    var resultAsync = inBackgroundAsync {
//      clearNetworkInterceptor()
//      val (req, _) = setupNetworkInterceptor(true, true) { req ->
//        handler.server.isUrlMatchingRequestWithM3U8Manifest(req.uri.toString())
//      }
//      return@inBackgroundAsync req
//    }
    var resultAsync = setupListenerAndWaitForCorrectEvent { e ->
      handler.server.isUrlMatchingRequestWithM3U8Manifest(e.url)
    }
    
    driver.navigate().to(url)
    
    waitIfPaused()
    driver.waitForVisibility(By.tagName("body"))
    series = try {
      SerialisationUtils.jsonMapper.readValue(settings.outputPath.toFile(), Series::class.java)
    } catch (e: Exception) {
      log.error("Error", e)
      Series(0, driver.title.orEmpty(), 0, getMainLang(), url)
    }
    
    val req = resultAsync.await()
//    val result = findLoadedVideoDownloadUrl(VideoData(url, driver.title.orEmpty(), 1).also { it.pageOpenTimestamp = startTime }, handler)
    val result = extractUrlFromRequest(req)
    return result
  }
  
  protected open suspend fun findDirectVideoDownloadUrlFromHtml(data: VideoData, serverHandler: VideoServerHandler): DirectUrl? {
    var found = serverHandler.findVideoSrcUrl(5)
    if (found == null) {
      checkForCaptchaAndOtherOverlays(data)
//      runBlocking(Dispatchers.Main) {
//        GlobalState.view.showMessageDialog("Cannot find URL. Video not started?")
//      }
      found = serverHandler.findVideoSrcUrl(5)
    }
    return found?.let { DirectUrl(it) }
  }
  
  protected open suspend fun findLoadedVideoDownloadUrl(data: VideoData, serverHandler: VideoServerHandler): VideoUrl? {
    delay(200)
    if (serverHandler.server.isStreaming) {
      var s = 100
      var streamUrl = getM3U8UrlFromEvents(serverHandler)
      if (streamUrl == null) {
        checkForCaptchaAndOtherOverlays(data)
        runBlocking(Dispatchers.Main) {
          GlobalState.view.showMessageDialog("Cannot find URL. Video not started?")
        }
      }
      while (streamUrl == null && s > 0) {
        delay(100)
        s--
        streamUrl = getM3U8UrlFromEvents(serverHandler)
      }
      return streamUrl?.let { M3U8Url(it) }
      
    } else {
      var found = serverHandler.findVideoSrcUrl(5)
      if (found == null) {
        checkForCaptchaAndOtherOverlays(data)
        runBlocking(Dispatchers.Main) {
          GlobalState.view.showMessageDialog("Cannot find URL. Video not started?")
        }
        found = serverHandler.findVideoSrcUrl(5)
      }
      return found?.let { DirectUrl(it) }
    }
    return null
  }
  
  protected open suspend fun getSeriesLink() = getFullUrl(settings.seriesUrl)
  
  protected open suspend fun <T> openStreamAndInitializePlayerThenRun(
    data: VideoData,
    server: VideoServer,
    blockExecutedOnPage: (suspend () -> T)? = null
  ): T? {
    return data.server?.let {
      driver.navigate().to(getFullUrl(it.videoPageExternalUrl!!))
      blockExecutedOnPage?.invoke()
    } ?: run {
      log.error("Server details are empty for episode ${data.number}")
      null
    }
  }
  
  protected open suspend fun forceSeriesPageLoaded() {
  
  }
  
  protected open suspend fun goToEpisodePage(data: VideoData) {
    log.info("Opening ${data.episodePageUrl}")
    driver.navigate().to(getFullUrl(data.episodePageUrl))
  }
  
  
  protected open fun getPriorities(
    episodeNumber: Int,
    allFoundServers: List<VideoServer>,
    handlers: Map<VideoServerDefinition, VideoServerHandler>
  ): List<Pair<VideoServerConfig, List<VideoServer>>> {
    
    val base = settings.videoServerConfigs.filter { obj -> obj.enabled }.map { priority ->
      priority to allFoundServers
        .filter { priority.definition.searchTerm().containsMatchIn(it.serverName.lowercase()) }
        .filter { settings.allowedQualities.any { q -> it.format.quality == q.kind } }
        .onEach { it.matchedServerDef = priority.definition }
        .take(settings.triesBeforeStreamingProviderChange)
    }.filter { (_, v) -> v.isNotEmpty() }
    val offset = episodeNumber - 1 % settings.numOfTopStreamingProvidersUsedSimultaneously
    val result = base.take(settings.numOfTopStreamingProvidersUsedSimultaneously).toMutableList()
    Collections.rotate(result, -offset)
    result.addAll(base.drop(settings.numOfTopStreamingProvidersUsedSimultaneously))

//    var offset = indexOffset
//    val base: MutableList<VideoServerConfig> = settings.videoServerConfigs.filter { obj -> obj.enabled }.toMutableList()
//
//    if (settings.numOfTopStreamingProvidersUsedSimultaneously <= 0) {
//      return base
//    }
//    val top: List<VideoServerConfig> = base.take(settings.numOfTopStreamingProvidersUsedSimultaneously)
//    base.removeAll(top)
//    offset %= top.size
//    val deque = ArrayDeque(top)
//    for (i in 0 until offset) {
//      val poll = deque.poll()
//      deque.add(poll)
//    }
//    val result = deque.toMutableList()
//    result.addAll(base)
    return result
  }
  
  
  protected open fun getM3U8UrlFromEvents(handler: VideoServerHandler): String? {
    val url = try {
      val events = getNetworkResponseEventsFromBrowserTools().filter { it.response?.mimeType == "application/vnd.apple.mpegurl" }.toList()
      val m3u8events = events.filter { it.response!!.url.contains(".m3u8") }
      val index = m3u8events.indexOfLast { handler.checkIfM3U8UrlCorrect(it.response!!.url, series) }
      if (events.isNotEmpty()) {
        log.debug("Filtered stream urls from browser tools (matching: ${events.size})\n" + m3u8events.mapIndexed { i, e ->
          (if (i == index) "[#] " else "[ ] ") + e.response!!.url
        }.joinToString("\n"))
      }
      
      m3u8events.getOrNull(index)?.response?.url ?: m3u8events.firstNotNullOfOrNull {
        handler.tryToGetCorrectM3U8Url(
          it.response?.url!!,
          series
        )
      }?.also {
        log.debug("Found alternative url from browser tools: $it")
      }
    } catch (e: IllegalArgumentException) {
      null
    }
    if (url == null) {
      val events = getNetworkResponseEventsFromJS()
      val m3u8events = events.filter { it.name.contains(".m3u8") }
      val index = m3u8events.indexOfLast { handler.checkIfM3U8UrlCorrect(it.name, series) }
      if (events.isNotEmpty()) {
        log.debug("Filtered stream urls from JavaScript performance objects (all: ${events.size})\n" + m3u8events.mapIndexed { i, e ->
          (if (i == index) "[#] " else "[ ] ") + e.name
        }.joinToString("\n"))
      }
      
      return m3u8events.getOrNull(index)?.name ?: m3u8events
        .firstNotNullOfOrNull { handler.tryToGetCorrectM3U8Url(it.name, series) }
        ?.also {
          log.debug("Found alternative url from JavaScript performance objects: $it")
        }
    }
    return url
  }
  
  protected open suspend fun checkForCaptchaAndOtherOverlays(data: VideoData) {
  
  }
  
  protected open suspend fun isSingleBuiltinHostingPerEpisode(data: VideoData): Boolean = false
  
  protected open suspend fun builtinHostingServerHandler(data: VideoData): VideoServerHandler {
    throw NotImplementedError()
  }
  
  private fun createHandlerWithCustomRegexCheck(
    handler: CommonVideoServers,
    regex: Regex,
    map: MutableMap<VideoServerDefinition, VideoServerHandler>? = null
  ): VideoServerHandler {
    val overwritten = object : VideoServerHandler(handler, driver, this) {
      private val streamRegex = regex
      
      override fun checkIfM3U8UrlCorrect(url: String, series: Series): Boolean {
        return url.matches(streamRegex) && super.checkIfM3U8UrlCorrect(url, series)
      }
    }
    if (map != null) {
      map[handler] = overwritten
    }
    return overwritten
  }
  
  protected open fun streamingServersHandlers(): Map<VideoServerDefinition, VideoServerHandler> {
    val map: MutableMap<VideoServerDefinition, VideoServerHandler> =
      CommonVideoServers.values().associateWith { object : VideoServerHandler(it, driver, this) {} }.toMutableMap()
    
    createHandlerWithCustomRegexCheck(CommonVideoServers.VOE, Regex(""".*index.*.m3u8.*"""), map)
    createHandlerWithCustomRegexCheck(CommonVideoServers.VTUBE, Regex(""".*index.*.m3u8.*"""), map)
    createHandlerWithCustomRegexCheck(CommonVideoServers.UPSTREAM, Regex(""".*index.*.m3u8.*"""), map)
    createHandlerWithCustomRegexCheck(CommonVideoServers.VIDGUARD, Regex(""".*index.*.m3u8.*"""), map)
    createHandlerWithCustomRegexCheck(CommonVideoServers.VIDSTREAM, Regex(""".*v.m3u8.*"""), map)
    return map
  }
  
  protected open suspend fun getSeriesName(): String = ""
  
  protected abstract suspend fun getMainLang(): String
  
  protected abstract suspend fun findEpisodeItems(serverIndex: String? = null): List<VideoData>
  
  protected abstract suspend fun loadItemDataFromSummaryPageAndGetServers(data: VideoData): List<VideoServer>
  
  
  private fun extractUrlFromRequest(wrapper: Intercepted): VideoUrl {
    val headers = wrapper.headers.associate { it.name to it.value }
    return M3U8Url(wrapper.url, UrlMetadata(headers))
  }
  
  private fun extractUrlFromRequest3(wrapper: BrowserResponseEvent): VideoUrl {
    val headers = wrapper.response!!.headers!!.mapValues {
      if (it is Iterable<*>) {
        it.joinToString(",")
      } else {
        it?.toString() ?: ""
      }
    }
    
    return M3U8Url(wrapper.response.url.toString(), UrlMetadata(headers))
  }
  
  private fun extractUrlFromRequest2(wrapper: RequestData): VideoUrl {
    val headers = wrapper.headers
      .filter { it.value?.value?.isNotEmpty() == true }
      .associate { it.name to it.value.value }
    
    return M3U8Url(wrapper.url, UrlMetadata(headers))
  }
  
  
  fun closeOtherTabs(leftTab: String) {
    for (handle in driver.windowHandles) {
      try {
        if (handle != leftTab) {
          driver.switchTo().window(handle).close()
        }
      } catch (e: Exception) {
        log.warn("Cannot cleanup tab: {}", e.message?.lines()?.first())
      }
    }
  }
  
  suspend fun setupListenerAndWaitForCorrectEvent(
    filter: (data: Intercepted) -> Boolean
  ): Deferred<Intercepted> = inBackgroundAsync {
    return@inBackgroundAsync flow.receiveAsFlow().first { filter(it) }
  }
}

private val USEFUL_REQUEST_TYPES = listOf(ResourceType.OTHER, ResourceType.FETCH, ResourceType.XHR, ResourceType.MEDIA)
