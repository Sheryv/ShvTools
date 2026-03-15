package com.sheryv.tools.webcrawler.process.base.model

import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.Crawler
import com.sheryv.tools.webcrawler.service.BrowserSupport
import com.sheryv.tools.webcrawler.utils.ViewUtils
import com.sheryv.util.logging.log
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.openqa.selenium.*
import org.openqa.selenium.bidi.network.AddInterceptParameters
import org.openqa.selenium.bidi.network.ContinueRequestParameters
import org.openqa.selenium.bidi.network.InterceptPhase
import org.openqa.selenium.bidi.network.RequestData
import org.openqa.selenium.chromium.ChromiumDriver
import org.openqa.selenium.devtools.v142.network.Network
import org.openqa.selenium.devtools.v142.network.model.RequestId
import org.openqa.selenium.devtools.v142.network.model.ResponseReceived
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.remote.http.HttpHandler
import org.openqa.selenium.remote.http.HttpRequest
import org.openqa.selenium.remote.http.HttpResponse
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.*
import java.util.logging.Level

typealias NetworkInterceptor = (request: HttpRequest, handler: HttpHandler) -> HttpResponse?
typealias NetworkRequestListener = (request: RequestData) -> Unit
typealias NetworkEventResponseReceivedListener = (eventNumber: Long, response: ResponseReceived) -> Unit

