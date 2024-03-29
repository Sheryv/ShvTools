package com.sheryv.tools.webcrawler.process.impl.filmweb

import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.FilmwebSettings
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.SeleniumCrawler
import com.sheryv.tools.webcrawler.process.base.model.ProcessParams
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.process.base.model.Step
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.logging.log
import org.openqa.selenium.By

class FilmwebCrawler(
  configuration: Configuration,
  browser: BrowserConfig,
  def: CrawlerDefinition<SeleniumDriver, FilmwebSettings>,
  driver: SeleniumDriver,
  params: ProcessParams
) : SeleniumCrawler<FilmwebSettings>(configuration, browser, def, driver, params) {
  
  private val result = FilmwebResult()
  private lateinit var user: String
  
  override fun getSteps(): List<Step<out Any, out Any>> {
    return listOf(
      Step("init", { loadInitPage() }),
      Step("login", ::login),
      Step("movies", ::movies),
      Step("tvshows", ::tvshows),
      Step("wannaSee", ::wannaSee),
      Step("save", ::save),
    )
  }
  
  private fun login(a: Any?): Any {
    logText("Before login")
    driver.get("/login")
//    driver.findElements(By.tagName("didomi-notice-agree-button")).firstOrNull()?.click()
    logText("Wait for logging")
    val link = driver.waitFor(By.cssSelector("header .userAvatar > a"), 60 * 5L)
    val linkHref = link.getAttribute("href")
    user = linkHref.substring(linkHref.lastIndexOf("/") + 1)
    logText("User {}", user)
    
    driver.get("/user/$user")
    
    logText("Wait for profile")
    driver.waitFor(By.className("UserProfilePageAbout"))
    return user
//    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10))
//    driver.manage().window().maximize()
//
//    driver.get("https://www.google.com")
//    driver.close()
  
  }
  
  private fun movies(a: Any?): Any {
    val url = "/user/$user/films"
    driver.get(url)
    
    val res = loadItemsPaged(url)
    if (res != null) {
      result.movies = res
    } else {
      log.warn("No rated movie was found")
    }
    return ""
  }
  
  private fun tvshows(a: Any?): Any {
    val url = "/user/$user/serials"
    driver.get(url)
    
    val res = loadItemsPaged(url)
    if (res != null) {
      result.tvShows = res
    } else {
      log.warn("No rated tv show was found")
    }
    
    return ""
  }
  
  private fun wannaSee(a: Any?): Any {
    val url = "/user/$user/wantToSee"
    driver.get(url)
    
    val res = loadItemsPaged(url)
    if (res != null) {
      result.wantToSee = res
    } else {
      log.warn("No rated want to see entry was found")
    }
    
    return ""
  }
  
  private fun save(a: Any?): Any {
    SerialisationUtils.jsonMapper.writeValue(settings.outputPath.toFile(), result)
    log.info("File saved " + settings.outputPath)
    return ""
  }
  
  private fun loadItemsPaged(currentUrl: String): List<FilmwebSearch>? {
    driver.waitFor(By.cssSelector(".userVotesPage .userVotesPage__results"))
    val pagination =
      driver.findElements(By.cssSelector(".pagination .pagination__item:not(.pagination__item--next):not(.pagination__item--prev)"))
    val result = mutableListOf<FilmwebSearch>()
    
    if (pagination.isNotEmpty()) {
      val pages = pagination.size
      if (pages == 1) {
        return loadItems()
      } else {
        
        for (page in 1..pages) {
          if (page != 1) {
            driver.get("$currentUrl?page=$page")
          }
          
          loadItems()?.let { result.addAll(it) }
        }
      }
    } else {
      return loadItems()
    }
    return result
  }
  
  private fun loadItems(): List<FilmwebSearch>? {
    val listWrapper = driver.waitFor(By.cssSelector(".userVotesPage .userVotesPage__results"))
    val list = listWrapper.findElements(By.className("userVotesPage__result"))
    
    val res: List<FilmwebSearch>? = SerialisationUtils.deserializeList(driver.executeScriptFunctionToList("loadItemsFromPage"))
    
    return res
  }
}
