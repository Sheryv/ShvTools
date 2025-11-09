package com.sheryv.tools.webcrawler

import com.sheryv.tools.webcrawler.trakt.ProcessingResult
import com.sheryv.util.SerialisationUtils
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

class RunFilmwebScraper {
  
  @Test
  fun parse() {
    val s = "Widziałeś 7 z 8 odcinków"
    val s2 = "6/10"
    val y = "2000"
    val y2 = "2001 - 1002"
    val y3 = "2002-1002"
    
    assert(parse(s) == Ratio(7, 8))
    assert(parse(s2) == Ratio(6, 10))
    assert(parseYear(y) == 2000)
    assert(parseYear(y2) == 2001)
    assert(parseYear(y3) == 2002)
  }
  
  
  @Test
  fun fillWatched() {
    val list = SerialisationUtils.fromJson<List<ProcessingResult>>(Files.readString(Path.of("processed.json")))
    val out = mutableListOf<ProcessingResult>()
    val input = SerialisationUtils.fromJson<Data>(Files.readString(Path.of("to_process.json")))
    for (result in list) {
      if (input.watchlist.any { it.url == result.url }){
        out.add(result.copy(watchlist = true))
      }else{
        out.add(result)
      }
    }
    Files.writeString(Path.of("processed.json"), SerialisationUtils.toJson(out))
  }
  
  @Test
  fun run() {
    val url = "https://www.filmweb.pl"
    val input = SerialisationUtils.fromJson<Data>(Files.readString(Path.of("to_process.json")))
    val out = mutableListOf<ProcessingResult>()
    if (Files.exists(Path.of("processed.json"))) {
      out.addAll(SerialisationUtils.fromJson<List<ProcessingResult>>(Files.readString(Path.of("processed.json"))))
    }
    
    try {
      MockCrawler.createMock({
        for (data in input.ratings) {
          if (out.any { it.url == data.url }) {
            println("Skipping ${out.first { it.url == data.url }.title}")
            continue
          }
          
          driver.get(url + data.url)
          val parent = ".page__wrapper .filmCoverSection__filmPreview > div"
          val title = getText("$parent .filmCoverSection__title")
          val year = getText("$parent .filmCoverSection__year")
          val originalTitle = getTextOrNull("$parent .filmCoverSection__originalTitle")?.let {
            if (it.endsWith(year)) {
              it.removeSuffix(year)
            } else {
              it
            }
          }
          
          val res = ProcessingResult(
            data.url,
            false,
            originalTitle ?: title,
            originalTitle?.let { title },
            driver.findElement(By.cssSelector(parent)).getAttribute("data-entity-name")!!,
            parseYear(year),
            getText(".FilmRatingSection a").takeIf { it.isNotBlank() }?.let { parse(it) },
            LocalDate.parse(getAttr(".FilmRatingSection button", "title")),
            parse(getText("$parent .entityInUserTaste__title"))
          )
          
          out.add(res)
          println(res)
        }
        
        for ((i, data) in input.watchlist.withIndex()) {
          if (out.any { it.url == data.url }) {
            val current = out.first { it.url == data.url }
            println("Skipping ${current.title}")
            out[i] = current.copy(watchlist = true)
            continue
          }
          
          driver.get(url + data.url)
          val parent = ".page__wrapper .filmCoverSection__filmPreview > div"
          val title = getText("$parent .filmCoverSection__title")
          val year = getText("$parent .filmCoverSection__year")
          val originalTitle = getTextOrNull("$parent .filmCoverSection__originalTitle")?.let {
            if (it.endsWith(year)) {
              it.removeSuffix(year)
            } else {
              it
            }
          }
          
          val res = ProcessingResult(
            data.url,
            true,
            originalTitle ?: title,
            originalTitle?.let { title },
            driver.findElement(By.cssSelector(parent)).getAttribute("data-entity-name")!!,
            parseYear(year),
            getTextOrNull(".FilmRatingSection a")?.takeIf { it.isNotBlank() }?.let { parse(it) },
            null,
            getTextOrNull("$parent .entityInUserTaste__title")?.let { parse(it) }
          )
          
          out.add(res)
          println(res)
        }
        
        
      }, url)
    } finally {
      Files.writeString(Path.of("processed.json"), SerialisationUtils.toJson(out))
    }
  }
  
  
  suspend fun MockCrawler.getAttr(
    cssSelector: String,
    attribute: String,
    condition: ((WebElement) -> Boolean) = { !it.getAttribute(attribute).isNullOrEmpty() }
  ): String {
    val attr = waitForFirstAttributeIf(By.cssSelector(cssSelector), attribute, condition, 20)
    return attr!!.getAttribute(attribute)!!
  }
  
  fun MockCrawler.getText(cssSelector: String): String {
    val e = driver.waitForVisibility(By.cssSelector(cssSelector), 20)
    return e.text
  }
  
  fun MockCrawler.getTextOrNull(cssSelector: String): String? {
    try {
      val element = driver.wait(5).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(cssSelector)))
      return element.text
    } catch (e: TimeoutException) {
      return null
    }
  }
  
  fun parse(s: String): Ratio? {
    if (s.contains(" z ")) {
      val parts = s.split(" ")
      return Ratio(parts[1].toInt(), parts[3].toInt())
    }
    if (s.contains("/")) {
      val parts = s.split("/")
      return Ratio(parts[0].toInt(), parts[1].toInt())
    }
    return null
  }
  
  fun parseYear(text: String): Int {
    if (text.contains("-") || text.contains(" ")) {
      val match = numberPattern.find(text.trim())
      val number = match!!.groups.get(0)!!.value
      return number.toInt()
    }
    return text.toInt()
  }
  
  private val numberPattern = Regex("""\d+""")
  
  data class Data(val ratings: List<Link>, val watchlist: List<Link>)
  data class Link(val url: String)
  
  data class Ratio(val current: Int, val total: Int)
}