open class SeleniumDriver(
  protected val wrappedDriver: WebDriver,
  protected val implicitWait: Long = 3,
  protected val executor: JavascriptExecutor = wrappedDriver as JavascriptExecutor
) : SDriver, WebDriver {
  protected lateinit var crawler: Crawler<SeleniumDriver, SettingsBase>
  private val cachedScript: String by lazy {
    val script = BrowserSupport.get.loadScriptFromClassPath(crawler.def.id())
    script
  }
  
  var networkBidi: org.openqa.selenium.bidi.module.Network? = null
  
  private val initScript: String by lazy {
    BrowserSupport.get.loadScriptFromClassPath("_init")
  }
  
  //  private val interceptors: MutableList<NetworkInterceptor> = Collections.synchronizedList(mutableListOf<NetworkInterceptor>())
  private val networkResponseListeners: MutableList<NetworkEventResponseReceivedListener> =
    Collections.synchronizedList(mutableListOf<NetworkEventResponseReceivedListener>())
  
  private var interceptor: NetworkInterceptor? = null
  
  private val requestStartListeners: MutableList<NetworkRequestListener> =
    Collections.synchronizedList(mutableListOf<NetworkRequestListener>())
  
  init {
    wrappedDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait))
    
    val cmd = mapOf(
      "source" to initScript + "\n\nconsole.log('${ViewUtils.TITLE} initialised');"
    )
    when (wrappedDriver) {
      is ChromiumDriver -> {
        wrappedDriver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", cmd)
      }
    }
  }
  
  override fun initialize(crawler: Crawler<SDriver, SettingsBase>) {
    this.crawler = crawler as Crawler<SeleniumDriver, SettingsBase>
  }
  
  protected val cachedDocument: Document by lazy { Jsoup.parse(wrappedDriver.pageSource) }
  
  override fun findElementsByTag(tag: String): Elements {
    return cachedDocument.getElementsByTag(tag)
  }
  
  override fun findElementsBySelector(selector: String): Elements {
    return cachedDocument.select(selector)
  }
  
  override fun findElementById(id: String): Element? {
    return cachedDocument.getElementById(id)
  }
  
  override fun getPageDocument(): Document {
    return cachedDocument
  }
  
  override fun findElements(by: By): MutableList<WebElement> {
    return wrappedDriver.findElements(by)
  }
  
  override fun findElement(by: By): WebElement {
    return wrappedDriver.findElement(by)
  }
  
  override fun getCurrentUrl(): String? {
    return wrappedDriver.currentUrl
  }
  
  override fun getTitle(): String? {
    return wrappedDriver.title
  }
  
  override fun getPageSource(): String? {
    return wrappedDriver.pageSource
  }
  
  override fun close() {
    when (wrappedDriver) {
      is ChromiumDriver -> {
        wrappedDriver.devTools.clearListeners()
      }
    }
    removeListeners()
    networkBidi?.close()
    return wrappedDriver.close()
  }
  
  override fun quit() {
    return wrappedDriver.quit()
  }
  
  override fun getWindowHandles(): MutableSet<String> {
    return wrappedDriver.windowHandles
  }
  
  override fun getWindowHandle(): String {
    return wrappedDriver.windowHandle
  }
  
  override fun switchTo(): WebDriver.TargetLocator {
    return wrappedDriver.switchTo()
  }
  
  override fun navigate(): WebDriver.Navigation {
    return wrappedDriver.navigate()
  }
  
  override fun manage(): WebDriver.Options {
    return wrappedDriver.manage()
  }
  
  override fun get(url: String) {
    if (url.startsWith("http:") || url.startsWith("https:")) {
      wrappedDriver.get(url)
    } else {
      var merged = crawler.def.attributes.websiteUrl.trimEnd('/') + "/" + url.trimStart('/')
      wrappedDriver.get(merged)
    }
  }
  
  fun waitFor(by: By, seconds: Long = 10): WebElement {
    val wait = WebDriverWait(wrappedDriver, Duration.ofSeconds(seconds))
    return wait.until(ExpectedConditions.presenceOfElementLocated(by))!!
  }
  
  fun waitForVisibility(by: By, seconds: Long = 10): WebElement {
    val wait = WebDriverWait(wrappedDriver, Duration.ofSeconds(seconds))
    return wait.until(ExpectedConditions.visibilityOfElementLocated(by))
  }
  
  fun wait(seconds: Long = 10): WebDriverWait {
    return WebDriverWait(wrappedDriver, Duration.ofSeconds(seconds))
  }
  
  fun executeScript(script: String): Any? {
    val res = executor.executeScript(script)
//    log.debug("Executed JS: $script")
    return res
  }
  
  fun executeScriptFunction(function: String, vararg params: String): Any? {
    val exp = getFunctionExpressionWithCheck(function, params)
    val r = executor.executeScript(exp)
    log.debug("Executed function: $function(${params.joinToString(", ")}) |\nresult: $r")
    return r
  }
  
  fun executeScriptFunctionToList(function: String, vararg params: String): List<Map<String, *>>? {
    val exp = getFunctionExpressionWithCheck(function, params)
    val r = executeScriptFetchList(exp)
    log.debug("Executed function: $function(${params.joinToString(", ")}) |\nresult: $r")
    return r
  }
  
  fun executeScriptFetchList(script: String): List<Map<String, *>>? {
    val o = executeScript(script)
    return if (o is List<*>) {
      return o as List<Map<String, *>>
    } else null
  }
  
  fun saveScreenshot(targetPath: Path): Path {
    Files.createDirectories(targetPath.parent)
    val shot = (wrappedDriver as TakesScreenshot).getScreenshotAs<File>(OutputType.FILE).toPath()
    Files.move(targetPath, shot, StandardCopyOption.REPLACE_EXISTING)
    return targetPath
  }
  
  fun enableDevToolsWithNetworkModule() {
    when (wrappedDriver) {
      is FirefoxDriver -> {
        wrappedDriver.setLogLevel(Level.OFF)
        
        networkBidi = org.openqa.selenium.bidi.module.Network(wrappedDriver)
        networkBidi!!.addIntercept(AddInterceptParameters(InterceptPhase.BEFORE_REQUEST_SENT))
        networkBidi!!.onBeforeRequestSent { req ->
          val requestId = req.request.requestId
          log.debug("INTERCEPT, RequestId: {}, {}", requestId, req.request.url)
          networkBidi!!.continueRequest(ContinueRequestParameters(requestId))
          log.debug("INTERCEPT done, RequestId: {}", requestId)
        }
        
      }
      
      is ChromiumDriver -> {
        
        networkBidi = org.openqa.selenium.bidi.module.Network(wrappedDriver)
//        networkBidi!!.addIntercept(AddInterceptParameters(InterceptPhase.RESPONSE_STARTED))
//        networkBidi!!.onResponseStarted { resp ->
//          val requestId = resp.request.requestId
//          log.debug("INTERCEPT 2, RequestId: {}, {}", requestId, resp.request.url)
//          inBackground {
//            networkBidi!!.continueResponse(ContinueResponseParameters(requestId))
////            networkBidi!!.provideResponse(ProvideResponseParameters(requestId))
//            log.debug("INTERCEPT 2 done async, RequestId: {}", requestId)
//          }
//          log.debug("INTERCEPT 2 done, RequestId: {}", requestId)
//        }
        
//        networkBidi!!.onBeforeRequestSent { req ->
//          val requestId = req.request.requestId
//          log.debug(
//            "INTERCEPT: {} [{}] {}",
//            requestId,
//            req.request.headers.firstOrNull { "Accept".equals(it.name, true)  }?.value?.value.orEmpty(),
//            req.request.url
//          )
//          val listeners = requestStartListeners
//          for (listener in listeners) {
//            listener.invoke(req.request)
//          }
////          inBackground {
////            networkBidi!!.continueRequest(ContinueRequestParameters(requestId))
////            delay(1000)
////          }
////          networkBidi!!.continueRequest(ContinueRequestParameters(requestId))
//        }
//
        wrappedDriver.setLogLevel(Level.FINE)
        wrappedDriver.devTools.createSessionIfThereIsNotOne()
        wrappedDriver.devTools.send(
          Network.enable(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
          )
        )


//        wrappedDriver.devTools.domains.network().interceptTrafficWith { next ->
//          HttpHandler { req: HttpRequest ->
//            val interceptor = this.interceptor
//            val response = if (interceptor == null) {
//              next.execute(req)
//            } else {
//              interceptor.invoke(req, next)
//            }
//            var content = ""
//            if (response?.contentType?.contains("mpegurl") == true) {
//              content = "\n" + response.contentAsString()
//            }
//
//            log.debug("Intercept: ${req.method} ${response?.status ?: "0"} ${response?.contentType.orEmpty()} | size: ${response?.content?.length() ?: 0} B | ${req.uri}${content}")
//            response
//          }
//        }
//        val usefulTypes = listOf(ResourceType.OTHER, ResourceType.FETCH, ResourceType.XHR, ResourceType.MEDIA)
////        Network.requestWillBeSent()
//        wrappedDriver.devTools.addListener(Network.responseReceived()) { id, e ->
////          if (usefulTypes.contains(e.type))
//          log.debug(
//            "EVENT responseReceived {} | {} [{}] | size: {} B | {}",
//            e.type,
//            e.requestId,
//            e.frameId,
//            e.response.encodedDataLength,
//            e.response.url
//          )
//
//          val listeners = networkResponseListeners
//          for (listener in listeners) {
//            listener.invoke(id, e)
//          }
//        }
      }
    }
  }
  
  fun getNetworkResponse(requestId: String): Network.GetResponseBodyResponse? {
    return when (wrappedDriver) {
      is ChromiumDriver ->
//        try {
//        wrappedDriver.executeCdpCommand("Network.getResponseBody", mapOf("requestId" to requestId))
//      } catch (e: Exception) {
        wrappedDriver.devTools.send(Network.getResponseBody(RequestId(requestId)))
//      }
      
      else -> null
    }
  }
  
  fun setNetworkInterceptor(interceptor: NetworkInterceptor): NetworkInterceptor {
//    interceptors.add(interceptor)
    this.interceptor = interceptor
    return interceptor
  }
  
  fun clearNetworkInterceptor() {
//    interceptors.remove(interceptor)
    this.interceptor = null
  }
  
  private fun getFunctionExpressionWithCheck(function: String, params: Array<out String>): String {
    if (cachedScript.contains(Regex("function\\s+$function\\s*\\("))) {
      return cachedScript + ";\nreturn " + function + "(" + params.joinToString(", ") + ");"
    }
    throw IllegalArgumentException("Function '$function' was not found in script files")
  }
  
  
  fun addListener(listener: NetworkEventResponseReceivedListener): NetworkEventResponseReceivedListener {
    networkResponseListeners.add(listener)
    return listener
  }
  
  fun removeListener(listener: NetworkEventResponseReceivedListener): NetworkEventResponseReceivedListener {
    networkResponseListeners.remove(listener)
    return listener
  }
  
  
  fun addListener(listener: NetworkRequestListener): NetworkRequestListener {
    this.requestStartListeners.add(listener)
    return listener
  }
  
  fun removeListener(listener: NetworkRequestListener): NetworkRequestListener {
    this.requestStartListeners.remove(listener)
    return listener
  }
  
  fun removeListeners() {
    networkResponseListeners.clear()
    this.requestStartListeners.clear()
  }
}
