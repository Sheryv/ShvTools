package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common

import com.fasterxml.jackson.module.kotlin.convertValue
import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.browser.DriverBuilder
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.config.impl.streamingwebsite.HistoryItem
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.SeleniumCrawler
import com.sheryv.tools.webcrawler.process.base.model.*
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.*
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.videoserver.HLSVideoServerHandler
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.videoserver.VideoServerHandler
import com.sheryv.tools.webcrawler.service.Registry
import com.sheryv.tools.webcrawler.service.videosearch.TmdbService
import com.sheryv.tools.webcrawler.utils.AppError
import com.sheryv.tools.webcrawler.view.remoteclient.WebSocket
import com.sheryv.tools.webcrawler.view.remoteclient.WsMessage.InterceptorMessage
import com.sheryv.tools.webcrawler.view.remoteclient.WsMessage.MsgType
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.inBackground
import com.sheryv.util.inBackgroundWithResult
import com.sheryv.util.logging.log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openqa.selenium.By
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
  driverBuilder: DriverBuilder<SeleniumDriver>,
  params: ProcessParams
) : SeleniumCrawler<StreamingWebsiteSettings>(configuration, browser, def, driverBuilder, params), KoinComponent {
  lateinit var series: Series
  lateinit var websocket: WebSocket
  val interceptorEventsFlow: Channel<InterceptorMessage> =
    Channel<InterceptorMessage>(capacity = 500, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  
  val handlers by lazy {
    Registry.get().serverDefinitions().map { it.buildHandler(this) }
  }
  
  val tmdb: TmdbService by inject()
  
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
  
  override fun initialize() {
    websocket = WebSocket({ m, e ->
      if (m?.type == MsgType.INTERCEPT) {
        val intercepted = SerialisationUtils.jsonMapper.convertValue<InterceptorMessage>(m.data)
        log.trace("Intercept: {}", intercepted.url)
        interceptorEventsFlow.trySend(intercepted)
      }
    }, { }
    ).also {
      it.connectionLostTimeout = 10
      it.start()
    }
    
    super.initialize()
  }
  
  protected suspend fun start() {
    
    series = if (Files.exists(settings.outputPath) && params.runPreconfiguredFromLastResult) {
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
        throw RuntimeException("Error while trying to deserialize current series", e)
      }
    } else {
      Series(0, settings.seriesName, settings.seasonNumber, getMainLang(), getSeriesLink())
    }


//    driver.enableDevToolsWithNetworkModule()
    val enabledAudioTypes = settings.allowedEpisodeTypes.filter { it.enabled }.map { it.kind }.sortedBy { it.priority }
    driver.get(getSeriesLink())
//    driver.get("https://bot.sannysoft.com/")
    driver.waitForVisibility(By.tagName("body"))
    
    
    forceSeriesPageLoaded()
    
    title = driver.title
    if (series.id == 0L) {
      val details = extractSeriesDetails()
      series = tmdb.searchSeriesBestMatch(series, details.title, details.year?.let { listOf(it, it - 1) } ?: emptyList()) ?: series.copy(title = details.title)
    }
    settings.appendHistory(HistoryItem(series.title, series.seriesUrl, series.tvdbId?.toLongOrNull(), series.season))
    Configuration.get().save()
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
            val allServers: List<VideoServer> = loadItemDataFromSummaryPageAndGetServers(item).let { found ->
              if (found.any { it.type != EpisodeAudioTypes.UNKNOWN }) {
                val n = mutableListOf<VideoServer>()
                for (en in enabledAudioTypes) {
                  n.addAll(found.filter { it.type == en }.sortedBy { it.format.quality.priority })
                }
                n
              } else {
                found.sortedBy { it.format.quality.priority }
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
            
            val priorities = getPriorities(index, allServers)
            if (priorities.isNotEmpty()) {
              log.info("Using servers: " + priorities.joinToString(" | ") { (k, v) ->
                "${k.def.label}=(${v.index})"
              })
            } else {
              log.warn("No server matches criteria")
            }
            
            for (pair in priorities) {
              try {
                val handler = pair.first
                item.server = pair.second
                waitIfPaused()
                item.pageOpenTimestamp = Instant.now()
                
                clearListeners()
                var asyncIntercepted: Deferred<InterceptorMessage>? = null
                if (handler is HLSVideoServerHandler) {
                  asyncIntercepted = setupListenerAndWaitForCorrectEvent { e ->
                    log.debug("[{}] Checking: {}", item.number, e.url)
                    handler.isUrlMatchingRequestWithM3U8Manifest(e.url)
                  }
                }
                
                downloadUrl = openStreamAndInitializePlayerThenRun(item, pair.second, handler) {
                  waitIfPaused()
                  if (handler is HLSVideoServerHandler) {
                    val findContainerJob = inBackground {
                      log.debug("[{}] Waiting for video tag container", item.number)
                      handler.findVideoContainer()
                    }
                    asyncIntercepted!!.invokeOnCompletion {
                      findContainerJob.cancel()
                    }
                    findContainerJob.join()
                    log.debug("[{}] Waiting for intercepted request", item.number)
                    val response = try {
                      withTimeout(15.seconds) {
                        asyncIntercepted.await()
                      }
                    } finally {
                      asyncIntercepted.cancel()
                    }
                    return@openStreamAndInitializePlayerThenRun extractUrlFromRequest(response)
                  } else {
                    findDirectVideoDownloadUrlFromHtml(item, handler)
                  }
                }
                
                if (downloadUrl != null) {
                  break
                }
              } catch (e: TerminationException) {
                throw e
              } catch (e: Exception) {
                log.error(
                  "Error while searching download url at ${pair.second.serverName} [${pair.second.index}] | ${pair.second.videoPageExternalUrl}",
                  e
                )
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
      interceptorEventsFlow.cancel()
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
    val handler = settings.videoServerConfigs
      .firstOrNull { it.definition.domains.any { url.contains(it) } }
      ?.let { c -> handlers.first { it.def == c.definition } }
      ?: throw AppError("There is no handler defined for hosting provider at $url")
    
    var asyncIntercepted: Deferred<InterceptorMessage>? = null
    if (handler is HLSVideoServerHandler) {
      asyncIntercepted = setupListenerAndWaitForCorrectEvent { e ->
        log.debug("Checking ad-hoc: {}", e.url)
        handler.isUrlMatchingRequestWithM3U8Manifest(e.url)
      }
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
    
    val result = if (handler is HLSVideoServerHandler) {
      val findContainerJob = inBackground {
        log.debug("Waiting ad-hoc for video tag container")
        handler.findVideoContainer()
      }
      asyncIntercepted!!.invokeOnCompletion {
        findContainerJob.cancel()
      }
      findContainerJob.join()
      log.debug("Waiting ad-hoc for intercepted request")
      val response = try {
        withTimeout(15.seconds) {
          asyncIntercepted.await()
        }
      } finally {
        asyncIntercepted.cancel()
      }
      extractUrlFromRequest(response)
    } else {
      findDirectVideoDownloadUrlFromHtml(VideoData(url, driver.title.orEmpty(), 1), handler)
    }
    
    return result
  }
  
  protected open suspend fun findDirectVideoDownloadUrlFromHtml(data: VideoData, serverHandler: VideoServerHandler<*>): DirectUrl? {
    var found = serverHandler.findVideoSrcUrl(5)
    if (found == null) {
      if (checkForCaptchaAndOtherOverlays(data)) {
        runBlocking(Dispatchers.Main) {
          GlobalState.view.showMessageDialog("Cannot find URL. Video not started?")
        }
        found = serverHandler.findVideoSrcUrl(5)
      }
    }
    return found?.let { DirectUrl(it) }
  }
  
  protected open suspend fun findLoadedVideoDownloadUrl(data: VideoData, serverHandler: VideoServerHandler<*>): VideoUrl? {
    delay(200)
    if (serverHandler is HLSVideoServerHandler) {
      var s = 100
      var streamUrl = serverHandler.getM3U8UrlFromEvents()
      if (streamUrl == null) {
        checkForCaptchaAndOtherOverlays(data)
        runBlocking(Dispatchers.Main) {
          GlobalState.view.showMessageDialog("Cannot find URL. Video not started?")
        }
      }
      while (streamUrl == null && s > 0) {
        delay(100)
        s--
        streamUrl = serverHandler.getM3U8UrlFromEvents()
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
    handler: VideoServerHandler<*>,
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
  ): List<Pair<VideoServerHandler<*>, VideoServer>> {
    
    val base = settings.videoServerConfigs.filter { obj -> obj.enabled }.map { priority ->
      priority to allFoundServers
        .filter { priority.definition.searchTerm.containsMatchIn(it.serverName.lowercase()) }
        .firstOrNull { settings.allowedQualities.any { q -> it.format.quality == q.kind } }
    }.filter { (_, v) -> v != null }.map { (k, v) -> k to v!! }
    
    val list = if (settings.shuffleStreamingProviderOrder) {
      
      val offset = episodeNumber - 1 % settings.numOfTopStreamingProvidersUsedSimultaneously
      val result = base.take(settings.numOfTopStreamingProvidersUsedSimultaneously).toMutableList()
      Collections.rotate(result, -offset)
      result.addAll(base.drop(settings.numOfTopStreamingProvidersUsedSimultaneously))
      result
    } else {
      base
    }
    
    return list.map { (k, v) -> handlers.first { it.def == k.definition } to v }
  }
  
  protected open suspend fun searchByTitleAndYear(title: String, year: Int? = null): String? {
    return null
  }
  
  
  protected open suspend fun checkForCaptchaAndOtherOverlays(data: VideoData): Boolean {
    return false
  }
  
  protected open suspend fun isSingleBuiltinHostingPerEpisode(data: VideoData): Boolean = false
  
  protected open suspend fun builtinHostingServerHandler(data: VideoData): VideoServerHandler<*> {
    throw NotImplementedError()
  }

  protected open suspend fun extractSeriesDetails(): SeriesDetailsFromPage = SeriesDetailsFromPage(title ?: settings.seriesName)
  
  protected abstract suspend fun getMainLang(): String
  
  protected abstract suspend fun findEpisodeItems(serverIndex: String? = null): List<VideoData>
  
  protected abstract suspend fun loadItemDataFromSummaryPageAndGetServers(data: VideoData): List<VideoServer>
  
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
    filter: (data: InterceptorMessage) -> Boolean
  ): Deferred<InterceptorMessage> = inBackgroundWithResult {
    return@inBackgroundWithResult interceptorEventsFlow.receiveAsFlow().first { filter(it) }
  }
  
  private fun extractUrlFromRequest(wrapper: InterceptorMessage): VideoUrl {
    val headers = wrapper.headers.associate { it.name to it.value }
    return M3U8Url(wrapper.url, UrlMetadata(headers))
  }
  
  
}
