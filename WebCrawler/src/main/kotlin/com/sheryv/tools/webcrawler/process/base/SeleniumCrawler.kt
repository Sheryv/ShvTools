package com.sheryv.tools.webcrawler.process.base

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.ProcessingStates
import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.browser.DriverBuilder
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.model.*
import com.sheryv.tools.webcrawler.process.base.model.browserevent.BrowserResponseEvent
import com.sheryv.tools.webcrawler.process.base.model.browserevent.JSNetworkEvent
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.inBackgroundAsync
import com.sheryv.util.logging.log
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import org.openqa.selenium.By
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebElement
import org.openqa.selenium.bidi.network.RequestData
import org.openqa.selenium.devtools.v142.network.model.ResponseReceived
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.remote.http.HttpRequest
import org.openqa.selenium.remote.http.HttpResponse
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class SeleniumCrawler<S : SettingsBase>(
  configuration: Configuration,
  browser: BrowserConfig,
  def: CrawlerDefinition<SeleniumDriver, S>,
  driverBuilder: DriverBuilder<SeleniumDriver>,
  params: ProcessParams
) :
  Crawler<SeleniumDriver, S>(configuration, browser, def, driverBuilder, params) {
  
  protected val wait: WebDriverWait by lazy { WebDriverWait(driver, Duration.ofSeconds(15)) }
  protected var title: String? = null
  
  protected fun loadInitPage() {
    driver.get(def.attributes.websiteUrl)
    driver.waitForVisibility(By.tagName("body"))
    title = driver.title
  }
  
  internal suspend fun waitForAttribute(selector: By, attribute: String, timeoutSeconds: Int = 30): WebElement? {
    return waitForAttributeCheckBy(selector, attribute, timeoutSeconds) { it?.getAttribute(attribute) != null }
  }
  
  internal suspend fun waitForNonEmptyAttribute(selector: By, attribute: String, timeoutSeconds: Int = 30): WebElement? {
    return waitForAttributeCheckBy(selector, attribute, timeoutSeconds) { !it?.getAttribute(attribute).isNullOrEmpty() }
  }
  
  internal fun wait(selector: By, timeoutSeconds: Int = 3, throwEx: Boolean = false): WebElement? {
    var element: WebElement? = null
    try {
      element = wait.withTimeout(Duration.ofSeconds(timeoutSeconds.toLong())).until(ExpectedConditions.presenceOfElementLocated(selector))
      
    } catch (e: TimeoutException) {
      com.sheryv.util.logging.log.warn(
        "ERROR: Selector '{}' not found during {} seconds | {}",
        selector.toString(),
        timeoutSeconds,
        e.message
      )
      if (throwEx) {
        throw e
      }
    }
    return element
  }
  
  internal suspend fun waitForAttributeCheckBy(
    selector: By,
    attribute: String,
    timeoutSeconds: Int,
    checkIsCorrect: (WebElement?) -> Boolean
  ): WebElement? {
    var element: WebElement? = null
    var waitValue = 5
    val repeats = if (timeoutSeconds > 10) {
      timeoutSeconds / waitValue
    } else {
      waitValue = 1
      timeoutSeconds
    }
    
    for (i in (0 until repeats)) {
      if (GlobalState.processingState.value == ProcessingStates.STOPPING) {
        throw TerminationException()
      }
      try {
        val start = System.currentTimeMillis()
        element = wait.withTimeout(Duration.ofSeconds(waitValue.toLong())).until(ExpectedConditions.presenceOfElementLocated(selector))
        if (checkIsCorrect(element)) {
          break
        }
        if (GlobalState.processingState.value == ProcessingStates.STOPPING) {
          throw TerminationException()
        }
        val end = System.currentTimeMillis()
        if (end - start < waitValue * 1000) {
          delay(waitValue * 1000 - (end - start))
        }
      } catch (ignored: TimeoutException) {
        element = null
      }
    }
    
    if (!checkIsCorrect(element)) {
      log.warn("ERROR: Attribute '{}' not found for selector '{}' during {} seconds", attribute, selector.toString(), timeoutSeconds)
      return null
    }
    return element
  }
  
  internal suspend fun waitForFirstAttributeIf(
    selector: By,
    attribute: String,
    condition: ((WebElement) -> Boolean),
    timeoutSeconds: Int = 30
  ): WebElement? {
    var waitValue = 5
    val repeats = if (timeoutSeconds > 10) {
      timeoutSeconds / waitValue
    } else {
      waitValue = 1
      timeoutSeconds
    }
    
    for (i in (0 until repeats)) {
      if (GlobalState.processingState.value == ProcessingStates.STOPPING) {
        throw TerminationException()
      }
      try {
        val start = System.currentTimeMillis()
        val elements =
          wait.withTimeout(Duration.ofSeconds(waitValue.toLong())).until(ExpectedConditions.presenceOfAllElementsLocatedBy(selector))
        if (elements.any(condition)) {
          return elements.first(condition)
        }
        if (GlobalState.processingState.value == ProcessingStates.STOPPING) {
          throw TerminationException()
        }
        val end = System.currentTimeMillis()
        if (end - start < waitValue * 1000) {
          delay(waitValue * 1000 - (end - start))
        }
      } catch (ignored: TimeoutException) {
      }
    }
    
    return null
  }
  
  val cachedEventsPrinted: MutableSet<String> = Collections.synchronizedSet(mutableSetOf<String>())
  
  fun getNetworkResponseEventsFromBrowserTools(): Sequence<BrowserResponseEvent> {
    val skipped = listOf("Script", "Image", "Stylesheet", "Font", "Document")
    val logs = driver.manage().logs()
    val all = logs.get(LogType.PERFORMANCE).all
    val events = all.asSequence()
      .map { SerialisationUtils.jsonMapper.readTree(it.message).get("message") }
      .filter {
        it.get("method").asText() == "Network.responseReceived" && !skipped.contains(it.get("params").get("type").asText())
      }
      .map { SerialisationUtils.jsonMapper.convertValue(it.get("params"), BrowserResponseEvent::class.java) }
    return events.map { event ->
//      if (this.params.streamingUrlOverride != null) {
//        all.size
//        logs.availableLogTypes
      if (!cachedEventsPrinted.contains(event.requestId)) {
        cachedEventsPrinted.add(event.requestId!!)
//        log.debug("Network response event: ${event.response.mimeType} ${event.type} | ${event.requestId} | ${event.response.url} ")
        
        if (event.response.mimeType.contains("mpegurl")) {
          try {
            val resp = driver.getNetworkResponse(event.requestId)
            val body = if (resp?.base64Encoded == true || (resp?.body?.length
                ?: 0) > 200
            ) " | Size: ${resp?.body?.length ?: 0}" else "\n" + resp?.body.orEmpty()
            
            log.debug("Response body ${event.requestId} ${body}")
          } catch (e: Exception) {
            log.warn("No resource ${event.requestId} | ${e.message}")
          }
        }
      }
//      }
      
      event
    }
  }
  
  fun getNetworkResponseEventsFromJS(): List<JSNetworkEvent> {
    return driver.executeScriptFetchList(
      "return window.performance.getEntries().filter(r => r.entryType == 'resource' && r.initiatorType == 'xmlhttprequest')"
    )?.let {
      SerialisationUtils.jsonMapper.convertValue(it, SerialisationUtils.type<List<JSNetworkEvent>>())
    }!!
  }
  
  
  suspend fun setupNetworkInterceptor(
    cancelRequest: Boolean = false,
    cancelAllRequestAfter: Boolean = false,
    filter: (HttpRequest) -> Boolean
  ): Pair<HttpRequest, HttpResponse?> = suspendCoroutine { continuation ->
    var found = false
    val listener = driver.setNetworkInterceptor { req, handler ->
      if (found && cancelAllRequestAfter) {
        log.info("Canceling request {}", req.uri)
        return@setNetworkInterceptor null
      }
      
      if (filter(req)) {
        found = true
        log.info("Found request {}", req.uri)
        if (cancelRequest) {
          continuation.resume(req to null)
          return@setNetworkInterceptor null
        } else {
          val response = handler.execute(req)
          continuation.resume(req to response)
          return@setNetworkInterceptor response
        }
      } else {
        log.trace("Skipping request {}", req.uri)
        return@setNetworkInterceptor handler.execute(req)
      }
    }
  }
  
  fun clearNetworkInterceptor() {
    driver.clearNetworkInterceptor()
  }
  
  suspend fun setupListenerAndWaitForCorrectEvent4(
    filter: (eventNumber: Long, response: ResponseReceived) -> Boolean
  ): Deferred<ResponseReceived> = inBackgroundAsync {
    suspendCoroutine { continuation ->
      var listener: NetworkEventResponseReceivedListener? = null
      listener = driver.addListener { id, e ->
        if (filter(id, e)) {
          continuation.resume(e)
          driver.removeListener(listener!!)
        }
      }
    }
  }
  
  suspend fun setupListenerAndWaitForCorrectEvent2(
    filter: (eventNumber: Long, response: BrowserResponseEvent) -> Boolean
  ): Deferred<BrowserResponseEvent> = inBackgroundAsync {
    val start = java.time.Instant.now().epochSecond
    val cachedNotMatching = mutableSetOf<String>()
    
    log.debug("Started listening for events")
    
    val check = {
      getNetworkResponseEventsFromBrowserTools()
        .filter { it.response.responseTime >= start }
        .filterNot { cachedNotMatching.contains(it.requestId) }
        .onEach { log.debug("Event processing: ${it.toLine()}") }
        .firstOrNull {
          val result = filter((it.timestamp * 1000).toLong(), it)
          if (!result) {
            cachedNotMatching.add(it.requestId!!)
          }
          result
        }
    }
    
    var result = check()
    while (result == null) {
      delay(100)
      result = check()
    }
    log.debug("Found event: ${result.toLine()}")
    return@inBackgroundAsync result
  }
  
  suspend fun setupListenerAndWaitForCorrectEvent3(
    filter: (response: RequestData) -> Boolean
  ): Deferred<RequestData> = inBackgroundAsync {
    log.debug("Started listening for events")
    
    suspendCoroutine { continuation ->
      var listener: NetworkRequestListener? = null
      listener = driver.addListener { e ->
        if (filter(e)) {
          log.debug("Found event: ${e.requestId}")
          continuation.resume(e)
          driver.removeListener(listener!!)
        }
      }
    }
  }
  
  fun clearListeners() {
    driver.removeListeners()
  }
}

