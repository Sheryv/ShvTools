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
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chromium.ChromiumDriver
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration


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
  
  init {
    wrappedDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait))
    
    val searchPrefix = """^\\""" + "\$?cdc_a.*"
    val cmd = mapOf(
      "source" to """
      Object.defineProperty(Navigator.prototype, 'webdriver', {
          set: undefined,
          enumerable: true,
          configurable: true,
          get: new Proxy(
              Object.getOwnPropertyDescriptor(Navigator.prototype, 'webdriver').get,
              { apply: (target, thisArg, args) => {
                  // emulate getter call validation
                  Reflect.apply(target, thisArg, args);
                  return undefined;
              }}
          )
      });
      Object.defineProperty(navigator, 'webdriver', {
        configurable: true,
        get: () => undefined
      });
      Object.keys(window).filter(k=>k.match('$searchPrefix')).forEach(k=>{ delete window[k]})
      Object.keys(document).filter(k=>k.match('$searchPrefix')).forEach(k=>{ delete document[k]})
      delete Navigator.prototype.webdriver;
      delete navigator.webdriver;
      delete navigator.webdriver;
      console.log('${ViewUtils.TITLE} initialised');
    """
    )
    when (wrappedDriver) {
      is ChromiumDriver -> wrappedDriver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", cmd)
      is EdgeDriver -> wrappedDriver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", cmd)
    }
  }
  
  override fun initialize(crawler: Crawler<out SDriver, SettingsBase>) {
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
  
  override fun findElements(by: By?): MutableList<WebElement> {
    return wrappedDriver.findElements(by)
  }
  
  override fun findElement(by: By?): WebElement {
    return wrappedDriver.findElement(by)
  }
  
  override fun getCurrentUrl(): String {
    return wrappedDriver.currentUrl
  }
  
  override fun getTitle(): String {
    return wrappedDriver.title
  }
  
  override fun getPageSource(): String {
    return wrappedDriver.pageSource
  }
  
  override fun close() {
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
    return wait.until(ExpectedConditions.presenceOfElementLocated(by))
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
  
  private fun getFunctionExpressionWithCheck(function: String, params: Array<out String>): String {
    if (cachedScript.contains(Regex("function\\s+$function\\s*\\("))) {
      return cachedScript + ";\nreturn " + function + "(" + params.joinToString(", ") + ");"
    }
    throw IllegalArgumentException("Function '$function' was not found in script files")
  }
}
