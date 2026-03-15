package com.sheryv.tools.webcrawler.service.subsdownload

import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.util.io.HttpSupport
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.io.IOException
import java.lang.String
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.Duration
import kotlin.Exception
import kotlin.plus
import kotlin.text.contains

class SubsDownloadRunner {
  private var executor: JavascriptExecutor? = null
  
  var webWait: WebDriverWait? = null
    private set
  
  var driver: WebDriver? = null
    private set
  
  fun start(options: SubsDownloadOptions) {
    try {
      driver = ChromeDriver(this.chromeOptions)
      webWait = WebDriverWait(driver, Duration.ofSeconds(15))
      driver!!.manage().timeouts().scriptTimeout(Duration.ofSeconds(3))
      driver!!.manage().timeouts().implicitlyWait(Duration.ofSeconds(1))
      executor = driver as JavascriptExecutor
      run(options)
      println("Runner execution finished successfully")
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      if (driver != null) {
        driver!!.close()
        driver!!.quit()
      }
    }
  }
  
  @Throws(IOException::class)
  private fun run(options: SubsDownloadOptions) {
    val url = options.formatFullUrl()
    driver!!.navigate().to(url)
    val searchResults = By.id("search_results")
    webWait!!.until<WebElement?>(ExpectedConditions.presenceOfElementLocated(searchResults))
    val js = "return $('#search_results td > a[itemprop=url]').parent().next().next().find('a').attr('href');"
    val link = executor!!.executeScript(js) as String
    val zip = download(link, options)
    val destinationDir: File =
      Paths.get(options.temporaryDirectory, options.series + "_" + options.season + "_" + options.episode).toFile()
    ZipUtils.unzip(zip, destinationDir)
    zip.delete()
    var hasPolish = false
    val dir: File = File(
      options.downloadDirectory,
      String.format("%s_s%02de%02d", options.series, options.season, options.episode)
    )
    for (file in destinationDir.listFiles()) {
      var prefix = "_"
      if ("polish".contains(file.getName())) {
        prefix = "pl" + prefix
        hasPolish = true
      } else if ("english".contains(file.getName())) {
        prefix = "en" + prefix
      }
      for (sub in file.listFiles()) {
        dir.mkdirs()
        Files.copy(
          sub.toPath(), Paths.get(dir.getAbsolutePath(), prefix + sub.getName()),
          StandardCopyOption.REPLACE_EXISTING
        )
      }
    }
    Files.walk(destinationDir.toPath())
      .map<File?> { obj: Path? -> obj!!.toFile() }
      .sorted { o1: File?, o2: File? -> -o1!!.compareTo(o2) }
      .forEach { obj: File? -> obj!!.delete() }
    println("Files saved to " + dir.getAbsolutePath())
    if (hasPolish) {
      println("Detected Polish subtitles - finishing")
    } else {
      translateSubs()
    }
  }
  
  private fun translateSubs() {
  }
  
  @Throws(IOException::class)
  private fun download(link: String, options: SubsDownloadOptions): File {
    val http: HttpSupport = HttpSupport()
    val path: Path =
      Paths.get(options.temporaryDirectory, options.series + "_" + options.season + "_" + options.episode + ".zip")
    http.stream(HttpSupport.getRequest(options.baseUrl + link)).body().use { stream ->
      Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING)
    }
    return path.toFile()
  }
  
  private val chromeOptions: ChromeOptions
    get() {
      val c: Configuration = Configuration.get()
      val dc = DesiredCapabilities()
//      dc.setJavascriptEnabled(false)
      val options = ChromeOptions()
      options.setBinary(c.browserSettings.currentBrowser().binaryPath!!.toFile())
      options.setCapability("applicationCacheEnabled", true)
      options.merge(dc)
      return options
    }
}
